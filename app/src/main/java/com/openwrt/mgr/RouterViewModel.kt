package com.openwrt.mgr

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.openwrt.mgr.data.NetworkDevice
import com.openwrt.mgr.data.OpenWrtClient
import com.openwrt.mgr.data.SshClient
import com.openwrt.mgr.data.StorageVolume
import com.openwrt.mgr.data.NetInterfaceInfo
import com.openwrt.mgr.data.PluginInfo
import com.openwrt.mgr.data.ProcessInfo
import com.openwrt.mgr.data.LogLine
import com.openwrt.mgr.data.RouterSession
import com.openwrt.mgr.data.SystemInfo
import com.openwrt.mgr.data.WirelessInterface
import com.openwrt.mgr.data.MtdPartition
import com.openwrt.mgr.data.BinaryActionResult
import com.openwrt.mgr.ui.i18n.AppLanguage
import com.openwrt.mgr.ui.i18n.L10nCatalog
import com.openwrt.mgr.ui.theme.AppIconStyle
import com.openwrt.mgr.ui.theme.BackgroundStyle
import com.openwrt.mgr.ui.theme.ThemeMode
import com.openwrt.mgr.ui.theme.ThemeStyle
import com.openwrt.mgr.util.TerminalBuffer
import com.openwrt.mgr.util.IconSwitcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class AppTab { OVERVIEW, DEVICES, WIFI, PLUGINS, SSH, ACTIONS, THEME }

enum class ToolsSection { ACTIONS, BACKUP, PROCESSES, LOGS }

data class UiState(
    val host: String = OpenWrtClient.DEFAULT_HOST,
    val username: String = "root",
    val password: String = "",
    val rememberPassword: Boolean = false,
    val useHttps: Boolean = false,
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val selectedTab: AppTab = AppTab.OVERVIEW,
    val session: RouterSession? = null,
    val systemInfo: SystemInfo? = null,
    val storageVolumes: List<StorageVolume> = emptyList(),
    val networkIfaces: List<NetInterfaceInfo> = emptyList(),
    val newPassword: String = "",
    val confirmPassword: String = "",
    val devices: List<NetworkDevice> = emptyList(),
    val wireless: List<WirelessInterface> = emptyList(),
    val plugins: List<PluginInfo> = emptyList(),
    val pluginsOnlyInstalled: Boolean = true,
    val pluginsLoading: Boolean = false,
    val pluginsError: String? = null,
    // System tools (inside Actions tab)
    val toolsSection: ToolsSection = ToolsSection.ACTIONS,
    val mtdPartitions: List<MtdPartition> = emptyList(),
    val selectedMtdIndex: Int = 0,
    val keepSettingsOnFlash: Boolean = true,
    val pendingDownloadBytes: ByteArray? = null,
    val pendingDownloadName: String? = null,
    val pendingDownloadNonce: Long = 0L,
    val processes: List<ProcessInfo> = emptyList(),
    val processQuery: String = "",
    val processesLoading: Boolean = false,
    val processesError: String? = null,
    val systemLogs: List<LogLine> = emptyList(),
    val logQuery: String = "",
    val logLinesLimit: Int = 200,
    val logsLoading: Boolean = false,
    val logsError: String? = null,
    val appLanguage: AppLanguage = AppLanguage.SYSTEM,
    val themeStyle: ThemeStyle = ThemeStyle.SAKURA,
    val appIconStyle: AppIconStyle = AppIconStyle.SAKURA,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val backgroundStyle: BackgroundStyle = BackgroundStyle.SOFT_GRADIENT,
    val customBackgroundUri: String? = null,
    val backgroundDim: Float = 0.35f,
    /** 0f transparent .. 1f solid for chrome (top/bottom bars) */
    val chromeAlpha: Float = 0.94f,
    /** 0f transparent .. 1f solid for content cards */
    val cardAlpha: Float = 0.96f,
    // SSH terminal
    val sshPort: Int = 22,
    val sshConnected: Boolean = false,
    val sshConnecting: Boolean = false,
    val sshOutput: String = "",
    val sshCommand: String = "",
    val sshStatus: String? = null,
    // Custom primary (ARGB long, alpha forced FF when used)
    val customPrimaryArgb: Long = 0xFFB12D5B,
    val showAppInfo: Boolean = false
)

class RouterViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("openwrt_mgr", Context.MODE_PRIVATE)
    private val client = OpenWrtClient()
    private val sshClient by lazy { SshClient() }
    private val termBuffer = TerminalBuffer()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val bgFile = File(app.filesDir, "custom_background.jpg")

    var state by mutableStateOf(
        UiState(
            host = prefs.getString("host", OpenWrtClient.DEFAULT_HOST) ?: OpenWrtClient.DEFAULT_HOST,
            username = prefs.getString("username", "root") ?: "root",
            rememberPassword = prefs.getBoolean("rememberPassword", false),
            password = if (prefs.getBoolean("rememberPassword", false)) {
                prefs.getString("password", "") ?: ""
            } else {
                ""
            },
            useHttps = prefs.getBoolean("useHttps", false),
            appLanguage = AppLanguage.fromKey(prefs.getString("appLanguage", AppLanguage.SYSTEM.key)),
            themeStyle = ThemeStyle.fromKey(prefs.getString("themeStyle", ThemeStyle.SAKURA.key)),
            appIconStyle = AppIconStyle.fromKey(prefs.getString("appIconStyle", AppIconStyle.SAKURA.key)),
            themeMode = ThemeMode.fromKey(prefs.getString("themeMode", ThemeMode.SYSTEM.key)),
            backgroundStyle = BackgroundStyle.fromKey(
                prefs.getString("backgroundStyle", BackgroundStyle.SOFT_GRADIENT.key)
            ),
            customBackgroundUri = prefs.getString("customBackgroundUri", null)
                ?: if (bgFile.exists()) bgFile.toURI().toString() else null,
            backgroundDim = prefs.getFloat("backgroundDim", 0.35f),
            chromeAlpha = prefs.getFloat("chromeAlpha", 0.94f),
            cardAlpha = prefs.getFloat("cardAlpha", 0.96f),
            pluginsOnlyInstalled = prefs.getBoolean("pluginsOnlyInstalled", true),
            sshPort = prefs.getInt("sshPort", 22),
            customPrimaryArgb = prefs.getLong("customPrimaryArgb", 0xFFB12D5BL)
        )
    )
        private set

    fun updateHost(value: String) { state = state.copy(host = value) }
    fun updateUsername(value: String) { state = state.copy(username = value) }
    fun updatePassword(value: String) { state = state.copy(password = value) }
    fun updateRememberPassword(value: Boolean) {
        state = state.copy(rememberPassword = value)
        val ed = prefs.edit().putBoolean("rememberPassword", value)
        if (!value) {
            ed.remove("password")
        } else if (state.password.isNotEmpty()) {
            ed.putString("password", state.password)
        }
        ed.apply()
    }
    fun updateUseHttps(value: Boolean) { state = state.copy(useHttps = value) }
    fun selectTab(tab: AppTab) { state = state.copy(selectedTab = tab) }

    private fun l10n() = L10nCatalog.forLanguage(state.appLanguage)

    fun setAppLanguage(lang: AppLanguage) {
        prefs.edit().putString("appLanguage", lang.key).apply()
        state = state.copy(
            appLanguage = lang,
            statusMessage = l10n().t("switched_to", if (lang == AppLanguage.SYSTEM) l10n().langSystem else lang.nativeName)
        )
    }



    fun setThemeStyle(style: ThemeStyle) {
        prefs.edit().putString("themeStyle", style.key).apply()
        state = state.copy(themeStyle = style, statusMessage = l10n().t("switched_to", style.displayName(l10n())))
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString("themeMode", mode.key).apply()
        state = state.copy(themeMode = mode, statusMessage = l10n().t("appearance_colon", mode.displayName(l10n())))
    }

    fun setAppIconStyle(style: AppIconStyle) {
        viewModelScope.launch {
            if (style == AppIconStyle.CUSTOM && !IconSwitcher.customIconFile(getApplication()).exists()) {
                state = state.copy(errorMessage = l10n().pickCustomIconFirst)
                return@launch
            }
            withContext(Dispatchers.IO) {
                IconSwitcher.applyStyle(getApplication(), style)
            }
            prefs.edit().putString("appIconStyle", style.key).apply()
            state = state.copy(
                appIconStyle = style,
                statusMessage = l10n().t("app_icon_applied", style.displayName(l10n()))
            )
        }
    }

    fun setCustomAppIcon(uri: Uri?) {
        viewModelScope.launch {
            if (uri == null) {
                withContext(Dispatchers.IO) { IconSwitcher.clearCustomIcon(getApplication()) }
                // fallback to sakura if currently custom
                val next = if (state.appIconStyle == AppIconStyle.CUSTOM) AppIconStyle.SAKURA else state.appIconStyle
                withContext(Dispatchers.IO) { IconSwitcher.applyStyle(getApplication(), next) }
                prefs.edit().putString("appIconStyle", next.key).apply()
                state = state.copy(appIconStyle = next, statusMessage = l10n().customIconCleared)
                return@launch
            }
            val ok = withContext(Dispatchers.IO) {
                IconSwitcher.saveCustomIcon(getApplication(), uri)
            }
            if (!ok) {
                state = state.copy(errorMessage = l10n().customIconSaveFail)
                return@launch
            }
            withContext(Dispatchers.IO) {
                IconSwitcher.applyStyle(getApplication(), AppIconStyle.CUSTOM)
            }
            prefs.edit().putString("appIconStyle", AppIconStyle.CUSTOM.key).apply()
            state = state.copy(
                appIconStyle = AppIconStyle.CUSTOM,
                statusMessage = l10n().customIconSaved
            )
        }
    }


    fun setBackgroundStyle(style: BackgroundStyle) {
        prefs.edit().putString("backgroundStyle", style.key).apply()
        state = state.copy(backgroundStyle = style, statusMessage = l10n().t("background_colon", style.displayName(l10n())))
    }

    fun setBackgroundDim(dim: Float) {
        val v = dim.coerceIn(0.1f, 0.75f)
        prefs.edit().putFloat("backgroundDim", v).apply()
        state = state.copy(backgroundDim = v)
    }

    fun setChromeAlpha(alpha: Float) {
        val v = alpha.coerceIn(0.35f, 1f)
        prefs.edit().putFloat("chromeAlpha", v).apply()
        state = state.copy(chromeAlpha = v)
    }

    fun setCardAlpha(alpha: Float) {
        val v = alpha.coerceIn(0.55f, 1f)
        prefs.edit().putFloat("cardAlpha", v).apply()
        state = state.copy(cardAlpha = v)
    }

    fun setCustomBackground(uri: Uri?) {
        viewModelScope.launch {
            if (uri == null) {
                runCatching { bgFile.delete() }
                prefs.edit().remove("customBackgroundUri").apply()
                state = state.copy(
                    customBackgroundUri = null,
                    backgroundStyle = if (state.backgroundStyle == BackgroundStyle.CUSTOM_IMAGE) {
                        BackgroundStyle.SOFT_GRADIENT
                    } else state.backgroundStyle,
                    statusMessage = l10n().customBgCleared
                )
                return@launch
            }
            try {
                withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(bgFile).use { output -> input.copyTo(output) }
                    } ?: error(l10n().cannotReadImage)
                }
                val local = bgFile.toURI().toString()
                prefs.edit()
                    .putString("customBackgroundUri", local)
                    .putString("backgroundStyle", BackgroundStyle.CUSTOM_IMAGE.key)
                    .apply()
                state = state.copy(
                    customBackgroundUri = local,
                    backgroundStyle = BackgroundStyle.CUSTOM_IMAGE,
                    statusMessage = l10n().customBgApplied
                )
            } catch (e: Exception) {
                state = state.copy(errorMessage = e.message ?: l10n().setBgFailed)
            }
        }
    }

    fun setPluginsOnlyInstalled(only: Boolean) {
        prefs.edit().putBoolean("pluginsOnlyInstalled", only).apply()
        state = state.copy(pluginsOnlyInstalled = only)
    }

    fun customIconPreviewUri(): String? {
        val f = IconSwitcher.customIconFile(getApplication())
        return if (f.exists()) f.toURI().toString() else null
    }

    fun clearMessages() {
        state = state.copy(statusMessage = null, errorMessage = null)
    }

    fun login() {
        val host = state.host.trim().ifBlank { OpenWrtClient.DEFAULT_HOST }
        val username = state.username.trim().ifBlank { "root" }
        val password = state.password
        viewModelScope.launch {
            state = state.copy(isLoading = true, errorMessage = null, statusMessage = null)
            try {
                val session = withContext(Dispatchers.IO) {
                    client.login(host, username, password, state.useHttps)
                        .withCredentials(password, state.sshPort)
                }
                val remember = state.rememberPassword
                val editor = prefs.edit()
                    .putString("host", host)
                    .putString("username", username)
                    .putBoolean("useHttps", state.useHttps)
                    .putBoolean("rememberPassword", remember)
                if (remember) {
                    editor.putString("password", password)
                } else {
                    editor.remove("password")
                }
                editor.apply()
                state = state.copy(
                    isLoggedIn = true,
                    isLoading = false,
                    session = session,
                    host = host,
                    username = username,
                    rememberPassword = remember,
                    statusMessage = l10n().t("connected_to", host)
                )
                refreshAll()
            } catch (e: Exception) {
                state = state.copy(
                    isLoading = false,
                    isLoggedIn = false,
                    session = null,
                    errorMessage = e.message ?: l10n().loginFailed
                )
            }
        }
    }

    fun logout() {
        runCatching { sshClient.disconnect() }
        val keepPwd = state.rememberPassword
        state = UiState(
            host = state.host,
            username = state.username,
            password = if (keepPwd) state.password else "",
            rememberPassword = keepPwd,
            useHttps = state.useHttps,
            themeStyle = state.themeStyle,
            appIconStyle = state.appIconStyle,
            themeMode = state.themeMode,
            backgroundStyle = state.backgroundStyle,
            customBackgroundUri = state.customBackgroundUri,
            backgroundDim = state.backgroundDim,
            chromeAlpha = state.chromeAlpha,
            cardAlpha = state.cardAlpha,
            pluginsOnlyInstalled = state.pluginsOnlyInstalled,
            sshPort = state.sshPort,
            customPrimaryArgb = state.customPrimaryArgb
        )
    }

    fun refreshAll() {
        val session = state.session ?: return
        viewModelScope.launch {
            state = state.copy(isLoading = true, errorMessage = null)
            try {
                // Core data first; plugins load separately so overview stays snappy.
                val info = withContext(Dispatchers.IO) { client.fetchSystemInfo(session) }
                val devices = withContext(Dispatchers.IO) {
                    runCatching { client.fetchDevices(session) }.getOrDefault(emptyList())
                }
                val wireless = withContext(Dispatchers.IO) {
                    runCatching { client.fetchWireless(session) }.getOrDefault(emptyList())
                }
                val storage = withContext(Dispatchers.IO) {
                    runCatching { client.fetchStorage(session) }.getOrDefault(emptyList())
                }
                val ifaces = withContext(Dispatchers.IO) {
                    runCatching { client.fetchNetworkOverview(session) }.getOrDefault(emptyList())
                }
                state = state.copy(
                    isLoading = false,
                    systemInfo = info,
                    devices = devices,
                    wireless = wireless,
                    storageVolumes = storage,
                    networkIfaces = ifaces,
                    statusMessage = l10n().dataRefreshed
                )
                // Non-blocking plugin scan
                refreshPlugins(showGlobalLoading = false)
            } catch (e: Exception) {
                state = state.copy(
                    isLoading = false,
                    errorMessage = e.message ?: l10n().refreshFailed
                )
            }
        }
    }

    fun refreshPlugins(showGlobalLoading: Boolean = true) {
        val session = state.session ?: return
        viewModelScope.launch {
            state = state.copy(
                isLoading = if (showGlobalLoading) true else state.isLoading,
                pluginsLoading = true,
                pluginsError = null,
                errorMessage = null
            )
            try {
                val plugins = withContext(Dispatchers.IO) { client.listPlugins(session) }
                state = state.copy(
                    isLoading = if (showGlobalLoading) false else state.isLoading,
                    pluginsLoading = false,
                    plugins = plugins,
                    pluginsError = null,
                    statusMessage = if (showGlobalLoading) l10n().pluginsUpdated else state.statusMessage
                )
            } catch (e: Exception) {
                val msg = e.message ?: l10n().pluginsRefreshFailed
                state = state.copy(
                    isLoading = if (showGlobalLoading) false else state.isLoading,
                    pluginsLoading = false,
                    pluginsError = msg,
                    errorMessage = if (showGlobalLoading) msg else state.errorMessage
                )
            }
        }
    }

    fun togglePlugin(plugin: PluginInfo, enable: Boolean) {
        val session = state.session ?: return
        viewModelScope.launch {
            state = state.copy(isLoading = true, errorMessage = null, statusMessage = null)
            val result = withContext(Dispatchers.IO) {
                runCatching { client.setPluginEnabled(session, plugin, enable) }
                    .getOrElse { com.openwrt.mgr.data.RouterActionResult(false, it.message ?: l10n().operationFailed) }
            }
            if (result.success) {
                refreshPlugins()
                state = state.copy(statusMessage = result.message)
            } else {
                state = state.copy(isLoading = false, errorMessage = result.message)
            }
        }
    }

    fun restartPlugin(plugin: PluginInfo) {
        val session = state.session ?: return
        viewModelScope.launch {
            state = state.copy(isLoading = true, errorMessage = null, statusMessage = null)
            val result = withContext(Dispatchers.IO) {
                runCatching { client.restartPlugin(session, plugin) }
                    .getOrElse { com.openwrt.mgr.data.RouterActionResult(false, it.message ?: l10n().rebootFailed) }
            }
            state = if (result.success) {
                state.copy(isLoading = false, statusMessage = result.message)
            } else {
                state.copy(isLoading = false, errorMessage = result.message)
            }
        }
    }



    fun setCustomPrimary(argb: Long) {
        val v = argb or 0xFF000000L
        prefs.edit().putLong("customPrimaryArgb", v).apply()
        state = state.copy(
            customPrimaryArgb = v,
            themeStyle = ThemeStyle.CUSTOM,
            statusMessage = l10n().customColorApplied
        )
        prefs.edit().putString("themeStyle", ThemeStyle.CUSTOM.key).apply()
    }

    fun setShowAppInfo(show: Boolean) {
        state = state.copy(showAppInfo = show)
    }

    fun updateSshPort(port: String) {
        val p = port.filter { it.isDigit() }.toIntOrNull()?.coerceIn(1, 65535) ?: 22
        prefs.edit().putInt("sshPort", p).apply()
        val sess = state.session?.withCredentials(state.password, p)
        state = state.copy(sshPort = p, session = sess ?: state.session)
    }

    fun updateSshCommand(value: String) {
        state = state.copy(sshCommand = value)
    }

    fun clearSshOutput() {
        termBuffer.reset()
        state = state.copy(sshOutput = "", sshStatus = null)
    }

    fun connectSsh() {
        if (state.sshConnecting || state.sshConnected) return
        val host = state.host
        val username = state.username
        val password = state.password
        val port = state.sshPort
        if (password.isBlank()) {
            state = state.copy(errorMessage = l10n().sshNeedPassword)
            return
        }
        viewModelScope.launch {
            termBuffer.reset()
            state = state.copy(sshConnecting = true, sshStatus = l10n().sshConnecting, errorMessage = null, sshOutput = "")
            sshClient.setListeners(
                onOutput = { chunk ->
                    mainHandler.post {
                        val next = termBuffer.feed(chunk)
                        if (next == state.sshOutput) return@post
                        state = state.copy(sshOutput = next)
                    }
                },
                onStatus = { connected, msg ->
                    mainHandler.post {
                        state = state.copy(
                            sshConnected = connected,
                            sshConnecting = false,
                            sshStatus = msg,
                            statusMessage = if (connected) l10n().sshConnected else msg
                        )
                    }
                }
            )
            try {
                withContext(Dispatchers.IO) {
                    sshClient.connect(host, port, username, password)
                }
            } catch (e: Exception) {
                state = state.copy(
                    sshConnecting = false,
                    sshConnected = false,
                    sshStatus = e.message ?: l10n().sshConnectFailed,
                    errorMessage = e.message ?: l10n().sshConnectFailed
                )
            }
        }
    }

    fun disconnectSsh() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { sshClient.disconnect() }
            withContext(Dispatchers.Main) {
                termBuffer.reset()
                state = state.copy(sshConnected = false, sshConnecting = false, sshStatus = l10n().sshDisconnected)
            }
        }
    }

    fun sendSshCommand() {
        val cmd = state.sshCommand
        if (!state.sshConnected) {
            state = state.copy(errorMessage = l10n().sshNotConnected)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                if (cmd.isNotEmpty()) {
                    sshClient.sendCommand(cmd)
                } else {
                    sshClient.send("\n")
                }
            }.onFailure { e ->
                mainHandler.post {
                    state = state.copy(errorMessage = e.message ?: l10n().sendFailed)
                }
            }
        }
        state = state.copy(sshCommand = "")
    }

    @Volatile private var lastResizeCols = 0
    @Volatile private var lastResizeRows = 0
    private var resizeJob: kotlinx.coroutines.Job? = null

    fun resizeSsh(cols: Int, rows: Int) {
        if (!state.sshConnected) return
        if (cols == lastResizeCols && rows == lastResizeRows) return
        resizeJob?.cancel()
        resizeJob = viewModelScope.launch {
            delay(250)
            if (!state.sshConnected) return@launch
            if (cols == lastResizeCols && rows == lastResizeRows) return@launch
            lastResizeCols = cols
            lastResizeRows = rows
            withContext(Dispatchers.IO) {
                runCatching { sshClient.resize(cols, rows) }
            }
        }
    }

    fun sendSshRaw(text: String) {
        if (!state.sshConnected) return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { sshClient.send(text) }
        }
    }


    fun setToolsSection(section: ToolsSection) {
        state = state.copy(toolsSection = section)
        when (section) {
            ToolsSection.PROCESSES -> if (state.processes.isEmpty() && !state.processesLoading) refreshProcesses()
            ToolsSection.LOGS -> if (state.systemLogs.isEmpty() && !state.logsLoading) refreshLogs()
            ToolsSection.BACKUP -> if (state.mtdPartitions.isEmpty()) refreshMtdPartitions()
            ToolsSection.ACTIONS -> Unit
        }
    }

    fun updateProcessQuery(q: String) {
        state = state.copy(processQuery = q)
    }

    fun updateLogQuery(q: String) {
        state = state.copy(logQuery = q)
    }

    fun setLogLinesLimit(limit: Int) {
        state = state.copy(logLinesLimit = limit.coerceIn(50, 1000))
    }

    fun refreshProcesses() {
        val session = state.session ?: return
        viewModelScope.launch {
            state = state.copy(processesLoading = true, processesError = null)
            try {
                val list = withContext(Dispatchers.IO) { client.listProcesses(session) }
                state = state.copy(
                    processesLoading = false,
                    processes = list,
                    processesError = null,
                    statusMessage = null
                )
            } catch (e: Exception) {
                state = state.copy(
                    processesLoading = false,
                    processesError = e.message ?: l10n().processFetchFailed
                )
            }
        }
    }

    fun killProcess(pid: String) {
        val session = state.session ?: return
        viewModelScope.launch {
            state = state.copy(isLoading = true, errorMessage = null, statusMessage = null)
            val result = withContext(Dispatchers.IO) {
                runCatching { client.killProcess(session, pid, force = false) }
                    .getOrElse { com.openwrt.mgr.data.RouterActionResult(false, it.message ?: l10n().t("kill_failed")) }
            }
            if (result.success) {
                state = state.copy(isLoading = false, statusMessage = result.message)
                // refresh list after kill
                runCatching {
                    val list = withContext(Dispatchers.IO) { client.listProcesses(session) }
                    state = state.copy(processes = list, processesError = null)
                }
            } else {
                state = state.copy(isLoading = false, errorMessage = result.message)
            }
        }
    }

    fun refreshLogs() {
        val session = state.session ?: return
        val limit = state.logLinesLimit
        viewModelScope.launch {
            state = state.copy(logsLoading = true, logsError = null)
            try {
                val list = withContext(Dispatchers.IO) { client.fetchSystemLogs(session, limit) }
                state = state.copy(
                    logsLoading = false,
                    systemLogs = list,
                    logsError = null,
                    statusMessage = l10n().t("logs_refreshed", list.size)
                )
            } catch (e: Exception) {
                state = state.copy(
                    logsLoading = false,
                    logsError = e.message ?: l10n().logsFetchFailed
                )
            }
        }
    }

    fun rebootRouter() = runAction { client.reboot(it) }
    fun restartNetwork() = runAction { client.restartNetwork(it) }
    fun restartWifi() = runAction { client.restartWifi(it) }

    override fun onCleared() {
        super.onCleared()
        runCatching { sshClient.disconnect() }
    }

    
    fun updateNewPassword(value: String) { state = state.copy(newPassword = value) }
    fun updateConfirmPassword(value: String) { state = state.copy(confirmPassword = value) }

    fun changeUserPassword() {
        val session = state.session ?: return
        val np = state.newPassword
        val cp = state.confirmPassword
        if (np.isBlank()) {
            state = state.copy(errorMessage = l10n().enterNewPassword)
            return
        }
        if (np != cp) {
            state = state.copy(errorMessage = l10n().passwordMismatch)
            return
        }
        if (np.length < 4) {
            state = state.copy(errorMessage = l10n().passwordTooShort)
            return
        }
        viewModelScope.launch {
            state = state.copy(isLoading = true, errorMessage = null, statusMessage = null)
            val result = withContext(Dispatchers.IO) {
                runCatching { client.changePassword(session, state.username, np) }
                    .getOrElse { com.openwrt.mgr.data.RouterActionResult(false, it.message ?: l10n().changePasswordFailed) }
            }
            state = state.copy(
                isLoading = false,
                statusMessage = if (result.success) result.message else null,
                errorMessage = if (!result.success) result.message else null,
                password = if (result.success) {
                    if (state.rememberPassword) prefs.edit().putString("password", np).apply()
                    np
                } else state.password,
                newPassword = if (result.success) "" else state.newPassword,
                confirmPassword = if (result.success) "" else state.confirmPassword
            )
        }
    }


    fun refreshMtdPartitions() {
        val session = state.session ?: return
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) {
                runCatching { client.listMtdPartitions(session) }.getOrDefault(emptyList())
            }
            state = state.copy(
                mtdPartitions = list,
                selectedMtdIndex = state.selectedMtdIndex.coerceIn(0, (list.size - 1).coerceAtLeast(0))
            )
        }
    }

    fun selectMtdIndex(index: Int) {
        state = state.copy(selectedMtdIndex = index.coerceAtLeast(0))
    }

    fun setKeepSettingsOnFlash(keep: Boolean) {
        state = state.copy(keepSettingsOnFlash = keep)
    }

    fun generateBackup() {
        val session = state.session ?: return
        viewModelScope.launch {
            state = state.copy(isLoading = true, errorMessage = null, statusMessage = null)
            val result = withContext(Dispatchers.IO) {
                runCatching { client.createConfigBackup(session) }
                    .getOrElse { BinaryActionResult(false, it.message ?: l10n().operationFailed) }
            }
            state = if (result.success && result.bytes != null) {
                state.copy(
                    isLoading = false,
                    statusMessage = result.message,
                    pendingDownloadBytes = result.bytes,
                    pendingDownloadName = result.fileName.ifBlank { "openwrt-backup.tar.gz" },
                    pendingDownloadNonce = System.currentTimeMillis()
                )
            } else {
                state.copy(isLoading = false, errorMessage = result.message)
            }
        }
    }

    fun factoryReset() = runAction { client.factoryReset(it) }

    fun restoreBackup(bytes: ByteArray) {
        val session = state.session ?: return
        viewModelScope.launch {
            state = state.copy(isLoading = true, errorMessage = null, statusMessage = null)
            val result = withContext(Dispatchers.IO) {
                runCatching { client.restoreConfigBackup(session, bytes) }
                    .getOrElse { com.openwrt.mgr.data.RouterActionResult(false, it.message ?: l10n().operationFailed) }
            }
            state = if (result.success) {
                state.copy(isLoading = false, statusMessage = result.message)
            } else {
                state.copy(isLoading = false, errorMessage = result.message)
            }
        }
    }

    fun downloadSelectedMtd() {
        val session = state.session ?: return
        val part = state.mtdPartitions.getOrNull(state.selectedMtdIndex) ?: run {
            state = state.copy(errorMessage = l10n().t("select_mtd_first"))
            return
        }
        viewModelScope.launch {
            state = state.copy(isLoading = true, errorMessage = null, statusMessage = null)
            val result = withContext(Dispatchers.IO) {
                runCatching { client.downloadMtdPartition(session, part) }
                    .getOrElse { BinaryActionResult(false, it.message ?: l10n().operationFailed) }
            }
            state = if (result.success && result.bytes != null) {
                state.copy(
                    isLoading = false,
                    statusMessage = result.message,
                    pendingDownloadBytes = result.bytes,
                    pendingDownloadName = result.fileName.ifBlank { "${part.dev}.bin" },
                    pendingDownloadNonce = System.currentTimeMillis()
                )
            } else {
                state.copy(isLoading = false, errorMessage = result.message)
            }
        }
    }

    fun flashFirmware(bytes: ByteArray) {
        val session = state.session ?: return
        val keep = state.keepSettingsOnFlash
        viewModelScope.launch {
            state = state.copy(isLoading = true, errorMessage = null, statusMessage = null)
            val result = withContext(Dispatchers.IO) {
                runCatching { client.flashFirmware(session, bytes, keep) }
                    .getOrElse { com.openwrt.mgr.data.RouterActionResult(false, it.message ?: l10n().operationFailed) }
            }
            state = if (result.success) {
                state.copy(isLoading = false, statusMessage = result.message)
            } else {
                state.copy(isLoading = false, errorMessage = result.message)
            }
        }
    }

    fun clearPendingDownload() {
        state = state.copy(pendingDownloadBytes = null, pendingDownloadName = null)
    }


    private fun runAction(block: (RouterSession) -> com.openwrt.mgr.data.RouterActionResult) {
        val session = state.session ?: return
        viewModelScope.launch {
            state = state.copy(isLoading = true, errorMessage = null, statusMessage = null)
            val result = withContext(Dispatchers.IO) {
                runCatching { block(session) }
                    .getOrElse {
                        com.openwrt.mgr.data.RouterActionResult(false, it.message ?: l10n().operationFailed)
                    }
            }
            state = if (result.success) {
                state.copy(isLoading = false, statusMessage = result.message)
            } else {
                state.copy(isLoading = false, errorMessage = result.message)
            }
        }
    }
}

class RouterViewModelFactory(private val app: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RouterViewModel::class.java)) {
            return RouterViewModel(app) as T
        }
        throw IllegalArgumentException("Unknown ViewModel")
    }
}

fun formatUptime(seconds: Long): String {
    if (seconds <= 0) return "-"
    val d = seconds / 86400
    val h = (seconds % 86400) / 3600
    val m = (seconds % 3600) / 60
    val l = L10nCatalog.forLanguage(AppLanguage.SYSTEM)
    return buildString {
        if (d > 0) append(l.t("day_unit", d))
        append(l.t("hour_unit", h) + l.t("minute_unit", m))
    }
}

fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "-"
    val units = arrayOf("B", "KB", "MB", "GB")
    var value = bytes.toDouble()
    var idx = 0
    while (value >= 1024 && idx < units.lastIndex) {
        value /= 1024
        idx++
    }
    return String.format("%.1f %s", value, units[idx])
}
