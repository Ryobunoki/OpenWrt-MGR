package com.openwrt.mgr.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.openwrt.mgr.R

/**
 * Terminal-only typeface: JetBrains Mono variable (single TTF in APK).
 * Not used by the rest of the UI (system sans).
 */
val TerminalFontFamily: FontFamily = FontFamily(
    Font(R.font.jb_mono_vf, weight = FontWeight.Normal),
    Font(R.font.jb_mono_vf, weight = FontWeight.Bold),
)
