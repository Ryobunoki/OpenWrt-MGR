package com.openwrt.mgr.ui.screens

import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.foundation.text.BasicTextField
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.openwrt.mgr.AppTab
import com.openwrt.mgr.ToolsSection
import com.openwrt.mgr.UiState
import com.openwrt.mgr.data.PluginInfo
import com.openwrt.mgr.data.ProcessInfo
import com.openwrt.mgr.data.LogLine
import com.openwrt.mgr.formatBytes
import com.openwrt.mgr.formatUptime
import com.openwrt.mgr.ui.theme.AppIconStyle
import com.openwrt.mgr.ui.theme.TerminalFontFamily
import com.openwrt.mgr.ui.theme.BackgroundStyle
import com.openwrt.mgr.ui.theme.ThemeMode
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import com.openwrt.mgr.BuildConfig
import com.openwrt.mgr.ui.theme.CustomPrimaryPresets
import java.io.File
import com.openwrt.mgr.ui.theme.ThemeStyle
import com.openwrt.mgr.util.AnsiStrip
import com.openwrt.mgr.ui.i18n.AppLanguage
import com.openwrt.mgr.ui.i18n.L10nCatalog
import com.openwrt.mgr.ui.i18n.LocalL10n
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow

private val LocalCardAlpha = compositionLocalOf { 0.88f }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(
    state: UiState,
    onHostChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onRememberPasswordChange: (Boolean) -> Unit = {},
    onHttpsChange: (Boolean) -> Unit,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
    onRefresh: () -> Unit,
    onTabSelected: (AppTab) -> Unit,
    onReboot: () -> Unit,
    onRestartNetwork: () -> Unit,
    onRestartWifi: () -> Unit,
    onToolsSectionChange: (ToolsSection) -> Unit,
    onRefreshProcesses: () -> Unit,
    onProcessQueryChange: (String) -> Unit,
    onThemeStyleChange: (ThemeStyle) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAppIconStyleChange: (AppIconStyle) -> Unit,
    onBackgroundStyleChange: (BackgroundStyle) -> Unit,
    onBackgroundDimChange: (Float) -> Unit,
    onChromeAlphaChange: (Float) -> Unit,
    onCardAlphaChange: (Float) -> Unit,
    onCustomBackgroundPicked: (Uri?) -> Unit,
    onPluginsOnlyInstalledChange: (Boolean) -> Unit,
    onRefreshPlugins: () -> Unit,
    onTogglePlugin: (PluginInfo, Boolean) -> Unit,
    onRestartPlugin: (PluginInfo) -> Unit,
    onSshPortChange: (String) -> Unit,
    onSshCommandChange: (String) -> Unit,
    onSshConnect: () -> Unit,
    onSshDisconnect: () -> Unit,
    onSshSend: () -> Unit,
    onSshClear: () -> Unit,
    onSshResize: (Int, Int) -> Unit,
    onSshRaw: (String) -> Unit,
    onCustomPrimaryChange: (Long) -> Unit,
    onShowAppInfo: (Boolean) -> Unit,
    onClearMessages: () -> Unit,
    onNewPasswordChange: (String) -> Unit = {},
    onConfirmPasswordChange: (String) -> Unit = {},
    onChangePassword: () -> Unit = {},
    onAppLanguageChange: (AppLanguage) -> Unit = {}
) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.statusMessage, state.errorMessage) {
        val msg = state.errorMessage ?: state.statusMessage
        if (!msg.isNullOrBlank()) {
            snackbar.showSnackbar(msg)
            onClearMessages()
        }
    }
    val l10n = L10nCatalog.forLanguage(state.appLanguage)
    CompositionLocalProvider(LocalL10n provides l10n) {
    AppBackground(state.backgroundStyle, state.customBackgroundUri, state.backgroundDim) {
        CompositionLocalProvider(LocalCardAlpha provides state.cardAlpha) {
        if (state.showAppInfo) {
            AppInfoDialog(state = state, onDismiss = { onShowAppInfo(false) })
        }
        if (!state.isLoggedIn) {
            LoginScreen(
                state = state,
                onHostChange = onHostChange,
                onUsernameChange = onUsernameChange,
                onPasswordChange = onPasswordChange,
                onRememberPasswordChange = onRememberPasswordChange,
                onHttpsChange = onHttpsChange,
                onLogin = onLogin,
                onThemeStyleChange = onThemeStyleChange,
                onThemeModeChange = onThemeModeChange,
                onCustomPrimaryChange = onCustomPrimaryChange,
                onShowAppInfo = onShowAppInfo,
                onBackgroundStyleChange = onBackgroundStyleChange
            )
        } else {
        val scheme = MaterialTheme.colorScheme
        val chromeColor = scheme.surface.copy(alpha = state.chromeAlpha.coerceIn(0.35f, 1f))
        val contentFg = scheme.onSurface
        val mutedFg = scheme.onSurfaceVariant
        val imeBottomPx = WindowInsets.ime.getBottom(LocalDensity.current)
        val hideBottomForSsh = state.selectedTab == AppTab.SSH && imeBottomPx > 0
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = contentFg,
            topBar = {
                Surface(
                    color = chromeColor,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .height(54.dp)
                            .padding(horizontal = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            Modifier
                                .weight(1f)
                                .padding(start = 12.dp, end = 4.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                l10n.appName,
                                fontWeight = FontWeight.SemiBold,
                                color = contentFg,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                state.host,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, lineHeight = 13.sp),
                                color = mutedFg,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { onShowAppInfo(true) }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.Info, contentDescription = l10n.appInfo, tint = contentFg, modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = onRefresh, enabled = !state.isLoading, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.Refresh, contentDescription = l10n.refresh, tint = contentFg, modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = onLogout, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.Close, contentDescription = l10n.logout, tint = contentFg, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            },
            bottomBar = {
                if (!hideBottomForSsh) {
Surface(
                        color = chromeColor,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val tabs = listOf(
                            Triple(AppTab.OVERVIEW, Icons.Default.Home, l10n.tabOverview),
                            Triple(AppTab.DEVICES, Icons.Default.Devices, l10n.tabDevices),
                            Triple(AppTab.PLUGINS, Icons.Default.Extension, l10n.tabPlugins),
                            Triple(AppTab.SSH, Icons.Default.Terminal, l10n.tabSsh),
                            Triple(AppTab.ACTIONS, Icons.Default.Build, l10n.tabActions),
                            Triple(AppTab.THEME, Icons.Default.Settings, l10n.tabSettings)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .height(54.dp)
                                .padding(horizontal = 0.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            tabs.forEach { (tab, icon, label) ->
                                val selected = state.selectedTab == tab
                                val tint = if (selected) scheme.primary else mutedFg
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable { onTabSelected(tab) }
                                        .padding(horizontal = 0.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (selected) scheme.primary.copy(alpha = 0.14f)
                                                else Color.Transparent
                                            )
                                            .padding(horizontal = 11.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = label,
                                            tint = tint,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    // Fixed slot keeps icons aligned; only selected text is opaque.
                                    Text(
                                        text = if (selected) label else " ",
                                        color = if (selected) tint else Color.Transparent,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            lineHeight = 12.sp
                                        ),
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Clip,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 1.dp, vertical = 0.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbar) }
        ) { padding ->
            // SSH uses its own IME padding; avoid stacking bottomBar + keyboard lift.
            val contentMod = if (state.selectedTab == AppTab.SSH && hideBottomForSsh) {
                // Keyboard open: bottom bar hidden — only top chrome inset; IME handled in SshTab
                Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding())
            } else {
                Modifier
                    .fillMaxSize()
                    .padding(padding)
            }
            Column(
                contentMod.background(scheme.background.copy(alpha = 0.0f))
            ) {
                if (state.isLoading) {
                    LinearProgressIndicator(
                        Modifier.fillMaxWidth(),
                        color = scheme.primary,
                        trackColor = scheme.surfaceVariant
                    )
                }
                when (state.selectedTab) {
                    AppTab.OVERVIEW -> OverviewTab(state)
                    AppTab.DEVICES -> DevicesTab(state)
                    AppTab.WIFI -> WifiTab(state)
                    AppTab.PLUGINS -> PluginsTab(state, onPluginsOnlyInstalledChange, onRefreshPlugins, onTogglePlugin, onRestartPlugin)
                    AppTab.SSH -> SshTab(
                        state = state,
                        onPortChange = onSshPortChange,
                        onCommandChange = onSshCommandChange,
                        onConnect = onSshConnect,
                        onDisconnect = onSshDisconnect,
                        onSend = onSshSend,
                        onClear = onSshClear,
                        onResize = onSshResize,
                        onRawSend = onSshRaw
                    )
                    AppTab.ACTIONS -> ActionsTab(
                        state = state,
                        loading = state.isLoading,
                        onSectionChange = onToolsSectionChange,
                        onReboot = onReboot,
                        onRestartNetwork = onRestartNetwork,
                        onRestartWifi = onRestartWifi,
                        onRefresh = onRefresh,
                        onRefreshProcesses = onRefreshProcesses,
                        onProcessQueryChange = onProcessQueryChange,
                        onNewPasswordChange = onNewPasswordChange,
                        onConfirmPasswordChange = onConfirmPasswordChange,
                        onChangePassword = onChangePassword
                    )
                    AppTab.THEME -> ThemeTab(
                        state = state,
                        onStyleChange = onThemeStyleChange,
                        onModeChange = onThemeModeChange,
                        onAppIconStyleChange = onAppIconStyleChange,
                        onCustomPrimaryChange = onCustomPrimaryChange,
                        onBackgroundStyleChange = onBackgroundStyleChange,
                        onBackgroundDimChange = onBackgroundDimChange,
                        onChromeAlphaChange = onChromeAlphaChange,
                        onCardAlphaChange = onCardAlphaChange,
                        onCustomBackgroundPicked = onCustomBackgroundPicked,
                        onSshPortChange = onSshPortChange,
                        onAppLanguageChange = onAppLanguageChange
                    )
                }
            }
        }
        }
        } // CompositionLocalProvider
    } // AppBackground
}



}

@Composable
private fun UriBitmapImage(uri: String, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop) {
    val context = LocalContext.current
    val bitmap = remember(uri) {
        runCatching {
            val u = Uri.parse(uri)
            context.contentResolver.openInputStream(u)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            } ?: if (uri.startsWith("file:")) {
                BitmapFactory.decodeFile(Uri.parse(uri).path)
            } else {
                BitmapFactory.decodeFile(uri)
            }
        }.getOrNull()?.asImageBitmap()
    }
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = null, modifier = modifier, contentScale = contentScale)
    }
}

@Composable
private fun FileBitmapImage(file: java.io.File, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop) {
    val bitmap = remember(file.absolutePath, file.lastModified()) {
        runCatching { BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() }.getOrNull()
    }
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = null, modifier = modifier, contentScale = contentScale)
    }
}

@Composable
private fun AppBackground(style: BackgroundStyle, customUri: String?, dim: Float, content: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(Modifier.fillMaxSize().background(scheme.background)) {
        when (style) {
            BackgroundStyle.NONE -> Unit
            BackgroundStyle.SOFT_GRADIENT -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    scheme.primaryContainer.copy(alpha = 0.55f),
                                    scheme.background,
                                    scheme.secondaryContainer.copy(alpha = 0.28f)
                                )
                            )
                        )
                )
            }
            BackgroundStyle.BLOOM -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    scheme.primary.copy(alpha = 0.28f),
                                    scheme.primaryContainer.copy(alpha = 0.18f),
                                    scheme.background
                                )
                            )
                        )
                )
            }
            BackgroundStyle.MESH -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    scheme.secondaryContainer.copy(alpha = 0.40f),
                                    scheme.tertiaryContainer.copy(alpha = 0.22f),
                                    scheme.primaryContainer.copy(alpha = 0.30f),
                                    scheme.background
                                )
                            )
                        )
                )
            }
            BackgroundStyle.CUSTOM_IMAGE -> {
                if (!customUri.isNullOrBlank()) {
                    UriBitmapImage(
                        uri = customUri,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = dim.coerceIn(0.05f, 0.85f))))
                }
            }
        }
        content()
    }
}

@Composable
private fun LoginScreen(
    state: UiState,
    onHostChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onRememberPasswordChange: (Boolean) -> Unit = {},
    onHttpsChange: (Boolean) -> Unit,
    onLogin: () -> Unit,
    onThemeStyleChange: (ThemeStyle) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onCustomPrimaryChange: (Long) -> Unit,
    onShowAppInfo: (Boolean) -> Unit,
    onBackgroundStyleChange: (BackgroundStyle) -> Unit
) {
    val l10n = LocalL10n.current

    var showAppearance by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().statusBarsPadding()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 48.dp, bottom = 32.dp)
                .imePadding(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(88.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Router, null, Modifier.size(44.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(l10n.appName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(28.dp))
            GlassCard {
                OutlinedTextField(state.host, onHostChange, label = { Text(l10n.routerAddress) }, singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri))
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(state.username, onUsernameChange, label = { Text(l10n.username) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                var showPassword by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value = state.password,
                    onValueChange = onPasswordChange,
                    label = { Text(l10n.password) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showPassword) l10n.hidePassword else l10n.showPassword,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onLogin() })
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(checked = state.rememberPassword, onCheckedChange = onRememberPasswordChange)
                    Text(
                        l10n.rememberPassword,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = state.useHttps, onCheckedChange = onHttpsChange)
                    Text(l10n.useHttps, color = MaterialTheme.colorScheme.onSurface)
                }
                Button(onClick = onLogin, enabled = !state.isLoading, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    if (state.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.size(8.dp))
                    }
                    Text(l10n.connectRouter)
                }
                if (!state.errorMessage.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(state.errorMessage!!, color = MaterialTheme.colorScheme.error)
                }
            }
        }
        Row(
            Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 4.dp, end = 4.dp)
        ) {
            IconButton(onClick = { showAppearance = true }) {
                Icon(Icons.Default.Palette, contentDescription = l10n.appearance, tint = MaterialTheme.colorScheme.onSurface)
            }
            IconButton(onClick = { onShowAppInfo(true) }) {
                Icon(Icons.Default.Info, contentDescription = l10n.appInfo, tint = MaterialTheme.colorScheme.onSurface)
            }
        }
        if (showAppearance) {
            AlertDialog(
                onDismissRequest = { showAppearance = false },
                title = { Text(l10n.appearanceQuick) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text(l10n.colorStyle, style = MaterialTheme.typography.labelLarge)
                        ChipRow(ThemeStyle.entries.map { it.displayName(l10n) }, ThemeStyle.entries.indexOf(state.themeStyle).coerceAtLeast(0)) {
                            onThemeStyleChange(ThemeStyle.entries[it])
                        }
                        Text(l10n.lightDark, style = MaterialTheme.typography.labelLarge)
                        ChipRow(ThemeMode.entries.map { it.displayName(l10n) }, ThemeMode.entries.indexOf(state.themeMode).coerceAtLeast(0)) {
                            onThemeModeChange(ThemeMode.entries[it])
                        }
                        Text(l10n.background, style = MaterialTheme.typography.labelLarge)
                        val bgs = BackgroundStyle.entries.filter { it != BackgroundStyle.CUSTOM_IMAGE }
                        ChipRow(bgs.map { it.displayName(l10n) }, bgs.indexOf(state.backgroundStyle).coerceAtLeast(0)) {
                            onBackgroundStyleChange(bgs[it])
                        }
                        if (state.themeStyle == ThemeStyle.CUSTOM) {
                            Text(l10n.customPrimary, style = MaterialTheme.typography.labelLarge)
                            PrimarySwatchRow(state.customPrimaryArgb, onCustomPrimaryChange)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAppearance = false }) { Text(l10n.done) }
                }
            )
        }
    }
}

@Composable
private fun ChipRow
(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.chunked(2).forEachIndexed { row, items ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEachIndexed { col, label ->
                    val idx = row * 2 + col
                    FilterChip(selected = selected == idx, onClick = { onSelect(idx) }, label = { Text(label) }, leadingIcon = if (selected == idx) {{ Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }} else null)
                }
            }
        }
    }
}

@Composable
private fun ThemeTab(
    state: UiState,
    onStyleChange: (ThemeStyle) -> Unit,
    onModeChange: (ThemeMode) -> Unit,
    onAppIconStyleChange: (AppIconStyle) -> Unit,
    onCustomPrimaryChange: (Long) -> Unit,
    onBackgroundStyleChange: (BackgroundStyle) -> Unit,
    onBackgroundDimChange: (Float) -> Unit,
    onChromeAlphaChange: (Float) -> Unit,
    onCardAlphaChange: (Float) -> Unit,
    onCustomBackgroundPicked: (Uri?) -> Unit,
    onSshPortChange: (String) -> Unit,
    onAppLanguageChange: (AppLanguage) -> Unit = {}
) {
    val l10n = LocalL10n.current

    var themeDetail by remember { mutableStateOf(false) }
    var languagePicker by remember { mutableStateOf(false) }
    if (themeDetail) {
        Dialog(
            onDismissRequest = { themeDetail = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                ThemeDetailScreen(
                    state = state,
                    onBack = { themeDetail = false },
                    onStyleChange = onStyleChange,
                    onModeChange = onModeChange,
                    onCustomPrimaryChange = onCustomPrimaryChange
                )
            }
        }
    }

    if (languagePicker) {
        AlertDialog(
            onDismissRequest = { languagePicker = false },
            title = { Text(l10n.languageSection) },
            text = {
                Column {
                    Text(
                        l10n.languageHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    AppLanguage.entries.forEach { lang ->
                        val selected = state.appLanguage == lang
                        val label = if (lang == AppLanguage.SYSTEM) l10n.langSystem else lang.nativeName
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    onAppLanguageChange(lang)
                                    languagePicker = false
                                },
                                label = { Text(label) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { languagePicker = false }) {
                    Text(l10n.done)
                }
            }
        )
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) onCustomBackgroundPicked(uri)
    }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val scheme = MaterialTheme.colorScheme
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            val currentLangLabel =
                if (state.appLanguage == AppLanguage.SYSTEM) l10n.langSystem else state.appLanguage.nativeName
            Surface(
                onClick = { languagePicker = true },
                shape = RoundedCornerShape(16.dp),
                color = scheme.surfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = scheme.secondaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Translate, null, tint = scheme.onSecondaryContainer)
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(l10n.languageSection, fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
                        Text(currentLangLabel, style = MaterialTheme.typography.bodySmall, color = muted)
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = muted)
                }
            }
        }
        item {
            Surface(
                onClick = { themeDetail = true },
                shape = RoundedCornerShape(16.dp),
                color = scheme.surfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = scheme.primaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Palette, null, tint = scheme.onPrimaryContainer)
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(l10n.themeSettings, fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
                        Text(l10n.themeSettingsHint, style = MaterialTheme.typography.bodySmall, color = muted)
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = muted)
                }
            }
        }
        item {
            InfoCard(l10n.appIcon) {
                AppIconStyle.entries.filter { it != AppIconStyle.CUSTOM }.forEach { item ->
                    OptionRow(
                        item.displayName(l10n),
                        l10n.desktopIconStyle,
                        state.appIconStyle == item,
                        iconSwatches(item)
                    ) { onAppIconStyleChange(item) }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
        item {
            InfoCard(l10n.sshSettings) {
                OutlinedTextField(
                    value = state.sshPort.toString(),
                    onValueChange = onSshPortChange,
                    label = { Text(l10n.sshPort) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    enabled = !state.sshConnected && !state.sshConnecting
                )
            }
        }
        item {
            InfoCard(l10n.uiOpacity) {
                Text(l10n.t("chrome_opacity") + " ${(state.chromeAlpha * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = muted)
                Slider(value = state.chromeAlpha, onValueChange = onChromeAlphaChange, valueRange = 0.35f..1f)
                Spacer(Modifier.height(6.dp))
                Text(l10n.t("card_opacity") + " ${(state.cardAlpha * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = muted)
                Slider(value = state.cardAlpha, onValueChange = onCardAlphaChange, valueRange = 0.55f..1f)
            }
        }
        item {
            InfoCard(l10n.customBackground) {
                BackgroundStyle.entries.forEach { item ->
                    OptionRow(
                        item.displayName(l10n),
                        when (item) {
                            BackgroundStyle.NONE -> l10n.solidFollowTheme
                            BackgroundStyle.CUSTOM_IMAGE -> if (state.customBackgroundUri != null) l10n.imageSelected else l10n.pickFromGallery
                            else -> item.displayName(l10n)
                        },
                        state.backgroundStyle == item,
                        listOf(scheme.primaryContainer, scheme.secondaryContainer, scheme.tertiaryContainer)
                    ) {
                        if (item == BackgroundStyle.CUSTOM_IMAGE) {
                            if (state.customBackgroundUri != null) onBackgroundStyleChange(BackgroundStyle.CUSTOM_IMAGE) else picker.launch("image/*")
                        } else onBackgroundStyleChange(item)
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { picker.launch("image/*") }, Modifier.weight(1f)) {
                        Icon(Icons.Default.Image, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(l10n.pickImage)
                    }
                    OutlinedButton(onClick = { onCustomBackgroundPicked(null); onBackgroundStyleChange(BackgroundStyle.NONE) }, Modifier.weight(1f)) { Text(l10n.clearImage) }
                }
                if (state.backgroundStyle == BackgroundStyle.CUSTOM_IMAGE) {
                    Spacer(Modifier.height(8.dp))
                    Text(l10n.t("image_dim") + " ${(state.backgroundDim * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                    Slider(value = state.backgroundDim, onValueChange = onBackgroundDimChange, valueRange = 0.1f..0.75f)
                }
            }
        }
    }
}

@Composable
private fun ThemeDetailScreen(
    state: UiState,
    onBack: () -> Unit,
    onStyleChange: (ThemeStyle) -> Unit,
    onModeChange: (ThemeMode) -> Unit,
    onCustomPrimaryChange: (Long) -> Unit
) {
    val l10n = LocalL10n.current

    val scheme = MaterialTheme.colorScheme
    val muted = scheme.onSurfaceVariant
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.Close, contentDescription = l10n.back)
            }
            Text(
                l10n.themeSettings,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface
            )
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                // Phone preview mock
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = scheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, scheme.outline.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .width(168.dp)
                            .height(240.dp)
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("OpenWrt", style = MaterialTheme.typography.labelMedium, color = muted)
                            Surface(shape = RoundedCornerShape(8.dp), color = scheme.primary.copy(alpha = 0.35f), modifier = Modifier.fillMaxWidth().height(28.dp)) {}
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(shape = RoundedCornerShape(8.dp), color = scheme.secondaryContainer.copy(alpha = 0.7f), modifier = Modifier.weight(1f).height(36.dp)) {}
                                Surface(shape = RoundedCornerShape(8.dp), color = scheme.tertiaryContainer.copy(alpha = 0.7f), modifier = Modifier.weight(1f).height(36.dp)) {}
                            }
                            Surface(shape = RoundedCornerShape(12.dp), color = scheme.surface.copy(alpha = 0.55f), modifier = Modifier.fillMaxWidth().weight(1f)) {}
                            Surface(shape = RoundedCornerShape(14.dp), color = scheme.surface.copy(alpha = 0.7f), modifier = Modifier.fillMaxWidth().height(36.dp)) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Home, null, tint = scheme.primary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
            item {
                // Theme style swatches
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ThemeStyle.entries.forEach { style ->
                        val selected = state.themeStyle == style
                        val colors = swatchesFor(style, state.customPrimaryArgb)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                onClick = { onStyleChange(style) },
                                shape = RoundedCornerShape(16.dp),
                                color = if (selected) scheme.primaryContainer else scheme.surfaceVariant.copy(alpha = 0.55f),
                                modifier = Modifier.size(56.dp),
                                border = if (selected) BorderStroke(2.dp, scheme.primary) else null
                            ) {
                                Box(Modifier.fillMaxSize().padding(8.dp)) {
                                    // quarter pie
                                    Canvas(Modifier.fillMaxSize()) {
                                        val r = size.minDimension / 2f
                                        val c = center
                                        val cols = colors.take(4).ifEmpty { listOf(Color.Gray) }
                                        cols.forEachIndexed { i, col ->
                                            drawArc(
                                                color = col,
                                                startAngle = -90f + i * (360f / cols.size),
                                                sweepAngle = 360f / cols.size,
                                                useCenter = true,
                                                topLeft = Offset(c.x - r, c.y - r),
                                                size = Size(r * 2, r * 2)
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(style.displayName(l10n), style = MaterialTheme.typography.labelSmall, color = if (selected) scheme.primary else muted, maxLines = 1)
                        }
                    }
                }
            }
            item {
                // Mode icons like KernelSU
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeMode.entries.forEach { mode ->
                        val selected = state.themeMode == mode
                        val icon = when (mode) {
                            ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                            ThemeMode.LIGHT -> Icons.Default.LightMode
                            ThemeMode.DARK -> Icons.Default.DarkMode
                            ThemeMode.AMOLED -> Icons.Default.Brightness4
                        }
                        Surface(
                            onClick = { onModeChange(mode) },
                            shape = RoundedCornerShape(18.dp),
                            color = if (selected) scheme.primary else scheme.surfaceVariant.copy(alpha = 0.55f),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(
                                    icon,
                                    contentDescription = mode.displayName(l10n),
                                    tint = if (selected) scheme.onPrimary else scheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            if (state.themeStyle == ThemeStyle.CUSTOM) {
                item {
                    InfoCard(l10n.customPrimary) {
                        PrimarySwatchRow(state.customPrimaryArgb, onCustomPrimaryChange)
                    }
                }
            }
            item {
                InfoCard(l10n.colorHint) {
                    Text(
                        l10n.t("current_combo") + "：${state.themeStyle.displayName(l10n)} · ${state.themeMode.displayName(l10n)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurface
                    )
                    if (state.themeStyle == ThemeStyle.DYNAMIC) {
                        Spacer(Modifier.height(6.dp))
                        Text(l10n.dynamicNeed12, style = MaterialTheme.typography.bodySmall, color = muted)
                    }
                }
            }
        }
    }
}

@Composable
private fun PluginsTab(
    state: UiState,
    onOnlyInstalledChange: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    onToggle: (PluginInfo, Boolean) -> Unit,
    onRestart: (PluginInfo) -> Unit
) {
    val l10n = LocalL10n.current

    val plugins = remember(state.plugins, state.pluginsOnlyInstalled) {
        if (state.pluginsOnlyInstalled) state.plugins.filter { it.installed } else state.plugins
    }
    val installedCount = remember(state.plugins) { state.plugins.count { it.installed } }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "plugins-header") {
            GlassCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(l10n.thirdPartyPlugins, fontWeight = FontWeight.SemiBold)
                        Text(
                            l10n.pluginsSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onRefresh, enabled = !state.pluginsLoading) {
                        Icon(Icons.Default.Refresh, l10n.refresh)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = state.pluginsOnlyInstalled, onCheckedChange = onOnlyInstalledChange)
                    Text(l10n.onlyInstalled)
                }
                if (state.pluginsLoading) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(l10n.scanningPlugins, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    l10n.t("installed_of", installedCount, state.plugins.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!state.pluginsError.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(state.pluginsError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (plugins.isEmpty() && !state.pluginsLoading) {
            item(key = "plugins-empty") {
                EmptyState(
                    state.pluginsError
                        ?: l10n.noPlugins
                )
            }
        } else {
            items(plugins, key = { it.id }) { plugin ->
                Card(
                    Modifier.fillMaxWidth(),
                    shape = cardShape(),
                    colors = CardDefaults.cardColors(
                        containerColor = cardContainerColor(),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(if (plugin.name.startsWith("plugin_")) l10n.t(plugin.name) else plugin.name, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                Text(
                                    l10n.t(plugin.description),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (plugin.installed && plugin.initScript != null) {
                                Switch(checked = plugin.enabled, onCheckedChange = { onToggle(plugin, it) })
                            }
                        }
                        KeyValue(l10n.packageName, plugin.id)
                        KeyValue(l10n.category, l10n.t(plugin.category))
                        KeyValue(l10n.status, if (!plugin.installed) l10n.notInstalled else if (plugin.enabled) l10n.enabled else l10n.installed)
                        KeyValue(l10n.version, if (plugin.version.startsWith("status_")) l10n.t(plugin.version) else plugin.version)
                        if (plugin.installed && plugin.initScript != null) {
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { onRestart(plugin) },
                                Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text(l10n.restartService) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewTab(state: UiState) {
    val l10n = LocalL10n.current

    val info = state.systemInfo
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            InfoCard(l10n.system) {
                KeyValue(l10n.hostname, info?.hostname ?: "-")
                KeyValue(l10n.model, info?.model ?: "-")
                KeyValue(l10n.version, info?.releaseDescription ?: "-")
                KeyValue(l10n.kernel, info?.kernelVersion ?: "-")
                KeyValue(l10n.uptime, formatUptime(info?.uptimeSeconds ?: 0))
            }
        }
        item {
            InfoCard(l10n.load) {
                KeyValue(l10n.min1, String.format("%.2f", info?.load1 ?: 0.0))
                KeyValue(l10n.min5, String.format("%.2f", info?.load5 ?: 0.0))
                KeyValue(l10n.min15, String.format("%.2f", info?.load15 ?: 0.0))
            }
        }
        item {
            val total = (info?.memTotal ?: 0L).coerceAtLeast(1L)
            val used = total - (info?.memAvailable ?: info?.memFree ?: 0L)
            val ratio = (used.toFloat() / total.toFloat()).coerceIn(0f, 1f)
            InfoCard(l10n.memory) {
                KeyValue(l10n.total, formatBytes(info?.memTotal ?: 0))
                KeyValue(l10n.available, formatBytes(info?.memAvailable ?: info?.memFree ?: 0))
                KeyValue(l10n.used, formatBytes(used))
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { ratio }, Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)))
            }
        }
        item {
            InfoCard(l10n.storage) {
                if (state.storageVolumes.isEmpty()) {
                    Text(l10n.noMounts, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    state.storageVolumes.forEachIndexed { idx, vol ->
                        if (idx > 0) Spacer(Modifier.height(10.dp))
                        Text(vol.mount, fontWeight = FontWeight.SemiBold)
                        Text(
                            listOf(vol.device, vol.fstype).filter { it.isNotBlank() && it != "-" }.joinToString(" / ").ifBlank { "-" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (vol.totalBytes > 0L) {
                            KeyValue(
                                l10n.usedTotal,
                                "${formatBytes(vol.usedBytes)} / ${formatBytes(vol.totalBytes)} (${vol.usedPercent}%)"
                            )
                            KeyValue(l10n.available, formatBytes(vol.freeBytes))
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { (vol.usedPercent / 100f).coerceIn(0f, 1f) },
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                            )
                        }
                    }
                }
            }
        }
        item {
            InfoCard(l10n.networkUpstream) {
                val ifaces = state.networkIfaces
                if (ifaces.isEmpty()) {
                    Text(l10n.noIfaces, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    val upstream = ifaces.filter { it.role == l10n.upstream || it.name.contains("wan", true) }
                    val show = if (upstream.isNotEmpty()) upstream else ifaces.take(6)
                    show.forEachIndexed { idx, nic ->
                        if (idx > 0) Spacer(Modifier.height(10.dp))
                        val status = if (nic.up) l10n.connected else l10n.notConnected
                        Text("${nic.name} · ${if (nic.role.startsWith("role_")) l10n.t(nic.role) else nic.role} · $status", fontWeight = FontWeight.SemiBold)
                        KeyValue(l10n.protoDevice, "${nic.proto} / ${nic.device}")
                        KeyValue("IPv4", nic.ipv4.joinToString(", ").ifBlank { "-" })
                        KeyValue("IPv6", nic.ipv6.joinToString(", ").ifBlank { "-" })
                        if (nic.mac.isNotBlank() && nic.mac != "-") KeyValue("MAC", nic.mac)
                        if (nic.rxBytes > 0 || nic.txBytes > 0) {
                            KeyValue(l10n.traffic, "↓ ${formatBytes(nic.rxBytes)}  ↑ ${formatBytes(nic.txBytes)}")
                        }
                    }
                }
            }
        }
        item {
            InfoCard(l10n.ports) {
                val ports = state.networkIfaces.filter { it.device.isNotBlank() && it.device != "-" }
                if (ports.isEmpty()) {
                    Text(l10n.noPortData, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    ports.forEach { nic ->
                        KeyValue(
                            nic.name,
                            buildString {
                                append(if (nic.up) "UP" else "DOWN")
                                append(" · ")
                                append(nic.device)
                                if (nic.ipv4.isNotEmpty()) append(" · ").append(nic.ipv4.first())
                            }
                        )
                    }
                }
            }
        }
        item {
            InfoCard(l10n.summary) {
                KeyValue(l10n.onlineDevices, "${state.devices.size}")
                KeyValue(l10n.installedPlugins, "${state.plugins.count { it.installed }}")
                KeyValue(l10n.storageVolumes, "${state.storageVolumes.size}")
                KeyValue(l10n.networkIfaces, "${state.networkIfaces.size}")
            }
        }
    }
}

@Composable
private fun DevicesTab(state: UiState) {
    val l10n = LocalL10n.current

    if (state.devices.isEmpty()) { EmptyState(l10n.noDhcp); return }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(state.devices, key = { it.mac + it.ip }) { d ->
            GlassCard {
                Text(d.hostname.ifBlank { l10n.unknownDevice }, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                KeyValue("IP", d.ip); KeyValue("MAC", d.mac); KeyValue(l10n.network, d.network); KeyValue(l10n.lease, d.expires)
            }
        }
    }
}

@Composable
private fun WifiTab(state: UiState) {
    val l10n = LocalL10n.current

    if (state.wireless.isEmpty()) { EmptyState(l10n.noWireless); return }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(state.wireless, key = { it.ifname + it.ssid }) { w ->
            GlassCard {
                Text(w.ssid.ifBlank { w.ifname }, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                KeyValue(l10n.iface, w.ifname); KeyValue(l10n.mode, w.mode); KeyValue(l10n.channel, w.channel); KeyValue(l10n.signal, w.signal); KeyValue(l10n.rate, w.bitrate)
            }
        }
    }
}


@Composable
private fun SshTab(
    state: UiState,
    onPortChange: (String) -> Unit,
    onCommandChange: (String) -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onSend: () -> Unit,
    onClear: () -> Unit,
    onResize: (Int, Int) -> Unit = { _, _ -> },
    onRawSend: (String) -> Unit = {}
) {
    val l10n = LocalL10n.current

    val scroll = rememberScrollState()
    val hScroll = rememberScrollState()
    var fontSp by remember { mutableStateOf(8f) }
    var softWrap by remember { mutableStateOf(true) }
    // Local unsent line buffer is NOT painted — display is remote buffer only.
    // Avoids "deleted text still visible" when echo + draft double-render.
    var draft by remember { mutableStateOf("") }
    var lastCols by remember { mutableStateOf(0) }
    var lastRows by remember { mutableStateOf(0) }
    var pendingCols by remember { mutableStateOf(0) }
    var pendingRows by remember { mutableStateOf(0) }
    val focusRequester = remember { FocusRequester() }
    val density = LocalDensity.current
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(state.sshOutput) {
        scroll.animateScrollTo(scroll.maxValue)
    }
    LaunchedEffect(state.sshConnected) {
        if (!state.sshConnected) {
            draft = ""
            lastCols = 0
            lastRows = 0
            pendingCols = 0
            pendingRows = 0
        } else {
            // Bind IME action key before the first Enter press
            delay(120)
            runCatching {
                focusRequester.requestFocus()
                keyboard?.show()
            }
        }
    }
    LaunchedEffect(pendingCols, pendingRows, state.sshConnected) {
        if (!state.sshConnected) return@LaunchedEffect
        if (pendingCols <= 0 || pendingRows <= 0) return@LaunchedEffect
        delay(280)
        if (pendingCols != lastCols || pendingRows != lastRows) {
            lastCols = pendingCols
            lastRows = pendingRows
            onResize(pendingCols, pendingRows)
        }
    }

    val statusColor = when {
        state.sshConnected -> Color(0xFF98C379)
        state.sshConnecting -> Color(0xFFE5C07B)
        else -> Color(0xFF9AA7B5)
    }
    val termBg = Color(0xFF0D1117)
    val termFg = Color(0xFFD7E0EA)
    val annotated = remember(state.sshOutput) {
        if (state.sshOutput.isBlank()) AnnotatedString("")
        else AnsiStrip.toAnnotatedString(state.sshOutput, termFg)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(termBg)
            .imePadding()
    ) {
        // Compact toolbar: host/status left; dense actions right (no edge clipping).
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 40.dp)
                .padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val statusText = when {
                state.sshConnecting -> l10n.connecting
                state.sshConnected -> "${state.host}:${state.sshPort}"
                else -> l10n.notConnected
            }
            Text(
                text = statusText,
                color = statusColor,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                ),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 2.dp),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(
                onClick = { fontSp = (fontSp - 1f).coerceAtLeast(8f) },
                modifier = Modifier.size(30.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = l10n.zoomOut, tint = termFg, modifier = Modifier.size(16.dp))
            }
            IconButton(
                onClick = { fontSp = (fontSp + 1f).coerceAtMost(21f) },
                modifier = Modifier.size(30.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = l10n.zoomIn, tint = termFg, modifier = Modifier.size(16.dp))
            }
            Text(
                text = if (softWrap) l10n.wrap else l10n.hScroll,
                color = termFg.copy(alpha = 0.88f),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, lineHeight = 14.sp),
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .clickable { softWrap = !softWrap }
                    .padding(horizontal = 6.dp, vertical = 8.dp)
            )
            if (state.sshConnected) {
                Text(
                    text = l10n.disconnect,
                    color = Color(0xFFFF8A80),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, lineHeight = 14.sp),
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier
                        .clickable(onClick = onDisconnect)
                        .padding(horizontal = 6.dp, vertical = 8.dp)
                )
            } else {
                Text(
                    text = if (state.sshConnecting) "..." else l10n.connect,
                    color = if (state.sshConnecting) Color(0xFFE5C07B) else Color(0xFF98C379),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, lineHeight = 14.sp),
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier
                        .clickable(
                            enabled = !state.sshConnecting && state.password.isNotBlank(),
                            onClick = onConnect
                        )
                        .padding(horizontal = 6.dp, vertical = 8.dp)
                )
            }
            Text(
                text = l10n.clearScreen,
                color = termFg.copy(alpha = 0.75f),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, lineHeight = 14.sp),
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .clickable {
                        draft = ""
                        onClear()
                    }
                    .padding(horizontal = 6.dp, vertical = 8.dp)
            )
        }

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 6.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    if (state.sshConnected) {
                        focusRequester.requestFocus()
                        keyboard?.show()
                    }
                }
                .onSizeChanged { size ->
                    if (size.width <= 0 || size.height <= 0) return@onSizeChanged
                    val cols = with(density) {
                        (size.width.toDp().value / (fontSp * 0.60f)).toInt().coerceIn(40, 240)
                    }
                    val rows = with(density) {
                        (size.height.toDp().value / (fontSp * 1.30f)).toInt().coerceIn(8, 80)
                    }
                    if (cols != pendingCols || rows != pendingRows) {
                        pendingCols = cols
                        pendingRows = rows
                    }
                }
        ) {
            val mod = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .then(if (softWrap) Modifier else Modifier.horizontalScroll(hScroll))
                .padding(8.dp)
            Text(
                text = if (annotated.isEmpty()) {
                    AnnotatedString(if (state.sshConnected) "" else l10n.tapToType)
                } else {
                    annotated
                },
                modifier = mod,
                style = TextStyle(
                    fontFamily = TerminalFontFamily,
                    fontSize = fontSp.sp,
                    lineHeight = (fontSp * 1.30f).sp,
                    color = termFg
                ),
                softWrap = softWrap
            )
            // Hidden input: only sends keystrokes; never paints draft over remote echo
            BasicTextField(
                value = draft,
                onValueChange = { nv ->
                    if (!state.sshConnected) return@BasicTextField
                    if (nv == draft) return@BasicTextField
                    // Enter
                    if (nv.contains('\n') || nv.contains('\r')) {
                        val cleaned = nv.replace("\r\n", "\n").replace('\r', '\n')
                        val before = cleaned.substringBefore('\n')
                        if (before.length > draft.length && before.startsWith(draft)) {
                            val extra = before.substring(draft.length)
                            if (extra.isNotEmpty()) onRawSend(extra)
                        } else if (before.isNotEmpty() && before != draft) {
                            // IME replaced whole line — send as-is once without DEL spam
                            if (draft.isNotEmpty()) {
                                // clear remote line buffer: Ctrl+U
                                onRawSend("\u0015")
                            }
                            if (before.isNotEmpty()) onRawSend(before)
                        }
                        onRawSend("\r")
                        draft = ""
                        return@BasicTextField
                    }
                    when {
                        nv.length > draft.length && nv.startsWith(draft) -> {
                            onRawSend(nv.substring(draft.length))
                            draft = nv
                        }
                        nv.length < draft.length && draft.startsWith(nv) -> {
                            val del = draft.length - nv.length
                            repeat(del) { onRawSend("\u007f") }
                            draft = nv
                        }
                        else -> {
                            // Composition / replace: Ctrl+U then new text
                            if (draft.isNotEmpty()) onRawSend("\u0015")
                            if (nv.isNotEmpty()) onRawSend(nv)
                            draft = nv
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .align(Alignment.BottomStart)
                    .focusRequester(focusRequester)
                    .alpha(0.02f)
                    .onPreviewKeyEvent { e ->
                        if (e.type == KeyEventType.KeyDown &&
                            (e.key == Key.Enter || e.key == Key.NumPadEnter)
                        ) {
                            if (state.sshConnected) {
                                onRawSend("\r")
                                draft = ""
                            }
                            true
                        } else {
                            false
                        }
                    },
                textStyle = TextStyle(color = Color.Transparent, fontSize = 1.sp),
                cursorBrush = SolidColor(Color.Transparent),
                enabled = state.sshConnected,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send,
                    keyboardType = KeyboardType.Text,
                    capitalization = KeyboardCapitalization.None,
                    autoCorrect = false
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (state.sshConnected) {
                            onRawSend("\r")
                            draft = ""
                        }
                    },
                    onDone = {
                        if (state.sshConnected) {
                            onRawSend("\r")
                            draft = ""
                        }
                    },
                    onGo = {
                        if (state.sshConnected) {
                            onRawSend("\r")
                            draft = ""
                        }
                    }
                )
            )
        }
    }
}


@Composable
private fun ActionsTab(
    state: UiState,
    loading: Boolean,
    onSectionChange: (ToolsSection) -> Unit,
    onReboot: () -> Unit,
    onRestartNetwork: () -> Unit,
    onRestartWifi: () -> Unit,
    onRefresh: () -> Unit,
    onRefreshProcesses: () -> Unit,
    onProcessQueryChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit = {},
    onConfirmPasswordChange: (String) -> Unit = {},
    onChangePassword: () -> Unit = {}
) {
    val l10n = LocalL10n.current

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                ToolsSection.ACTIONS to l10n.tabActions,
                ToolsSection.PROCESSES to l10n.processes
            ).forEach { (sec, label) ->
                FilterChip(
                    selected = state.toolsSection == sec,
                    onClick = { onSectionChange(sec) },
                    label = { Text(label) },
                    leadingIcon = if (state.toolsSection == sec) {
                        { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                    } else null
                )
            }
        }

        when (state.toolsSection) {
            ToolsSection.ACTIONS -> {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(l10n.quickActions, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    FilledTonalButton(onClick = onRefresh, enabled = !loading, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Text(l10n.refreshStatus) }
                    OutlinedButton(onClick = onRestartWifi, enabled = !loading, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Text(l10n.reloadWifi) }
                    OutlinedButton(onClick = onRestartNetwork, enabled = !loading, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Text(l10n.restartNetwork) }
                    Button(onClick = onReboot, enabled = !loading, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Text(l10n.rebootRouter) }

                    Text(l10n.changePassword, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    OutlinedTextField(
                        value = state.newPassword,
                        onValueChange = onNewPasswordChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(l10n.newPassword) },
                        visualTransformation = PasswordVisualTransformation()
                    )
                    OutlinedTextField(
                        value = state.confirmPassword,
                        onValueChange = onConfirmPasswordChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(l10n.confirmPassword) },
                        visualTransformation = PasswordVisualTransformation()
                    )
                    FilledTonalButton(
                        onClick = onChangePassword,
                        enabled = !loading && state.newPassword.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text(l10n.t("update_user_password", state.username)) }
                }
            }
            ToolsSection.PROCESSES -> ProcessesPane(
                state = state,
                onRefresh = onRefreshProcesses,
                onQueryChange = onProcessQueryChange
            )
            ToolsSection.LOGS -> Unit
        }
    }
}

@Composable
private fun ProcessesPane(
    state: UiState,
    onRefresh: () -> Unit,
    onQueryChange: (String) -> Unit
) {
    val l10n = LocalL10n.current

    val q = state.processQuery.trim()
    val list = remember(state.processes, q) {
        if (q.isBlank()) state.processes
        else state.processes.filter {
            it.command.contains(q, true) || it.pid.contains(q) || it.user.contains(q, true)
        }
    }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.processQuery,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text(l10n.searchProcess) },
                placeholder = { Text(l10n.pidName) }
            )
            FilledTonalButton(onClick = onRefresh, enabled = !state.processesLoading, shape = RoundedCornerShape(12.dp)) {
                if (state.processesLoading) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
                }
            }
        }
        Text(
            if (state.processesLoading) l10n.readingProcesses else l10n.t("process_count", list.size),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!state.processesError.isNullOrBlank()) {
            Text(state.processesError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            if (list.isEmpty() && !state.processesLoading) {
                item {
                    GlassCard {
                        Text(l10n.noData, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            items(list, key = { it.pid + it.command }) { p ->
                GlassCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("PID ${p.pid}", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        Text(p.user, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(p.command, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "CPU ${p.cpu}  MEM ${p.mem}  RSS ${p.rss}  STAT ${p.stat}  TIME ${p.time}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun OptionRow(title: String, subtitle: String, selected: Boolean, swatches: List<Color>, onClick: () -> Unit) {
    val l10n = LocalL10n.current

    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape)
            .border(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, shape)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = LocalCardAlpha.current) else cardContainerColor())
            .clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { swatches.take(3).forEach { ColorDot(it, 18.dp) } }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (selected) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable private fun ColorDot(color: Color, size: Dp = 22.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(color).border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape))
}

@Composable private fun swatchesFor(style: ThemeStyle, customArgb: Long = 0xFFB12D5B): List<Color> = when (style) {
    ThemeStyle.SAKURA -> listOf(Color(0xFFB12D5B), Color(0xFFFFD9E2), Color(0xFF7C5734))
    ThemeStyle.DYNAMIC -> listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.tertiary)
    ThemeStyle.MINT -> listOf(Color(0xFF006C4C), Color(0xFF89F8C7), Color(0xFF3D6373))
    ThemeStyle.OCEAN -> listOf(Color(0xFF00658C), Color(0xFFC5E7FF), Color(0xFF615A7C))
    ThemeStyle.CUSTOM -> {
        val c = Color(customArgb or 0xFFB12D5B)
        listOf(c, c.copy(alpha = 0.55f), c.copy(alpha = 0.25f))
    }
}

@Composable
private fun PrimarySwatchRow(selected: Long, onPick: (Long) -> Unit) {
    val scroll = rememberScrollState()
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(scroll),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CustomPrimaryPresets.forEach { argb ->
            val color = Color(argb or 0xFF000000)
            val isSel = (selected or 0xFF000000) == (argb or 0xFF000000)
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (isSel) 3.dp else 1.dp,
                        color = if (isSel) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape
                    )
                    .clickable { onPick(argb or 0xFF000000) }
            )
        }
    }
}

@Composable
private fun AppInfoDialog(state: UiState, onDismiss: () -> Unit) {
    val l10n = LocalL10n.current

    val ctx = LocalContext.current
    val customFile = remember { File(ctx.filesDir, "custom_launcher_icon.png") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(l10n.appInfo) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(72.dp)) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        if (state.appIconStyle == AppIconStyle.CUSTOM && customFile.exists()) {
                            FileBitmapImage(
                                file = customFile,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.Router, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
                Text(l10n.appName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                Text(l10n.t("version_fmt", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(l10n.authorLine, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(l10n.close) }
        }
    )
}

@Composable private fun iconSwatches(style: AppIconStyle): List<Color> = when (style) {
    AppIconStyle.SAKURA -> listOf(Color(0xFFB12D5B), Color(0xFFFF8FB5), Color(0xFFFFD9E2))
    AppIconStyle.MINT -> listOf(Color(0xFF006C4C), Color(0xFF6CDBAB), Color(0xFFC8F5E0))
    AppIconStyle.OCEAN -> listOf(Color(0xFF00658C), Color(0xFF80CFFF), Color(0xFFC5E7FF))
    AppIconStyle.MINIMAL -> listOf(Color(0xFF333333), Color(0xFF888888), Color(0xFFF2F2F2))
    AppIconStyle.CUSTOM -> listOf(Color(0xFFB12D5B), Color(0xFFFFD9E2), Color(0xFFFF8FB5))
}

@Composable private fun cardContainerColor(): Color {
    val alpha = LocalCardAlpha.current.coerceIn(0.55f, 1f)
    return MaterialTheme.colorScheme.surface.copy(alpha = alpha)
}

@Composable private fun cardShape(): RoundedCornerShape = RoundedCornerShape(16.dp)


@Composable private fun GlassCard(content: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Card(
        Modifier.fillMaxWidth(),
        shape = cardShape(),
        colors = CardDefaults.cardColors(
            containerColor = cardContainerColor(),
            contentColor = scheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(16.dp)) { content() }
    }
}

@Composable private fun InfoCard(title: String, content: @Composable () -> Unit) {
    val l10n = LocalL10n.current

    val scheme = MaterialTheme.colorScheme
    Card(
        Modifier.fillMaxWidth(),
        shape = cardShape(),
        colors = CardDefaults.cardColors(
            containerColor = cardContainerColor(),
            contentColor = scheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface
            )
            Spacer(Modifier.height(10.dp)); content()
        }
    }
}

@Composable private fun KeyValue(key: String, value: String) {
    val l10n = LocalL10n.current

    val scheme = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(key, color = scheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Text(value, fontWeight = FontWeight.Medium, color = scheme.onSurface, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable private fun EmptyState(text: String) {
    val l10n = LocalL10n.current

    Column(
        Modifier.fillMaxWidth().padding(24.dp),
        Arrangement.Center,
        Alignment.CenterHorizontally
    ) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    }
}
