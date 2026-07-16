package com.openwrt.mgr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import com.openwrt.mgr.ui.screens.AppRoot
import com.openwrt.mgr.ui.theme.OpenWrtTheme

class MainActivity : ComponentActivity() {
    private val viewModel: RouterViewModel by viewModels {
        RouterViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state = viewModel.state
            OpenWrtTheme(
                themeStyle = state.themeStyle,
                themeMode = state.themeMode,
                customPrimaryArgb = state.customPrimaryArgb
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent
                ) {
                    AppRoot(
                        state = state,
                        onHostChange = viewModel::updateHost,
                        onUsernameChange = viewModel::updateUsername,
                        onPasswordChange = viewModel::updatePassword,
                        onRememberPasswordChange = viewModel::updateRememberPassword,
                        onHttpsChange = viewModel::updateUseHttps,
                        onLogin = viewModel::login,
                        onLogout = viewModel::logout,
                        onRefresh = viewModel::refreshAll,
                        onTabSelected = viewModel::selectTab,
                        onReboot = viewModel::rebootRouter,
                        onRestartNetwork = viewModel::restartNetwork,
                        onRestartWifi = viewModel::restartWifi,
                        onToolsSectionChange = viewModel::setToolsSection,
                        onRefreshProcesses = viewModel::refreshProcesses,
                        onProcessQueryChange = viewModel::updateProcessQuery,
                        onKillProcess = viewModel::killProcess,
                        onThemeStyleChange = viewModel::setThemeStyle,
                        onThemeModeChange = viewModel::setThemeMode,
                        onAppIconStyleChange = viewModel::setAppIconStyle,
                        onBackgroundStyleChange = viewModel::setBackgroundStyle,
                        onBackgroundDimChange = viewModel::setBackgroundDim,
                        onChromeAlphaChange = viewModel::setChromeAlpha,
                        onCardAlphaChange = viewModel::setCardAlpha,
                        onCustomBackgroundPicked = viewModel::setCustomBackground,
                        onPluginsOnlyInstalledChange = viewModel::setPluginsOnlyInstalled,
                        onRefreshPlugins = viewModel::refreshPlugins,
                        onTogglePlugin = viewModel::togglePlugin,
                        onRestartPlugin = viewModel::restartPlugin,
                        onSshPortChange = viewModel::updateSshPort,
                        onSshCommandChange = viewModel::updateSshCommand,
                        onSshConnect = viewModel::connectSsh,
                        onSshDisconnect = viewModel::disconnectSsh,
                        onSshSend = viewModel::sendSshCommand,
                        onSshClear = viewModel::clearSshOutput,
                        onSshResize = viewModel::resizeSsh,
                        onSshRaw = viewModel::sendSshRaw,
                        onCustomPrimaryChange = viewModel::setCustomPrimary,
                        onShowAppInfo = viewModel::setShowAppInfo,
                        onClearMessages = viewModel::clearMessages,
                        onNewPasswordChange = viewModel::updateNewPassword,
                        onConfirmPasswordChange = viewModel::updateConfirmPassword,
                        onChangePassword = viewModel::changeUserPassword,
                        onAppLanguageChange = viewModel::setAppLanguage,
                        onGenerateBackup = viewModel::generateBackup,
                        onFactoryReset = viewModel::factoryReset,
                        onRestoreBackup = viewModel::restoreBackup,
                        onRefreshMtd = viewModel::refreshMtdPartitions,
                        onSelectMtd = viewModel::selectMtdIndex,
                        onDownloadMtd = viewModel::downloadSelectedMtd,
                        onKeepSettingsChange = viewModel::setKeepSettingsOnFlash,
                        onFlashFirmware = viewModel::flashFirmware,
                        onPendingDownloadSaved = viewModel::clearPendingDownload,
                    )
                }
            }
        }
    }
}
