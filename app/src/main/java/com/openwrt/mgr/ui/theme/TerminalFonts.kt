package com.openwrt.mgr.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.openwrt.mgr.R

/**
 * Terminal-only typeface: Ubuntu Mono Nerd Font (powerline / hyfetch glyphs).
 * Not used by the rest of the UI (system sans).
 */
val TerminalFontFamily: FontFamily = FontFamily(
    Font(R.font.ubuntu_mono_nerd, weight = FontWeight.Normal),
    Font(R.font.ubuntu_mono_nerd, weight = FontWeight.Bold),
)
