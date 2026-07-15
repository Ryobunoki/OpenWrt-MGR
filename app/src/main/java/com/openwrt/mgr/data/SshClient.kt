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

            readerThread = Thread({
                val buf = ByteArray(16384)
                try {
                    while (!Thread.currentThread().isInterrupted && ch.isConnected) {
                        val n = input.read(buf)
                        if (n < 0) break
                        if (n > 0) {
                            val text = String(buf, 0, n, Charsets.UTF_8)
                            outputListener?.invoke(text)
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

    private fun disconnectQuiet() {
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
}

