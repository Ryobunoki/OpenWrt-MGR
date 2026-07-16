package com.openwrt.mgr.data

import com.jcraft.jsch.ChannelShell
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import java.io.InputStream
import java.io.OutputStream
import java.util.Properties
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Lightweight interactive SSH shell for OpenWrt dropbear/openssh.
 * Runs IO on caller-provided threads; callbacks may arrive off-main-thread.
 */
class SshClient {
    // Lazy: avoids ClassInit/NoSuchMethod on app startup if stubs lag behind JSch
    private val jsch by lazy { JSch() }
    private var session: Session? = null
    private var channel: ChannelShell? = null
    private var stdin: OutputStream? = null
    private var readerThread: Thread? = null
    private val connected = AtomicBoolean(false)
    /** Incomplete UTF-8 sequence held across socket reads (prevents intermittent mojibake). */
    private var utf8Carry: ByteArray = ByteArray(0)

    @Volatile
    private var outputListener: ((String) -> Unit)? = null

    @Volatile
    private var statusListener: ((Boolean, String?) -> Unit)? = null

    fun isConnected(): Boolean = connected.get() && session?.isConnected == true

    fun setListeners(
        onOutput: (String) -> Unit,
        onStatus: (Boolean, String?) -> Unit
    ) {
        outputListener = onOutput
        statusListener = onStatus
    }

    fun connect(host: String, port: Int, username: String, password: String) {
        disconnect()
        try {
            val pureHost = host.trim()
                .removePrefix("https://")
                .removePrefix("http://")
                .substringBefore('/')
                .substringBefore(':')
                .ifBlank { "192.168.10.1" }
            val user = username.trim().ifBlank { "root" }
            val sess = jsch.getSession(user, pureHost, port.coerceIn(1, 65535))
            sess.setPassword(password)
            val config = Properties()
            config["StrictHostKeyChecking"] = "no"
            config["PreferredAuthentications"] = "password,keyboard-interactive"
            // Prefer modern OpenWrt/dropbear suites; keeps R8 able to drop unused algs
            config["kex"] = "curve25519-sha256,curve25519-sha256@libssh.org,ecdh-sha2-nistp256,diffie-hellman-group14-sha256,diffie-hellman-group14-sha1,diffie-hellman-group1-sha1"
            config["server_host_key"] = "ssh-ed25519,ecdsa-sha2-nistp256,ecdsa-sha2-nistp384,ecdsa-sha2-nistp521,rsa-sha2-512,rsa-sha2-256,ssh-rsa"
            config["cipher.s2c"] = "aes128-ctr,aes256-ctr,aes192-ctr,aes128-cbc,aes256-cbc,3des-cbc"
            config["cipher.c2s"] = "aes128-ctr,aes256-ctr,aes192-ctr,aes128-cbc,aes256-cbc,3des-cbc"
            config["mac.s2c"] = "hmac-sha2-256,hmac-sha2-512,hmac-sha1"
            config["mac.c2s"] = "hmac-sha2-256,hmac-sha2-512,hmac-sha1"
            config["compression.s2c"] = "none"
            config["compression.c2s"] = "none"
            sess.setConfig(config)
            sess.timeout = 15_000
            sess.serverAliveInterval = 20_000
            sess.serverAliveCountMax = 3
            sess.connect(15_000)

            val ch = sess.openChannel("shell") as ChannelShell
            ch.setPtyType("xterm-256color")
            ch.setPtySize(80, 24, 640, 480)
            runCatching { ch.setEnv("LANG", "C.UTF-8") }
            runCatching { ch.setEnv("LC_ALL", "C.UTF-8") }
            runCatching { ch.setEnv("TERM", "xterm-256color") }
            runCatching { ch.setEnv("COLORTERM", "truecolor") }
            val out = ch.outputStream
            val input: InputStream = ch.inputStream
            ch.connect(10_000)

            session = sess
            channel = ch
            stdin = out
            connected.set(true)
            statusListener?.invoke(true, null)

            utf8Carry = ByteArray(0)
            readerThread = Thread({
                val buf = ByteArray(16384)
                try {
                    while (!Thread.currentThread().isInterrupted && ch.isConnected) {
                        val n = input.read(buf)
                        if (n < 0) break
                        if (n > 0) {
                            val text = takeUtf8(buf, n)
                            if (text.isNotEmpty()) outputListener?.invoke(text)
                        }
                    }
                } catch (_: Exception) {
                    // closed or IO error
                } finally {
                    if (connected.getAndSet(false)) {
                        statusListener?.invoke(false, "SSH 已断开")
                    }
                    runCatching { ch.disconnect() }
                    runCatching { sess.disconnect() }
                }
            }, "ssh-reader").also {
                it.isDaemon = true
                it.start()
            }
        } catch (e: Exception) {
            connected.set(false)
            disconnectQuiet()
            statusListener?.invoke(false, e.message ?: "SSH 连接失败")
            throw e
        }
    }

    fun send(text: String) {
        val stream = stdin ?: throw IllegalStateException("SSH 未连接")
        synchronized(stream) {
            stream.write(text.toByteArray(Charsets.UTF_8))
            stream.flush()
        }
    }

    fun sendCommand(command: String) {
        val line = if (command.endsWith("\n")) command else "$command\n"
        send(line)
    }

    fun resize(cols: Int, rows: Int) {
        runCatching {
            channel?.setPtySize(cols.coerceAtLeast(20), rows.coerceAtLeast(8), cols * 8, rows * 16)
        }
    }

    fun disconnect() {
        connected.set(false)
        disconnectQuiet()
        statusListener?.invoke(false, null)
    }


    /**
     * Decode as much complete UTF-8 as possible; stash trailing partial sequence.
     * Avoids mojibake when multi-byte glyphs (nerd/powerline) split across TCP reads.
     */
    @Synchronized
    private fun takeUtf8(buf: ByteArray, n: Int): String {
        val data = if (utf8Carry.isEmpty()) {
            buf.copyOf(n)
        } else {
            ByteArray(utf8Carry.size + n).also {
                System.arraycopy(utf8Carry, 0, it, 0, utf8Carry.size)
                System.arraycopy(buf, 0, it, utf8Carry.size, n)
            }
        }
        if (data.isEmpty()) return ""
        val cut = completeUtf8Length(data)
        if (cut <= 0) {
            utf8Carry = data
            return ""
        }
        utf8Carry = if (cut < data.size) data.copyOfRange(cut, data.size) else ByteArray(0)
        return String(data, 0, cut, Charsets.UTF_8)
    }

    private fun completeUtf8Length(data: ByteArray): Int {
        var i = data.size
        // Walk back over continuation bytes (10xxxxxx)
        var cont = 0
        while (i > 0 && cont < 3 && (data[i - 1].toInt() and 0xC0) == 0x80) {
            i--
            cont++
        }
        if (i == 0) return 0
        val lead = data[i - 1].toInt() and 0xFF
        val need = when {
            lead and 0x80 == 0 -> 1
            lead and 0xE0 == 0xC0 -> 2
            lead and 0xF0 == 0xE0 -> 3
            lead and 0xF8 == 0xF0 -> 4
            else -> 1
        }
        val have = data.size - (i - 1)
        return if (have < need) i - 1 else data.size
    }
    private fun disconnectQuiet() {
        utf8Carry = ByteArray(0)
        // Snapshot then null fields first so the JSch session thread / reader
        // cannot race a second disconnect path with half-torn state.
        val ch = channel
        val sess = session
        val stream = stdin
        val reader = readerThread
        channel = null
        session = null
        stdin = null
        readerThread = null
        reader?.interrupt()
        runCatching { stream?.close() }
        runCatching { ch?.disconnect() }
        // Session.disconnect may run on the Connect thread after background;
        // stubs must provide PortWatcher.delPort etc. Still guard Throwable.
        runCatching { sess?.disconnect() }.exceptionOrNull()?.let { err ->
            // Never crash UI / reader threads on teardown of a half-dead session.
            if (err !is Exception && err !is Error) return@let
        }
    }
companion object {
        /**
         * One-shot non-interactive SSH command (ChannelExec).
         * Used when ubus file.exec is ACL-denied (code=6).
         * @return exitStatus to stdout text (UTF-8, lossy for binary)
         */
        fun execOnce(
            host: String,
            port: Int,
            username: String,
            password: String,
            command: String,
            timeoutMs: Int = 180_000
        ): Pair<Int, String> {
            val pureHost = host.trim()
                .removePrefix("https://")
                .removePrefix("http://")
                .substringBefore('/')
                .substringBefore(':')
                .ifBlank { "192.168.10.1" }
            val jsch = JSch()
            val sess = jsch.getSession(username.ifBlank { "root" }, pureHost, port.coerceIn(1, 65535))
            sess.setPassword(password)
            val config = Properties()
            config["StrictHostKeyChecking"] = "no"
            config["PreferredAuthentications"] = "password,keyboard-interactive"
            config["kex"] = "curve25519-sha256,curve25519-sha256@libssh.org,ecdh-sha2-nistp256,diffie-hellman-group14-sha256,diffie-hellman-group14-sha1,diffie-hellman-group1-sha1"
            config["server_host_key"] = "ssh-ed25519,ecdsa-sha2-nistp256,ecdsa-sha2-nistp384,ecdsa-sha2-nistp521,rsa-sha2-512,rsa-sha2-256,ssh-rsa"
            config["cipher.s2c"] = "aes128-ctr,aes256-ctr,aes192-ctr,aes128-cbc,aes256-cbc,3des-cbc"
            config["cipher.c2s"] = "aes128-ctr,aes256-ctr,aes192-ctr,aes128-cbc,aes256-cbc,3des-cbc"
            config["mac.s2c"] = "hmac-sha2-256,hmac-sha2-512,hmac-sha1"
            config["mac.c2s"] = "hmac-sha2-256,hmac-sha2-512,hmac-sha1"
            sess.setConfig(config)
            sess.timeout = timeoutMs.coerceIn(5_000, 600_000)
            try {
                sess.connect(15_000)
                val ch = sess.openChannel("exec") as com.jcraft.jsch.ChannelExec
                ch.setCommand(command)
                ch.setInputStream(null)
                val stdout = ch.inputStream
                val stderr = ch.errStream
                ch.connect(10_000)
                val out = java.io.ByteArrayOutputStream()
                val err = java.io.ByteArrayOutputStream()
                val buf = ByteArray(8192)
                val deadline = System.currentTimeMillis() + timeoutMs
                while (true) {
                    while (stdout.available() > 0) {
                        val n = stdout.read(buf)
                        if (n <= 0) break
                        out.write(buf, 0, n)
                    }
                    while (stderr.available() > 0) {
                        val n = stderr.read(buf)
                        if (n <= 0) break
                        err.write(buf, 0, n)
                    }
                    if (ch.isClosed) {
                        while (stdout.available() > 0) {
                            val n = stdout.read(buf)
                            if (n <= 0) break
                            out.write(buf, 0, n)
                        }
                        while (stderr.available() > 0) {
                            val n = stderr.read(buf)
                            if (n <= 0) break
                            err.write(buf, 0, n)
                        }
                        break
                    }
                    if (System.currentTimeMillis() > deadline) {
                        ch.disconnect()
                        throw RuntimeException("SSH 命令超时: $command")
                    }
                    Thread.sleep(40)
                }
                val code = ch.exitStatus
                ch.disconnect()
                val text = buildString {
                    append(out.toString(Charsets.UTF_8.name()))
                    val e = err.toString(Charsets.UTF_8.name())
                    if (e.isNotBlank()) {
                        if (isNotEmpty()) append('\n')
                        append(e)
                    }
                }
                return code to text
            } finally {
                runCatching { sess.disconnect() }
            }
        }
    }
}
