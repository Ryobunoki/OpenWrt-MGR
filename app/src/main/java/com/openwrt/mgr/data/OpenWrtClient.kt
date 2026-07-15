package com.openwrt.mgr.data

import org.json.JSONArray
import org.json.JSONObject
import java.security.cert.CertificateException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException

data class RouterSession(
    val baseUrl: String,
    val username: String,
    /** Either "ubus:<session>" or a raw LuCI sysauth cookie value. */
    val authToken: String
) {
    val isUbus: Boolean get() = authToken.startsWith("ubus:")
    val ubusToken: String? get() = if (isUbus) authToken.removePrefix("ubus:") else null
    val luciCookie: String? get() = if (!isUbus && authToken.isNotBlank()) authToken else null
}

data class SystemInfo(
    val hostname: String,
    val model: String,
    val boardName: String,
    val releaseDescription: String,
    val kernelVersion: String,
    val localTime: String,
    val uptimeSeconds: Long,
    val load1: Double,
    val load5: Double,
    val load15: Double,
    val memTotal: Long,
    val memFree: Long,
    val memAvailable: Long,
    val memBuffered: Long,
    val memCached: Long
)

data class NetworkDevice(
    val hostname: String,
    val ip: String,
    val mac: String,
    val network: String,
    val expires: String
)

data class WirelessInterface(
    val ifname: String,
    val ssid: String,
    val mode: String,
    val channel: String,
    val signal: String,
    val bitrate: String
)

data class PluginInfo(
    val id: String,
    val name: String,
    val description: String,
    val installed: Boolean,
    val enabled: Boolean,
    val version: String,
    val initScript: String?,
    val category: String
)

data class ProcessInfo(
    val pid: String,
    val user: String,
    val cpu: String,
    val mem: String,
    val vsz: String,
    val rss: String,
    val tty: String,
    val stat: String,
    val start: String,
    val time: String,
    val command: String
)

data class LogLine(
    val raw: String,
    val level: String = ""
)

data class StorageVolume(
    val mount: String,
    val device: String,
    val fstype: String,
    val totalBytes: Long,
    val usedBytes: Long,
    val freeBytes: Long,
    val usedPercent: Int
)

data class NetInterfaceInfo(
    val name: String,
    val up: Boolean,
    val proto: String,
    val device: String,
    val ipv4: List<String>,
    val ipv6: List<String>,
    val mac: String,
    val rxBytes: Long,
    val txBytes: Long,
    val role: String
)


data class RouterActionResult(
    val success: Boolean,
    val message: String
)

class OpenWrtException(message: String) : Exception(message)

class OpenWrtClient(
    private val http: HttpTransport = HttpTransport()
) {
    fun login(host: String, username: String, password: String, useHttps: Boolean = false): RouterSession {
        val base = normalizeBase(host, useHttps)
        return try {
            loginInternal(base, username, password)
        } catch (e: Exception) {
            val isSsl = e is SSLException ||
                e is CertificateException ||
                e.cause is CertificateException ||
                (e.message?.contains("Trust anchor", ignoreCase = true) == true) ||
                (e.message?.contains("CertPathValidatorException", ignoreCase = true) == true) ||
                (e.message?.contains("SSLHandshakeException", ignoreCase = true) == true)

            // Auto-fallback: if plain host without scheme, try the other protocol once.
            if (!host.trim().startsWith("http://") && !host.trim().startsWith("https://")) {
                val alt = normalizeBase(host, !useHttps)
                try {
                    return loginInternal(alt, username, password)
                } catch (_: Exception) {
                    // keep original error path
                }
            }

            if (isSsl) {
                throw OpenWrtException(
                    "HTTPS 证书不受信任（路由器多为自签名证书）。" +
                        "请安装最新 APK；或取消勾选“使用 HTTPS”改用 HTTP。"
                )
            }
            if (e is OpenWrtException) throw e
            throw OpenWrtException(e.message ?: "登录失败")
        }
    }

    private fun loginInternal(base: String, username: String, password: String): RouterSession {
        // Prefer real ubus session: plugin scan and file.exec need it.
        val ubusResult = runCatching { loginViaUbus(base, username, password) }
        if (ubusResult.isSuccess) return ubusResult.getOrThrow()

        val cookie = runCatching { loginViaLuciCookie(base, username, password) }.getOrNull()
        if (!cookie.isNullOrBlank()) {
            runCatching { return loginViaUbus(base, username, password) }
            return RouterSession(baseUrl = base, username = username, authToken = cookie)
        }

        val msg = (ubusResult.exceptionOrNull() as? OpenWrtException)?.message
            ?: ubusResult.exceptionOrNull()?.message
            ?: "登录失败"
        throw OpenWrtException(msg)
    }

    private fun loginViaLuciCookie(base: String, username: String, password: String): String? {
        val response = http.postForm(
            "$base/cgi-bin/luci/",
            mapOf("luci_username" to username, "luci_password" to password)
        )
        val setCookies = response.headers("Set-Cookie")
        return setCookies
            .asSequence()
            .mapNotNull { cookieHeader ->
                cookieHeader.split(";")
                    .map { it.trim() }
                    .firstOrNull {
                        it.startsWith("sysauth=") ||
                            it.startsWith("sysauth_http=") ||
                            it.startsWith("sysauth_https=")
                    }
                    ?.substringAfter("=")
                    ?.takeIf { it.isNotBlank() }
            }
            .firstOrNull()
    }

    private fun loginViaUbus(base: String, username: String, password: String): RouterSession {
        val payload = JSONObject()
            .put("jsonrpc", "2.0")
            .put("id", 1)
            .put("method", "call")
            .put(
                "params",
                JSONArray()
                    .put("00000000000000000000000000000000")
                    .put("session")
                    .put("login")
                    .put(
                        JSONObject()
                            .put("username", username)
                            .put("password", password)
                    )
            )
            .toString()
        val response = http.postJson("$base/ubus", payload)
        run {
            val text = response.body
            if (!response.isSuccessful) {
                throw OpenWrtException("登录失败 (HTTP ${response.code})")
            }
            val root = JSONObject(text)
            if (root.has("error")) {
                throw OpenWrtException(root.getJSONObject("error").optString("message", "登录失败"))
            }
            val result = root.optJSONArray("result")
                ?: throw OpenWrtException("登录响应无效")
            val code = result.optInt(0, -1)
            if (code != 0) {
                throw OpenWrtException("用户名或密码错误")
            }
            val data = result.optJSONObject(1)
                ?: throw OpenWrtException("未获取到会话")
            val ubusToken = data.optString("ubus_rpc_session")
            if (ubusToken.isBlank()) {
                throw OpenWrtException("未获取到会话令牌")
            }
            return RouterSession(baseUrl = base, username = username, authToken = "ubus:$ubusToken")
        }
    }

    fun fetchSystemInfo(session: RouterSession): SystemInfo {
        return try {
            fetchSystemInfoUbus(session)
        } catch (_: Exception) {
            SystemInfo(
                hostname = "OpenWrt",
                model = "-",
                boardName = "-",
                releaseDescription = if (session.isUbus) "已连接" else "已登录 LuCI",
                kernelVersion = "-",
                localTime = "-",
                uptimeSeconds = 0L,
                load1 = 0.0,
                load5 = 0.0,
                load15 = 0.0,
                memTotal = 0L,
                memFree = 0L,
                memAvailable = 0L,
                memBuffered = 0L,
                memCached = 0L
            )
        }
    }

    fun fetchDevices(session: RouterSession): List<NetworkDevice> {
        return try {
            fetchDevicesUbus(session)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun fetchWireless(session: RouterSession): List<WirelessInterface> {
        return try {
            fetchWirelessUbus(session)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun reboot(session: RouterSession): RouterActionResult {
        return try {
            if (session.authToken.startsWith("ubus:")) {
                ubusCall(session, "system", "reboot", JSONObject())
            } else {
                luciRpc(session, """{"method":"exec","params":["reboot"]}""")
            }
            RouterActionResult(true, "已发送重启指令")
        } catch (e: Exception) {
            RouterActionResult(false, e.message ?: "重启失败")
        }
    }

    fun restartNetwork(session: RouterSession): RouterActionResult {
        return try {
            if (session.authToken.startsWith("ubus:")) {
                ubusCall(session, "network", "restart", JSONObject())
            } else {
                luciRpc(session, """{"method":"exec","params":["/etc/init.d/network restart"]}""")
            }
            RouterActionResult(true, "网络服务正在重启")
        } catch (e: Exception) {
            RouterActionResult(false, e.message ?: "网络重启失败")
        }
    }

    fun restartWifi(session: RouterSession): RouterActionResult {
        return try {
            if (session.authToken.startsWith("ubus:")) {
                ubusCall(
                    session,
                    "file",
                    "exec",
                    JSONObject()
                        .put("command", "/sbin/wifi")
                        .put("params", JSONArray().put("reload"))
                )
            } else {
                luciRpc(session, """{"method":"exec","params":["wifi reload"]}""")
            }
            RouterActionResult(true, "Wi-Fi 已重新加载")
        } catch (e: Exception) {
            RouterActionResult(false, e.message ?: "Wi-Fi 重载失败")
        }
    }

    private fun fetchSystemInfoUbus(session: RouterSession): SystemInfo {
        val board = ubusCall(session, "system", "board", JSONObject()).optJSONObject(1) ?: JSONObject()
        val info = ubusCall(session, "system", "info", JSONObject()).optJSONObject(1) ?: JSONObject()
        val release = board.optJSONObject("release") ?: JSONObject()
        val memory = info.optJSONObject("memory") ?: JSONObject()
        val load = info.optJSONArray("load") ?: JSONArray()
        return SystemInfo(
            hostname = board.optString("hostname", "OpenWrt"),
            model = board.optString("model", "-"),
            boardName = board.optString("board_name", "-"),
            releaseDescription = release.optString("description", release.optString("version", "-")),
            kernelVersion = board.optString("kernel", "-"),
            localTime = info.opt("localtime")?.toString() ?: "-",
            uptimeSeconds = info.optLong("uptime", 0L),
            load1 = load.optDouble(0, 0.0) / 65536.0,
            load5 = load.optDouble(1, 0.0) / 65536.0,
            load15 = load.optDouble(2, 0.0) / 65536.0,
            memTotal = memory.optLong("total", 0L),
            memFree = memory.optLong("free", 0L),
            memAvailable = memory.optLong("available", memory.optLong("free", 0L)),
            memBuffered = memory.optLong("buffered", 0L),
            memCached = memory.optLong("cached", 0L)
        )
    }

    private fun fetchDevicesUbus(session: RouterSession): List<NetworkDevice> {
        val result = ubusCall(session, "luci-rpc", "getDHCPLeases", JSONObject())
        val data = result.optJSONObject(1) ?: return emptyList()
        val leases = data.optJSONArray("dhcp_leases") ?: JSONArray()
        val list = mutableListOf<NetworkDevice>()
        for (i in 0 until leases.length()) {
            val item = leases.optJSONObject(i) ?: continue
            list += NetworkDevice(
                hostname = item.optString("hostname", item.optString("name", "")),
                ip = item.optString("ipaddr", item.optString("ip", "-")),
                mac = item.optString("macaddr", item.optString("mac", "-")),
                network = item.optString("device", item.optString("network", "lan")),
                expires = item.opt("expires")?.toString() ?: item.optString("leasetime", "-")
            )
        }
        if (list.isNotEmpty()) return list

        val hints = runCatching {
            ubusCall(session, "luci-rpc", "getHostHints", JSONObject()).optJSONObject(1)
        }.getOrNull() ?: return emptyList()
        val keys = hints.keys()
        while (keys.hasNext()) {
            val mac = keys.next()
            val obj = hints.optJSONObject(mac) ?: continue
            val ipAddrs = obj.optJSONArray("ipaddrs") ?: JSONArray()
            val names = obj.optJSONArray("name") ?: JSONArray()
            list += NetworkDevice(
                hostname = names.optString(0, ""),
                ip = ipAddrs.optString(0, "-"),
                mac = mac,
                network = "lan",
                expires = "-"
            )
        }
        return list
    }

    private fun fetchWirelessUbus(session: RouterSession): List<WirelessInterface> {
        val result = runCatching {
            ubusCall(session, "luci-rpc", "getWirelessDevices", JSONObject())
        }.getOrNull() ?: return emptyList()
        val data = result.optJSONObject(1) ?: return emptyList()
        val list = mutableListOf<WirelessInterface>()
        val radios = data.keys()
        while (radios.hasNext()) {
            val radio = radios.next()
            val radioObj = data.optJSONObject(radio) ?: continue
            val interfaces = radioObj.optJSONObject("interfaces") ?: continue
            val ifKeys = interfaces.keys()
            while (ifKeys.hasNext()) {
                val key = ifKeys.next()
                val iface = interfaces.optJSONObject(key) ?: continue
                val config = iface.optJSONObject("config") ?: JSONObject()
                val iwinfo = iface.optJSONObject("iwinfo") ?: JSONObject()
                list += WirelessInterface(
                    ifname = iwinfo.optString("ifname", key),
                    ssid = config.optString("ssid", iwinfo.optString("ssid", "-")),
                    mode = config.optString("mode", iwinfo.optString("mode", "-")),
                    channel = iwinfo.opt("channel")?.toString()
                        ?: config.opt("channel")?.toString()
                        ?: "-",
                    signal = iwinfo.opt("signal")?.toString() ?: "-",
                    bitrate = iwinfo.opt("bitrate")?.toString() ?: "-"
                )
            }
        }
        return list
    }

    fun listPlugins(session: RouterSession): List<PluginInfo> {
        // name/description/category store i18n keys (or brand names)
        val known = listOf(
            Triple("luci-app-lucky", "Lucky", "plugin_desc_lucky"),
            Triple("lucky", "plugin_name_lucky_core", "plugin_desc_lucky_core"),
            Triple("luci-app-passwall", "Passwall", "plugin_desc_passwall"),
            Triple("luci-app-passwall2", "Passwall2", "plugin_desc_passwall2"),
            Triple("luci-app-ssr-plus", "SSR Plus+", "plugin_desc_ssr"),
            Triple("luci-app-openclash", "OpenClash", "plugin_desc_openclash"),
            Triple("luci-app-homeproxy", "HomeProxy", "plugin_desc_homeproxy"),
            Triple("luci-app-nikki", "Nikki", "plugin_desc_nikki"),
            Triple("luci-app-mosdns", "MosDNS", "plugin_desc_mosdns"),
            Triple("luci-app-adguardhome", "AdGuard Home", "plugin_desc_adguard"),
            Triple("adguardhome", "plugin_name_adguard_core", "plugin_desc_adguard_core"),
            Triple("luci-app-ddns", "DDNS", "plugin_desc_ddns"),
            Triple("luci-app-upnp", "UPnP", "plugin_desc_upnp"),
            Triple("luci-app-samba4", "Samba", "plugin_desc_samba"),
            Triple("luci-app-aria2", "Aria2", "plugin_desc_aria2"),
            Triple("luci-app-qbittorrent", "qBittorrent", "plugin_desc_qbit"),
            Triple("luci-app-docker", "Docker", "plugin_desc_docker"),
            Triple("luci-app-ttyd", "TTYD", "plugin_desc_ttyd"),
            Triple("luci-app-nlbwmon", "plugin_name_nlbwmon", "plugin_desc_nlbwmon"),
            Triple("luci-app-statistics", "plugin_name_statistics", "plugin_desc_statistics"),
            Triple("luci-app-firewall", "plugin_name_firewall", "plugin_desc_firewall"),
            Triple("luci-app-opkg", "plugin_name_opkg", "plugin_desc_opkg")
        )

        // Many OpenWrt builds deny file.exec (ubus code 6). Prefer file.list / file.stat / file.read.
        val errors = mutableListOf<String>()
        val opkgPackages = linkedMapOf<String, String>()
        val initScripts = linkedSetOf<String>()
        val rcEnabled = linkedSetOf<String>()
        val pathHits = linkedSetOf<String>()

        // 1) opkg package list via control files (no shell needed)
        runCatching {
            val entries = fileListNames(session, "/usr/lib/opkg/info")
            entries.forEach { name ->
                when {
                    name.endsWith(".control") -> {
                        val pkg = name.removeSuffix(".control")
                        val version = runCatching {
                            parseControlVersion(fileRead(session, "/usr/lib/opkg/info/$name", 4096))
                        }.getOrNull()
                        opkgPackages[pkg] = version ?: "installed"
                    }
                    name.contains("lucky", ignoreCase = true) -> {
                        pathHits += "OPKGINFO:$name"
                        if (name.endsWith(".list") || name.endsWith(".control")) {
                            val pkg = name.substringBeforeLast('.')
                            opkgPackages.putIfAbsent(pkg, "installed")
                        }
                    }
                }
            }
        }.onFailure { errors += "opkg信息: ${it.message}" }

        // 2) Fallback: partial read of /usr/lib/opkg/status (large file; only first chunk)
        if (opkgPackages.isEmpty()) {
            runCatching {
                val status = fileRead(session, "/usr/lib/opkg/status", 256 * 1024)
                parseOpkgStatus(status).forEach { (k, v) -> opkgPackages[k] = v }
            }.onFailure { errors += "opkg状态: ${it.message}" }
        }

        // 3) init.d scripts
        runCatching {
            fileListNames(session, "/etc/init.d").forEach { initScripts += it }
        }.onFailure { errors += "init.d: ${it.message}" }

        // 4) rc.d enabled services (Sxxname)
        runCatching {
            fileListNames(session, "/etc/rc.d").forEach { name ->
                val m = Regex("^S\\d+(.+)$").find(name)
                if (m != null) rcEnabled += m.groupValues[1]
            }
        }.onFailure { /* optional */ }

        // 5) Lucky / plugin path probes via file.stat
        val probePaths = listOf(
            "/etc/config/lucky",
            "/etc/init.d/lucky",
            "/usr/bin/lucky",
            "/usr/sbin/lucky",
            "/opt/lucky/lucky",
            "/usr/lib/lua/luci/controller/lucky.lua",
            "/usr/lib/lua/luci/controller/admin/lucky.lua",
            "/usr/share/luci/menu.d/luci-app-lucky.json",
            "/www/luci-static/resources/view/lucky",
            "/www/luci-static/resources/view/lucky.js",
            "/usr/libexec/rpcd/lucky",
            "/etc/uci-defaults/luci-lucky"
        )
        probePaths.forEach { p ->
            if (fileExists(session, p)) pathHits += "EXISTS:$p"
        }

        // 6) LuCI controller / menu directory listings
        runCatching {
            fileListNames(session, "/usr/lib/lua/luci/controller")
                .filter { it.contains("lucky", ignoreCase = true) }
                .forEach { pathHits += "CTRL:$it" }
        }
        runCatching {
            fileListNames(session, "/usr/share/luci/menu.d")
                .filter { it.contains("lucky", ignoreCase = true) }
                .forEach { pathHits += "MENU:$it" }
        }

        // 7) Optional: try file.exec once only if list/stat produced nothing useful for lucky
        // (kept last; many routers deny it with code=6)
        var execProbe = ""
        val needExecBoost = opkgPackages.keys.none { it.contains("lucky", true) } &&
            pathHits.none { it.contains("lucky", true) } &&
            initScripts.none { it.contains("lucky", true) }
        if (needExecBoost) {
            runCatching {
                execProbe = execCommand(
                    session,
                    "/bin/sh",
                    listOf(
                        "-c",
                        "opkg list-installed 2>/dev/null | grep -i lucky; " +
                            "ls /etc/init.d 2>/dev/null | grep -i lucky; " +
                            "ls /usr/lib/opkg/info 2>/dev/null | grep -i lucky"
                    )
                )
            }.onFailure {
                // code 6 is expected on locked-down images; do not fail whole scan
                if (it.message?.contains("code=6") == true) {
                    // ignore
                } else {
                    errors += "exec: ${it.message}"
                }
            }
        }

        val pathsText = pathHits.joinToString("\n")
        val statusText = buildString {
            opkgPackages.forEach { (k, v) -> append(k).append(" - ").append(v).append('\n') }
            if (execProbe.isNotBlank()) append(execProbe)
        }
        val installedMap = opkgPackages.toMap()
        if (execProbe.isNotBlank()) {
            parseOpkgInstalled(execProbe).forEach { (k, v) ->
                // merge rough lines
            }
            execProbe.lineSequence().forEach { line ->
                val t = line.trim()
                if (t.contains("lucky", true)) {
                    pathHits += "EXEC:$t"
                    val parts = t.split(Regex("\\s+-\\s+"), limit = 2)
                    if (parts.isNotEmpty() && parts[0].isNotBlank()) {
                        opkgPackages.putIfAbsent(parts[0], parts.getOrElse(1) { "installed" })
                    } else if (t.isNotBlank() && !t.contains('/')) {
                        initScripts += t
                    }
                }
            }
        }

        val luckyInstalled = detectLuckyInstalled(
            installedMap = opkgPackages,
            statusText = statusText + "\n" + pathsText + "\n" + execProbe,
            enabledScripts = initScripts,
            pathsText = pathsText + "\n" + execProbe,
            psText = "",
            uciText = ""
        )

        // If every source failed hard, surface error
        if (opkgPackages.isEmpty() && initScripts.isEmpty() && pathHits.isEmpty() && execProbe.isBlank()) {
            val detail = errors.joinToString("；").ifBlank { "无可用 ubus file 权限" }
            throw OpenWrtException(
                "插件扫描失败：$detail。当前路由器可能限制了 file.list/file.read/file.exec。" +
                    "可在路由器添加 ACL 允许 root 调用 file.*，或至少允许 file.list 与 file.stat。"
            )
        }

        return known.map { (id, name, desc) ->
            val installed = when {
                id == "luci-app-lucky" || id == "lucky" -> {
                    opkgPackages.containsKey(id) ||
                        statusText.contains(id) ||
                        luckyInstalled ||
                        (id == "lucky" && initScripts.any { it.equals("lucky", true) }) ||
                        (id == "luci-app-lucky" && (
                            opkgPackages.keys.any { it.contains("luci-app-lucky", true) } ||
                                pathHits.any { it.contains("lucky", true) }
                            ))
                }
                else -> {
                    opkgPackages.containsKey(id) ||
                        statusText.contains(id) ||
                        (id == "adguardhome" && initScripts.any { it.equals("adguardhome", true) })
                }
            }
            val version = opkgPackages[id]
                ?: when {
                    !installed -> "-"
                    id.contains("lucky") && luckyInstalled -> "status_detected"
                    else -> "status_installed_plain"
                }
            val init = guessInitScript(id, initScripts)
            val enabled = when {
                !installed -> false
                init != null && rcEnabled.contains(init) -> true
                init != null && initScripts.contains(init) -> rcEnabled.isEmpty() || rcEnabled.contains(init)
                else -> false
            }
            PluginInfo(
                id = id,
                name = name,
                description = desc,
                installed = installed,
                enabled = installed && enabled,
                version = version,
                initScript = init,
                category = when {
                    id.contains("lucky") || id.contains("passwall") || id.contains("ssr") ||
                        id.contains("clash") || id.contains("homeproxy") || id.contains("nikki") -> "cat_proxy"
                    id.contains("adguard") || id.contains("mosdns") || id.contains("ddns") -> "cat_dns"
                    id.contains("aria2") || id.contains("qbit") || id.contains("samba") || id.contains("docker") -> "cat_storage"
                    else -> "cat_system"
                }
            )
        }.sortedWith(
            compareByDescending<PluginInfo> { it.installed }
                .thenBy { it.category }
                .thenBy { it.name }
        )
    }

    private fun detectLuckyInstalled(
        installedMap: Map<String, String>,
        statusText: String,
        enabledScripts: Set<String>,
        pathsText: String,
        psText: String,
        uciText: String
    ): Boolean {
        if (installedMap.keys.any { it.contains("lucky", ignoreCase = true) }) return true
        if (statusText.contains("lucky", ignoreCase = true)) return true
        if (enabledScripts.any { it.contains("lucky", ignoreCase = true) }) return true
        if (pathsText.contains("EXISTS:", ignoreCase = true) && pathsText.contains("lucky", ignoreCase = true)) return true
        if (pathsText.contains("CTRL:", ignoreCase = true)) return true
        if (pathsText.contains("MENU:", ignoreCase = true)) return true
        if (pathsText.contains("OPKGINFO:", ignoreCase = true)) return true
        if (pathsText.contains("EXEC:", ignoreCase = true) && pathsText.contains("lucky", ignoreCase = true)) return true
        if (psText.contains("lucky", ignoreCase = true)) return true
        if (uciText.contains("lucky", ignoreCase = true)) return true
        return false
    }

    private fun fileListNames(session: RouterSession, path: String): List<String> {
        val result = ubusCall(
            session,
            "file",
            "list",
            JSONObject().put("path", path)
        )
        val data = result.optJSONObject(1) ?: return emptyList()
        val entries = data.optJSONArray("entries")
        if (entries != null) {
            val out = mutableListOf<String>()
            for (i in 0 until entries.length()) {
                val item = entries.opt(i)
                when (item) {
                    is JSONObject -> {
                        val n = item.optString("name")
                        if (n.isNotBlank()) out += n
                    }
                    is String -> if (item.isNotBlank()) out += item
                }
            }
            return out
        }
        // older shape: array of objects directly under result[1] keys
        val names = mutableListOf<String>()
        val keys = data.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            if (k == "entries") continue
            names += k
        }
        return names
    }

    private fun fileExists(session: RouterSession, path: String): Boolean {
        return runCatching {
            val result = ubusCall(
                session,
                "file",
                "stat",
                JSONObject().put("path", path)
            )
            val data = result.optJSONObject(1)
            data != null && (data.has("type") || data.has("size") || data.has("mode"))
        }.getOrDefault(false)
    }

    private fun fileRead(session: RouterSession, path: String, maxBytes: Int = 8192): String {
        val params = JSONObject()
            .put("path", path)
            .put("base64", false)
        // some rpcd-mod-file builds support length/offset
        if (maxBytes > 0) {
            params.put("length", maxBytes)
        }
        val result = ubusCall(session, "file", "read", params)
        val data = result.optJSONObject(1) ?: return ""
        return data.optString("data", data.optString("content", ""))
    }

    private fun parseControlVersion(controlText: String): String? {
        controlText.lineSequence().forEach { line ->
            val t = line.trim()
            if (t.startsWith("Version:", ignoreCase = true)) {
                return t.substringAfter(':').trim().ifBlank { null }
            }
        }
        return null
    }

    private fun parseOpkgStatus(statusText: String): Map<String, String> {
        val map = linkedMapOf<String, String>()
        var pkg: String? = null
        var version: String? = null
        fun flush() {
            val p = pkg
            if (!p.isNullOrBlank()) {
                map[p] = version ?: "installed"
            }
            pkg = null
            version = null
        }
        statusText.lineSequence().forEach { line ->
            val t = line.trim()
            when {
                t.isEmpty() -> flush()
                t.startsWith("Package:", ignoreCase = true) -> {
                    flush()
                    pkg = t.substringAfter(':').trim()
                }
                t.startsWith("Version:", ignoreCase = true) -> {
                    version = t.substringAfter(':').trim()
                }
            }
        }
        flush()
        return map
    }

    fun setPluginEnabled(session: RouterSession, plugin: PluginInfo, enable: Boolean): RouterActionResult {
        val init = plugin.initScript
            ?: return RouterActionResult(false, "未找到可控制的服务脚本")
        return try {
            if (enable) {
                execCommand(session, "/etc/init.d/$init", listOf("enable"))
                execCommand(session, "/etc/init.d/$init", listOf("start"))
                RouterActionResult(true, "${plugin.name} 已启用并启动")
            } else {
                execCommand(session, "/etc/init.d/$init", listOf("stop"))
                execCommand(session, "/etc/init.d/$init", listOf("disable"))
                RouterActionResult(true, "${plugin.name} 已停止并禁用")
            }
        } catch (e: Exception) {
            val msg = e.message ?: "插件操作失败"
            RouterActionResult(
                false,
                if (msg.contains("code=6")) {
                    "路由器禁止 file.exec（ACL code=6），无法启停服务。可检测插件，但控制服务需要放行 ubus file.exec。"
                } else msg
            )
        }
    }

    fun restartPlugin(session: RouterSession, plugin: PluginInfo): RouterActionResult {
        val init = plugin.initScript
            ?: return RouterActionResult(false, "未找到可控制的服务脚本")
        return try {
            execCommand(session, "/etc/init.d/$init", listOf("restart"))
            RouterActionResult(true, "${plugin.name} 已重启")
        } catch (e: Exception) {
            val msg = e.message ?: "重启失败"
            RouterActionResult(
                false,
                if (msg.contains("code=6")) {
                    "路由器禁止 file.exec（ACL code=6），无法重启服务。"
                } else msg
            )
        }
    }

    private fun guessInitScript(pkgId: String, scripts: Set<String>): String? {
        val candidates = when (pkgId) {
            "luci-app-lucky", "lucky" -> listOf("lucky")
            "luci-app-passwall" -> listOf("passwall")
            "luci-app-passwall2" -> listOf("passwall2")
            "luci-app-ssr-plus" -> listOf("shadowsocksr")
            "luci-app-openclash" -> listOf("openclash")
            "luci-app-homeproxy" -> listOf("homeproxy")
            "luci-app-nikki" -> listOf("nikki", "mihomo")
            "luci-app-mosdns" -> listOf("mosdns")
            "luci-app-adguardhome", "adguardhome" -> listOf("adguardhome")
            "luci-app-ddns" -> listOf("ddns")
            "luci-app-upnp" -> listOf("miniupnpd")
            "luci-app-samba4" -> listOf("samba4", "samba")
            "luci-app-aria2" -> listOf("aria2")
            "luci-app-qbittorrent" -> listOf("qbittorrent")
            "luci-app-docker" -> listOf("dockerd", "docker")
            "luci-app-ttyd" -> listOf("ttyd")
            "luci-app-nlbwmon" -> listOf("nlbwmon")
            "luci-app-statistics" -> listOf("collectd")
            else -> emptyList()
        }
        candidates.firstOrNull { name -> scripts.any { it.equals(name, ignoreCase = true) } }
            ?.let { wanted -> return scripts.first { it.equals(wanted, ignoreCase = true) } }
        if (pkgId.contains("lucky", ignoreCase = true)) {
            return scripts.firstOrNull { it.contains("lucky", ignoreCase = true) }
        }
        return null
    }

    private fun parseOpkgInstalled(text: String): Map<String, String> {
        val map = linkedMapOf<String, String>()
        // formats: "pkg - 1.2.3" or "pkg - 1.2.3-r1"
        text.lineSequence().forEach { line ->
            val t = line.trim()
            if (t.isEmpty()) return@forEach
            val parts = t.split(Regex("\\s+-\\s+"), limit = 2)
            if (parts.size == 2) {
                map[parts[0].trim()] = parts[1].trim()
            } else {
                val sp = t.split(Regex("\\s+"), limit = 2)
                if (sp.isNotEmpty()) map[sp[0]] = sp.getOrNull(1) ?: "installed"
            }
        }
        return map
    }


    fun listProcesses(session: RouterSession): List<ProcessInfo> {
        val errors = mutableListOf<String>()
        // busybox ps variants
        val tries = listOf(
            "ps" to listOf("w"),
            "ps" to listOf("-w"),
            "ps" to emptyList(),
            "ps" to listOf("aux")
        )
        for ((cmd, params) in tries) {
            try {
                val out = execCommand(session, cmd, params)
                val parsed = parsePs(out)
                if (parsed.isNotEmpty()) return parsed
            } catch (e: Exception) {
                errors += "${cmd}${params}: ${e.message}"
            }
        }
        // /proc fallback: list numeric dirs + cmdline/status (no full CPU)
        return try {
            listProcessesFromProc(session)
        } catch (e: Exception) {
            throw OpenWrtException(
                "无法获取进程列表。${errors.take(2).joinToString("；")}；proc: ${e.message}"
            )
        }
    }

    fun fetchSystemLogs(session: RouterSession, lines: Int = 200): List<LogLine> {
        val limit = lines.coerceIn(50, 1000)
        val errors = mutableListOf<String>()

        // Prefer ubus log.read (no file.exec ACL needed on most builds)
        try {
            val result = ubusCall(
                session,
                "log",
                "read",
                JSONObject()
                    .put("lines", limit)
                    .put("stream", false)
            )
            val data = result.optJSONObject(1)
            val arr = data?.optJSONArray("log") ?: data?.optJSONArray("lines")
            if (arr != null && arr.length() > 0) {
                val out = ArrayList<LogLine>(arr.length())
                for (i in 0 until arr.length()) {
                    val item = arr.opt(i)
                    when (item) {
                        is JSONObject -> {
                            val msg = item.optString("msg", item.optString("message", item.toString()))
                            val prio = item.optInt("priority", item.optInt("prio", -1))
                            val facility = item.optString("facility", "")
                            val time = item.optString("time", item.optString("timestamp", ""))
                            val level = priorityToLevel(prio)
                            val raw = buildString {
                                if (time.isNotBlank()) append(time).append(' ')
                                if (facility.isNotBlank()) append(facility).append(": ")
                                if (level.isNotBlank()) append(level).append(": ")
                                append(msg)
                            }
                            out += LogLine(raw = raw, level = level)
                        }
                        else -> {
                            val line = item?.toString().orEmpty()
                            if (line.isNotBlank()) {
                                out += parseLogLines(line, 1).firstOrNull() ?: LogLine(raw = line)
                            }
                        }
                    }
                }
                if (out.isNotEmpty()) return if (out.size > limit) out.takeLast(limit) else out
            }
            val plain = data?.optString("data", "").orEmpty()
            if (plain.isNotBlank()) {
                val parsed = parseLogLines(plain, limit)
                if (parsed.isNotEmpty()) return parsed
            }
        } catch (e: Exception) {
            errors += "log.read: ${e.message}"
        }

        // file.exec may be ACL-denied (code=6); still try for open builds
        val cmds = listOf(
            "logread" to listOf("-l", limit.toString()),
            "logread" to emptyList(),
            "dmesg" to emptyList()
        )
        for ((cmd, params) in cmds) {
            try {
                val out = execCommand(session, cmd, params)
                val parsed = parseLogLines(out, limit)
                if (parsed.isNotEmpty()) return parsed
            } catch (e: Exception) {
                errors += "$cmd: ${e.message}"
            }
        }

        val files = listOf(
            "/var/log/messages",
            "/tmp/log/messages",
            "/var/log/syslog",
            "/tmp/system.log",
            "/var/log/system.log",
            "/overlay/upper/var/log/messages"
        )
        for (path in files) {
            try {
                if (!fileExists(session, path)) continue
                val text = fileRead(session, path, maxBytes = 96 * 1024)
                val parsed = parseLogLines(text, limit)
                if (parsed.isNotEmpty()) return parsed
            } catch (e: Exception) {
                errors += "$path: ${e.message}"
            }
        }
        try {
            if (fileExists(session, "/proc/kmsg")) {
                val text = fileRead(session, "/proc/kmsg", maxBytes = 32 * 1024)
                val parsed = parseLogLines(text, limit)
                if (parsed.isNotEmpty()) return parsed
            }
        } catch (e: Exception) {
            errors += "kmsg: ${e.message}"
        }
        throw OpenWrtException(
            "无法读取系统日志（路由器可能限制 file.exec）。${errors.take(3).joinToString("；")}。可在 SSH 中执行 logread。"
        )
    }

    private fun priorityToLevel(prio: Int): String = when (prio) {
        0 -> "EMERG"
        1 -> "ALERT"
        2 -> "CRIT"
        3 -> "ERR"
        4 -> "WARN"
        5 -> "NOTICE"
        6 -> "INFO"
        7 -> "DEBUG"
        else -> ""
    }

    private fun listProcessesFromProc(session: RouterSession): List<ProcessInfo> {
        val names = fileListNames(session, "/proc")
        val pids = names.filter { it.all(Char::isDigit) }.sortedBy { it.toIntOrNull() ?: 0 }
        val result = ArrayList<ProcessInfo>(pids.size.coerceAtMost(120))
        for (pid in pids.take(120)) {
            val cmdline = runCatching {
                fileRead(session, "/proc/$pid/cmdline", 512).replace('\u0000', ' ').trim()
            }.getOrDefault("")
            val status = runCatching { fileRead(session, "/proc/$pid/status", 2048) }.getOrDefault("")
            val name = status.lineSequence()
                .firstOrNull { it.startsWith("Name:") }
                ?.substringAfter(":")
                ?.trim()
                .orEmpty()
            val rssKb = status.lineSequence()
                .firstOrNull { it.startsWith("VmRSS:") }
                ?.substringAfter(":")
                ?.trim()
                ?.substringBefore(" ")
                .orEmpty()
            val user = status.lineSequence()
                .firstOrNull { it.startsWith("Uid:") }
                ?.substringAfter(":")
                ?.trim()
                ?.split(Regex("\\s+"))
                ?.firstOrNull()
                .orEmpty()
            val cmd = cmdline.ifBlank { name.ifBlank { "[$pid]" } }
            result += ProcessInfo(
                pid = pid,
                user = user.ifBlank { "-" },
                cpu = "-",
                mem = "-",
                vsz = "-",
                rss = if (rssKb.isBlank()) "-" else rssKb,
                tty = "-",
                stat = "-",
                start = "-",
                time = "-",
                command = cmd
            )
        }
        if (result.isEmpty()) throw OpenWrtException(" /proc 无可用进程信息")
        return result
    }

    private fun parsePs(text: String): List<ProcessInfo> {
        val lines = text.lineSequence().map { it.trimEnd() }.filter { it.isNotBlank() }.toList()
        if (lines.isEmpty()) return emptyList()
        val body = if (lines.first().contains("PID", ignoreCase = true) ||
            lines.first().contains("USER", ignoreCase = true)
        ) lines.drop(1) else lines
        val out = ArrayList<ProcessInfo>(body.size)
        for (line in body) {
            val parts = line.trim().split(Regex("\\s+"))
            if (parts.isEmpty()) continue
            // busybox: USER PID %CPU %MEM VSZ RSS TTY STAT START TIME COMMAND
            // or: PID USER VSZ STAT COMMAND
            if (parts[0].all(Char::isDigit)) {
                // PID first
                val pid = parts[0]
                if (parts.size >= 5) {
                    out += ProcessInfo(
                        pid = pid,
                        user = parts.getOrElse(1) { "-" },
                        cpu = "-",
                        mem = "-",
                        vsz = parts.getOrElse(2) { "-" },
                        rss = "-",
                        tty = "-",
                        stat = parts.getOrElse(3) { "-" },
                        start = "-",
                        time = "-",
                        command = parts.drop(4).joinToString(" ").ifBlank { "-" }
                    )
                } else continue
            } else if (parts.size >= 11 && parts[1].all(Char::isDigit)) {
                out += ProcessInfo(
                    pid = parts[1],
                    user = parts[0],
                    cpu = parts.getOrElse(2) { "-" },
                    mem = parts.getOrElse(3) { "-" },
                    vsz = parts.getOrElse(4) { "-" },
                    rss = parts.getOrElse(5) { "-" },
                    tty = parts.getOrElse(6) { "-" },
                    stat = parts.getOrElse(7) { "-" },
                    start = parts.getOrElse(8) { "-" },
                    time = parts.getOrElse(9) { "-" },
                    command = parts.drop(10).joinToString(" ").ifBlank { "-" }
                )
            } else if (parts.size >= 2) {
                // last resort: find first numeric as pid
                val pidIdx = parts.indexOfFirst { it.all(Char::isDigit) && it.length <= 6 }
                if (pidIdx < 0) continue
                out += ProcessInfo(
                    pid = parts[pidIdx],
                    user = if (pidIdx > 0) parts[0] else "-",
                    cpu = "-",
                    mem = "-",
                    vsz = "-",
                    rss = "-",
                    tty = "-",
                    stat = "-",
                    start = "-",
                    time = "-",
                    command = parts.drop(pidIdx + 1).joinToString(" ").ifBlank { parts.last() }
                )
            }
        }
        return out.distinctBy { it.pid + "|" + it.command }
    }

    private fun parseLogLines(text: String, limit: Int): List<LogLine> {
        val lines = text.lineSequence().map { it.trimEnd() }.filter { it.isNotBlank() }.toList()
        if (lines.isEmpty()) return emptyList()
        val take = if (lines.size > limit) lines.takeLast(limit) else lines
        return take.map { line ->
            val upper = line.uppercase()
            val level = when {
                " EMERG" in upper || upper.contains("<0>") -> "EMERG"
                " ALERT" in upper || upper.contains("<1>") -> "ALERT"
                " CRIT" in upper || upper.contains("<2>") -> "CRIT"
                " ERR" in upper || "ERROR" in upper || upper.contains("<3>") -> "ERR"
                " WARN" in upper || upper.contains("<4>") -> "WARN"
                " NOTICE" in upper || upper.contains("<5>") -> "NOTICE"
                " INFO" in upper || upper.contains("<6>") -> "INFO"
                " DEBUG" in upper || upper.contains("<7>") -> "DEBUG"
                else -> ""
            }
            LogLine(raw = line, level = level)
        }
    }

    private fun execCommand(session: RouterSession, command: String, params: List<String>): String {
        val args = JSONArray()
        params.forEach { args.put(it) }
        val result = ubusCall(
            session,
            "file",
            "exec",
            JSONObject()
                .put("command", command)
                .put("params", args)
                .put("env", JSONObject())
        )
        val data = result.optJSONObject(1) ?: JSONObject()
        val stdout = data.optString("stdout", "")
        val stderr = data.optString("stderr", "")
        val code = data.optInt("code", 0)
        return buildString {
            append(stdout)
            if (stderr.isNotBlank()) {
                if (isNotEmpty()) append('\n')
                append(stderr)
            }
            if (code != 0 && isEmpty()) append("code=$code")
        }
    }


    fun fetchStorage(session: RouterSession): List<StorageVolume> {
        // Merge multiple sources: many builds deny file.exec (df) and/or lack luci.getMountPoints.
        val merged = linkedMapOf<String, StorageVolume>()
        fun absorb(list: List<StorageVolume>) {
            list.forEach { vol ->
                val key = vol.mount.ifBlank { vol.device }.ifBlank { return@forEach }
                val prev = merged[key]
                if (prev == null) {
                    merged[key] = vol
                } else if (prev.totalBytes <= 0L && vol.totalBytes > 0L) {
                    merged[key] = vol
                } else if (prev.fstype == "-" && vol.fstype != "-") {
                    merged[key] = prev.copy(
                        fstype = vol.fstype,
                        device = if (prev.device == "-" || prev.device.isBlank()) vol.device else prev.device
                    )
                }
            }
        }
        absorb(runCatching { fetchStorageLuci(session) }.getOrDefault(emptyList()))
        absorb(runCatching { fetchStorageDf(session) }.getOrDefault(emptyList()))
        absorb(runCatching { fetchStorageBlock(session) }.getOrDefault(emptyList()))
        absorb(runCatching { fetchStorageFromProc(session) }.getOrDefault(emptyList()))
        return merged.values
            .filter { isInterestingMount(it) }
            .sortedWith(
                compareBy<StorageVolume> {
                    when (it.mount) {
                        "/" -> 0
                        "/overlay" -> 1
                        "/tmp" -> 2
                        else -> if (it.mount.startsWith("/mnt") || it.mount.startsWith("/media")) 3 else 4
                    }
                }.thenBy { it.mount }
            )
    }

    private fun isInterestingMount(vol: StorageVolume): Boolean {
        val m = vol.mount
        if (m.isBlank() || m == "-") return false
        if (m.startsWith("/sys") || m.startsWith("/proc") || m == "/dev" || m.startsWith("/dev/")) return false
        if (m == "/tmp/run" || m == "/var/run" || m == "/run" || m.startsWith("/run/")) return false
        // Keep real disks even without size; drop pure tiny tmpfs noise unless large.
        if (vol.fstype.contains("tmpfs", true) || vol.fstype.contains("ramfs", true)) {
            if (m != "/tmp" && vol.totalBytes < 8L * 1024 * 1024) return false
        }
        return true
    }

    private fun jsonLong(o: org.json.JSONObject, vararg keys: String): Long {
        for (k in keys) {
            if (!o.has(k)) continue
            val v = o.opt(k) ?: continue
            when (v) {
                is Number -> return v.toLong()
                is String -> v.trim().removeSuffix("B").removeSuffix("b").toLongOrNull()?.let { return it }
            }
        }
        return 0L
    }

    private fun jsonInt(o: org.json.JSONObject, vararg keys: String): Int {
        for (k in keys) {
            if (!o.has(k)) continue
            val v = o.opt(k) ?: continue
            when (v) {
                is Number -> return v.toInt()
                is String -> v.trim().removeSuffix("%").toIntOrNull()?.let { return it }
            }
        }
        return 0
    }

    fun fetchNetworkOverview(session: RouterSession): List<NetInterfaceInfo> {
        val list = mutableListOf<NetInterfaceInfo>()
        // network.interface dump
        runCatching {
            val result = ubusCall(session, "network.interface", "dump", JSONObject())
            val data = result.optJSONObject(1) ?: return@runCatching
            val ifaces = data.optJSONArray("interface") ?: JSONArray()
            for (i in 0 until ifaces.length()) {
                val obj = ifaces.optJSONObject(i) ?: continue
                val name = obj.optString("interface", obj.optString("name", "-"))
                val up = obj.optBoolean("up", false)
                val proto = obj.optString("proto", "-")
                val device = obj.optString("device", obj.optString("l3_device", "-"))
                val ipv4 = mutableListOf<String>()
                val ipv6 = mutableListOf<String>()
                val v4 = obj.optJSONArray("ipv4-address") ?: JSONArray()
                for (j in 0 until v4.length()) {
                    val a = v4.optJSONObject(j) ?: continue
                    val ip = a.optString("address")
                    val mask = a.optInt("mask", -1)
                    if (ip.isNotBlank()) ipv4 += if (mask >= 0) "$ip/$mask" else ip
                }
                val v6 = obj.optJSONArray("ipv6-address") ?: JSONArray()
                for (j in 0 until v6.length()) {
                    val a = v6.optJSONObject(j) ?: continue
                    val ip = a.optString("address")
                    val mask = a.optInt("mask", -1)
                    if (ip.isNotBlank()) ipv6 += if (mask >= 0) "$ip/$mask" else ip
                }
                // also ipv6-prefix-assignment addresses
                val v6p = obj.optJSONArray("ipv6-prefix-assignment") ?: JSONArray()
                for (j in 0 until v6p.length()) {
                    val a = v6p.optJSONObject(j) ?: continue
                    val local = a.optJSONObject("local-address")
                    val ip = local?.optString("address").orEmpty()
                    if (ip.isNotBlank()) ipv6 += ip
                }
                val role = when {
                    name.equals("wan", true) || name.startsWith("wan", true) -> "role_upstream"
                    name.equals("lan", true) || name.startsWith("lan", true) -> "role_lan"
                    name.contains("wwan", true) || proto.contains("modem", true) -> "role_mobile"
                    else -> "role_iface"
                }
                list += NetInterfaceInfo(
                    name = name,
                    up = up,
                    proto = proto,
                    device = device,
                    ipv4 = ipv4,
                    ipv6 = ipv6,
                    mac = obj.optString("macaddr", "-"),
                    rxBytes = obj.optJSONObject("statistics")?.optLong("rx_bytes", 0L) ?: 0L,
                    txBytes = obj.optJSONObject("statistics")?.optLong("tx_bytes", 0L) ?: 0L,
                    role = role
                )
            }
        }
        if (list.isNotEmpty()) return list.sortedWith(compareByDescending<NetInterfaceInfo> { it.role == "role_upstream" }.thenBy { it.name })
        // fallback luci-rpc getNetworkDevices
        runCatching {
            val result = ubusCall(session, "luci-rpc", "getNetworkDevices", JSONObject())
            val data = result.optJSONObject(1) ?: return@runCatching
            val keys = data.keys()
            while (keys.hasNext()) {
                val name = keys.next()
                val obj = data.optJSONObject(name) ?: continue
                val ipaddrs = obj.optJSONArray("ipaddrs") ?: JSONArray()
                val ip6addrs = obj.optJSONArray("ip6addrs") ?: JSONArray()
                val v4 = (0 until ipaddrs.length()).mapNotNull { ipaddrs.optString(it).takeIf { s -> s.isNotBlank() } }
                val v6 = (0 until ip6addrs.length()).mapNotNull { ip6addrs.optString(it).takeIf { s -> s.isNotBlank() } }
                list += NetInterfaceInfo(
                    name = name,
                    up = obj.optBoolean("up", true),
                    proto = obj.optString("type", "-"),
                    device = name,
                    ipv4 = v4,
                    ipv6 = v6,
                    mac = obj.optString("mac", "-"),
                    rxBytes = obj.optLong("rx_bytes", 0L),
                    txBytes = obj.optLong("tx_bytes", 0L),
                    role = if (name.contains("wan", true)) "role_upstream" else "role_iface"
                )
            }
        }
        return list
    }

    fun changePassword(session: RouterSession, username: String, newPassword: String): RouterActionResult {
        if (newPassword.isBlank()) return RouterActionResult(false, "新密码不能为空")
        val user = username.ifBlank { "root" }
        // 1) luci setPassword
        val luci = runCatching {
            ubusCall(
                session,
                "luci",
                "setPassword",
                JSONObject().put("username", user).put("password", newPassword)
            )
            RouterActionResult(true, "密码已更新")
        }.getOrNull()
        if (luci?.success == true) return luci
        // 2) file.exec passwd via chpasswd
        val execOk = runCatching {
            execCommand(session, "/bin/sh", listOf("-c", "echo ${shellQuote("$user:$newPassword")} | chpasswd"))
            true
        }.getOrDefault(false)
        if (execOk) return RouterActionResult(true, "密码已通过 chpasswd 更新")
        val msg = luci?.message ?: "修改密码失败"
        return RouterActionResult(
            false,
            if (msg.contains("code=6")) "路由器禁止执行命令（file.exec），无法改密。请在 LuCI 网页修改，或放行 ubus file.exec / luci.setPassword。"
            else msg
        )
    }

    private fun shellQuote(s: String): String =
        "'" + s.replace("'", "'\\''") + "'"

    private fun fetchStorageLuci(session: RouterSession): List<StorageVolume> {
        val out = mutableListOf<StorageVolume>()
        fun absorbObj(o: org.json.JSONObject) {
            val mount = o.optString("mount", o.optString("path", o.optString("target", ""))).ifBlank { return }
            val total = jsonLong(o, "size", "total", "total_bytes", "blocks")
            val free = jsonLong(o, "free", "avail", "available", "free_bytes")
            var used = jsonLong(o, "used", "used_bytes")
            if (used <= 0L && total > 0L) used = (total - free).coerceAtLeast(0L)
            var pct = jsonInt(o, "percent", "used_percent", "usage")
            if (pct <= 0 && total > 0L) pct = ((used * 100) / total).toInt()
            // Luci statvfs is usually bytes; some firmwares report KiB when values look tiny for whole disks.
            var totalB = total
            var usedB = used
            var freeB = free
            if (totalB in 1 until (64L * 1024) && (mount == "/" || mount.startsWith("/mnt") || mount.startsWith("/overlay"))) {
                // likely KiB
                totalB *= 1024
                usedB *= 1024
                freeB *= 1024
            }
            out += StorageVolume(
                mount = mount,
                device = o.optString("device", o.optString("dev", o.optString("source", "-"))).ifBlank { "-" },
                fstype = o.optString("fs", o.optString("fstype", o.optString("type", "-"))).ifBlank { "-" },
                totalBytes = totalB,
                usedBytes = usedB,
                freeBytes = freeB,
                usedPercent = pct.coerceIn(0, 100)
            )
        }
        runCatching {
            val result = ubusCall(session, "luci", "getMountPoints", JSONObject())
            when (val data = result.opt(1)) {
                is JSONArray -> {
                    for (i in 0 until data.length()) {
                        data.optJSONObject(i)?.let { absorbObj(it) }
                    }
                }
                is org.json.JSONObject -> {
                    val arr = data.optJSONArray("mounts")
                        ?: data.optJSONArray("mountpoints")
                        ?: data.optJSONArray("result")
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            arr.optJSONObject(i)?.let { absorbObj(it) }
                        }
                    } else {
                        // map of mount -> info
                        val keys = data.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            val o = data.optJSONObject(k) ?: continue
                            if (!o.has("mount") && !o.has("path")) {
                                o.put("mount", k)
                            }
                            absorbObj(o)
                        }
                    }
                }
            }
        }
        return out
    }

    private fun fetchStorageDf(session: RouterSession): List<StorageVolume> {
        val text = runCatching {
            // Prefer helper (surfaces stdout even when partial); try common df paths.
            val attempts = listOf(
                "/bin/df" to listOf("-kP"),
                "/usr/bin/df" to listOf("-kP"),
                "/bin/busybox" to listOf("df", "-kP")
            )
            for ((cmd, params) in attempts) {
                val out = runCatching { execCommand(session, cmd, params) }.getOrDefault("")
                if (out.isNotBlank() && !out.startsWith("code=") && out.lineSequence().count() > 1) {
                    return@runCatching out
                }
            }
            ""
        }.getOrDefault("")
        if (text.isBlank()) return emptyList()
        val list = mutableListOf<StorageVolume>()
        text.lineSequence().drop(1).forEach { line ->
            val p = line.trim().split(Regex("\\s+"))
            if (p.size < 6) return@forEach
            val totalK = p[1].toLongOrNull() ?: return@forEach
            val usedK = p[2].toLongOrNull() ?: 0L
            val freeK = p[3].toLongOrNull() ?: 0L
            val pct = p[4].removeSuffix("%").toIntOrNull() ?: 0
            val mount = p.subList(5, p.size).joinToString(" ")
            if (totalK <= 0) return@forEach
            list += StorageVolume(
                mount = mount,
                device = p[0],
                fstype = "-",
                totalBytes = totalK * 1024,
                usedBytes = usedK * 1024,
                freeBytes = freeK * 1024,
                usedPercent = pct.coerceIn(0, 100)
            )
        }
        return list
    }

    private fun fetchStorageBlock(session: RouterSession): List<StorageVolume> {
        val list = mutableListOf<StorageVolume>()
        runCatching {
            val result = ubusCall(session, "block", "info", JSONObject())
            val data = result.opt(1) ?: return@runCatching
            fun absorbDevice(name: String, o: org.json.JSONObject) {
                val size = jsonLong(o, "size", "capacity")
                val mounts = o.optJSONObject("mount")
                val mountPath = when {
                    mounts != null && mounts.length() > 0 -> {
                        val it = mounts.keys()
                        if (it.hasNext()) it.next() else ""
                    }
                    else -> o.optString("mount", o.optString("path", ""))
                }
                if (mountPath.isBlank() && size <= 0L) return
                val used = jsonLong(o, "used")
                val free = if (size > 0 && used >= 0) (size - used).coerceAtLeast(0) else jsonLong(o, "free", "avail")
                val pct = if (size > 0L) (((size - free).coerceAtLeast(0) * 100) / size).toInt() else 0
                list += StorageVolume(
                    mount = mountPath.ifBlank { name },
                    device = name.ifBlank { o.optString("device", "-") },
                    fstype = o.optString("type", o.optString("fstype", "-")).ifBlank { "-" },
                    totalBytes = size,
                    usedBytes = if (used > 0) used else (size - free).coerceAtLeast(0),
                    freeBytes = free,
                    usedPercent = pct.coerceIn(0, 100)
                )
            }
            when (data) {
                is org.json.JSONObject -> {
                    val keys = data.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        val o = data.optJSONObject(k) ?: continue
                        absorbDevice(k, o)
                    }
                }
                is JSONArray -> {
                    for (i in 0 until data.length()) {
                        val o = data.optJSONObject(i) ?: continue
                        absorbDevice(o.optString("device", o.optString("name", "")), o)
                    }
                }
            }
        }
        return list
    }

    private fun fetchStorageFromProc(session: RouterSession): List<StorageVolume> {
        val mountsText = runCatching { fileRead(session, "/proc/mounts", 64 * 1024) }.getOrDefault("")
        if (mountsText.isBlank()) return emptyList()
        val list = mutableListOf<StorageVolume>()
        mountsText.lineSequence().forEach { line ->
            val p = line.trim().split(Regex("\\s+"))
            if (p.size < 3) return@forEach
            val device = p[0]
            val mount = p[1]
            val fstype = p[2]
            if (mount.startsWith("/sys") || mount.startsWith("/proc") || mount == "/dev") return@forEach
            val total = guessBlockBytes(session, device)
            list += StorageVolume(
                mount = mount,
                device = device,
                fstype = fstype,
                totalBytes = total,
                usedBytes = 0L,
                freeBytes = 0L,
                usedPercent = 0
            )
        }
        return list
    }

    private fun guessBlockBytes(session: RouterSession, device: String): Long {
        val name = device.removePrefix("/dev/").substringBefore('/')
        if (name.isBlank() || name == "overlay" || name == "tmpfs" || name.startsWith("ubi")) return 0L
        // /sys/class/block/<name>/size is in 512-byte sectors
        val sectors = runCatching {
            fileRead(session, "/sys/class/block/$name/size", 64).trim().toLongOrNull()
        }.getOrNull() ?: return 0L
        return if (sectors > 0L) sectors * 512L else 0L
    }


    private fun ubusCall(
        session: RouterSession,
        obj: String,
        method: String,
        params: JSONObject
    ): JSONArray {
        return if (session.isUbus) {
            ubusCallDirect(session, obj, method, params)
        } else {
            ubusCallViaLuci(session, obj, method, params)
        }
    }

    private fun ubusCallDirect(
        session: RouterSession,
        obj: String,
        method: String,
        params: JSONObject
    ): JSONArray {
        val token = session.ubusToken
            ?: throw OpenWrtException("无效的 ubus 会话")
        val payload = JSONObject()
            .put("jsonrpc", "2.0")
            .put("id", System.currentTimeMillis() % 100000)
            .put("method", "call")
            .put(
                "params",
                JSONArray()
                    .put(token)
                    .put(obj)
                    .put(method)
                    .put(params)
            )
            .toString()
        val response = http.postJson("${session.baseUrl}/ubus", payload)
        return parseUbusResponse(response, obj, method)
    }

    /** Cookie session: use LuCI admin/ubus. Never treat sysauth as ubus token. */
    private fun ubusCallViaLuci(
        session: RouterSession,
        obj: String,
        method: String,
        params: JSONObject
    ): JSONArray {
        val cookie = session.luciCookie
            ?: throw OpenWrtException("无有效会话，请重新登录")
        val cookieHeader = "sysauth=$cookie; sysauth_http=$cookie; sysauth_https=$cookie"
        val payloadVariants = listOf(
            JSONObject()
                .put("jsonrpc", "2.0")
                .put("id", System.currentTimeMillis() % 100000)
                .put("method", "call")
                .put("params", JSONArray().put(obj).put(method).put(params)),
            JSONObject()
                .put("jsonrpc", "2.0")
                .put("id", System.currentTimeMillis() % 100000)
                .put("method", "call")
                .put("params", JSONArray().put(JSONObject.NULL).put(obj).put(method).put(params)),
            JSONObject()
                .put("jsonrpc", "2.0")
                .put("id", System.currentTimeMillis() % 100000)
                .put("method", "call")
                .put("params", JSONArray().put("").put(obj).put(method).put(params))
        )
        var lastError: Exception? = null
        for (payload in payloadVariants) {
            try {
                val response = http.postJson(
                    "${session.baseUrl}/cgi-bin/luci/admin/ubus",
                    payload.toString(),
                    mapOf("Cookie" to cookieHeader)
                )
                return parseUbusResponse(response, obj, method)
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw OpenWrtException(
            lastError?.message
                ?: "LuCI 会话无法调用 ubus（$obj.$method）。请重新登录以获取 ubus 令牌。"
        )
    }

    private fun parseUbusResponse(response: HttpTransport.HttpResponse, obj: String, method: String): JSONArray {
        val text = response.body
        if (!response.isSuccessful) {
            throw OpenWrtException("请求失败 HTTP ${response.code}")
        }
        if (text.isBlank()) {
            throw OpenWrtException("空响应 ($obj.$method)")
        }
        val root = try {
            JSONObject(text)
        } catch (_: Exception) {
            val arr = JSONArray(text)
            if (arr.length() > 0 && arr.optJSONObject(0) != null) {
                arr.getJSONObject(0)
            } else {
                throw OpenWrtException("无法解析响应")
            }
        }
        if (root.has("error")) {
            val err = root.optJSONObject("error")
            throw OpenWrtException(err?.optString("message") ?: "ubus 错误")
        }
        val result = root.optJSONArray("result")
            ?: throw OpenWrtException("空响应")
        val code = result.optInt(0, -1)
        if (code != 0) {
            throw OpenWrtException("ubus 调用失败 ($obj.$method code=$code)")
        }
        return result
    }

    private fun luciRpc(session: RouterSession, body: String) {
        val cookie = session.luciCookie ?: session.authToken.removePrefix("ubus:")
        val response = http.postJson(
            "${session.baseUrl}/cgi-bin/luci/admin/ubus",
            body,
            mapOf("Cookie" to "sysauth=$cookie; sysauth_http=$cookie")
        )
        if (!response.isSuccessful) {
            throw OpenWrtException("操作失败 HTTP ${response.code}")
        }
    }

    private fun normalizeBase(host: String, useHttps: Boolean): String {
        val trimmed = host.trim().removeSuffix("/")
        return when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            useHttps -> "https://$trimmed"
            else -> "http://$trimmed"
        }
    }

    companion object {
        const val DEFAULT_HOST = "192.168.10.1"
    }
}
