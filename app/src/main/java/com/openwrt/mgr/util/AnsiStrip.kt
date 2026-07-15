package com.openwrt.mgr.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * Streaming ANSI parser for interactive SSH (oh-my-posh / hyfetch / xterm-256color).
 * Buffers incomplete ESC sequences across network chunks so CSI never leaks as plain text.
 */
object AnsiStrip {
    private val backspace = Regex(".\u0008")
    private val hyperlink = Regex("""\u001B]8;;.*?(?:\u0007|\u001B\\)""")
    private val title = Regex("""\u001B]0;.*?(?:\u0007|\u001B\\)""")
    private val osc = Regex("""\u001B\][^\u0007\u001B]*(?:\u0007|\u001B\\)""")
    private val dcs = Regex("""\u001BP.*?\u001B\\""", RegexOption.DOT_MATCHES_ALL)
    private val bel = Regex("\u0007")
    private val crOnly = Regex("\r(?!\n)")

    private val basic = listOf(
        Color(0xFF1A1A1A),
        Color(0xFFE06C75),
        Color(0xFF98C379),
        Color(0xFFE5C07B),
        Color(0xFF61AFEF),
        Color(0xFFC678DD),
        Color(0xFF56B6C2),
        Color(0xFFD7E0EA)
    )
    private val bright = listOf(
        Color(0xFF5C6370),
        Color(0xFFFF8A80),
        Color(0xFFB5E890),
        Color(0xFFFFD180),
        Color(0xFF82B1FF),
        Color(0xFFE1BEE7),
        Color(0xFF80DEEA),
        Color(0xFFFFFFFF)
    )

    /**
     * Holds incomplete ESC tail between read() chunks.
     * Call [feed] for each SSH chunk; [reset] on clear/disconnect.
     */
    class StreamBuffer {
        private val pending = StringBuilder()

        fun reset() {
            pending.clear()
        }

        /** Returns complete, display-safe text (SGR kept, other CSI/OSC dropped). */
        @Synchronized
        fun feed(chunk: String): String {
            if (chunk.isEmpty() && pending.isEmpty()) return ""
            pending.append(chunk)
            val raw = pending.toString()
            val (complete, rest) = splitIncompleteEsc(raw)
            pending.clear()
            if (rest.isNotEmpty()) pending.append(rest)
            return keepSgrOnly(preprocess(complete))
        }
    }

    fun sanitizeForTerminal(input: String): String =
        keepSgrOnly(preprocess(input))

    fun clean(input: String): String {
        var s = preprocess(input)
        s = s.replace(Regex("""\u001B\[[0-9;]*m"""), "")
        s = s.replace(Regex("""\u001B\[[0-?]*[ -/]*[@-~]"""), "")
        s = s.replace(Regex("""\u001B[()][0-9A-Za-z]"""), "")
        s = s.replace(Regex("""\u001B[@-Z\\-_]"""), "")
        return filterControls(s)
    }

    fun toAnnotatedString(
        input: String,
        defaultColor: Color = Color(0xFFD7E0EA)
    ): AnnotatedString {
        val prepared = keepSgrOnly(preprocess(input))
        return parseSgr(prepared, defaultColor)
    }

    /**
     * If [text] ends inside an ESC sequence, return (safePrefix, incompleteTail).
     */
    private fun splitIncompleteEsc(text: String): Pair<String, String> {
        if (text.isEmpty()) return "" to ""
        val esc = text.lastIndexOf('\u001B')
        if (esc < 0) return text to ""

        val tail = text.substring(esc)
        // Complete BEL-terminated OSC: ESC ] ... BEL
        // Complete ST-terminated: ESC ] ... ESC \
        // Complete CSI: ESC [ ... final-byte @-~
        // Charset: ESC ( X or ESC ) X
        // Single-byte ESC: ESC @-_
        if (isCompleteEsc(tail)) {
            return text to ""
        }
        // Incomplete: hold from last ESC
        return text.substring(0, esc) to tail
    }

    private fun isCompleteEsc(seq: String): Boolean {
        if (seq.isEmpty() || seq[0] != '\u001B') return true
        if (seq.length == 1) return false
        val n = seq[1]
        return when {
            n == '[' -> {
                // CSI: intermediate bytes then final in @-~
                var i = 2
                while (i < seq.length) {
                    val c = seq[i]
                    if (c in '@'..'~') return true
                    i++
                }
                false
            }
            n == ']' -> {
                // OSC ends with BEL or ESC \
                if (seq.indexOf('\u0007') >= 0) return true
                if (seq.indexOf("\u001B\\") >= 0) return true
                false
            }
            n == 'P' || n == 'X' || n == '^' || n == '_' -> {
                // DCS/SOS/PM/APC — end with ST ESC \
                seq.indexOf("\u001B\\") >= 0
            }
            n == '(' || n == ')' -> seq.length >= 3
            n in '@'..'_' -> true
            else -> true // unknown, don't stall forever
        }
    }

    private fun preprocess(input: String): String {
        var s = input
        var prev: String
        do {
            prev = s
            s = s.replace(backspace, "")
        } while (s != prev)
        s = hyperlink.replace(s, "")
        s = title.replace(s, "")
        s = osc.replace(s, "")
        s = dcs.replace(s, "")
        s = bel.replace(s, "")
        s = crOnly.replace(s, "")
        // strip leftover CR
        s = s.replace("\r", "")
        return s
    }

    private fun keepSgrOnly(input: String): String {
        val sb = StringBuilder(input.length)
        var i = 0
        while (i < input.length) {
            val ch = input[i]
            if (ch == '\u001B') {
                if (i + 1 < input.length && input[i + 1] == '[') {
                    val end = (i + 2 until input.length).firstOrNull { input[it] in '@'..'~' }
                    if (end != null && input[end] == 'm') {
                        sb.append(input, i, end + 1)
                        i = end + 1
                        continue
                    } else if (end != null) {
                        // non-SGR CSI (cursor etc.) drop
                        i = end + 1
                        continue
                    } else {
                        // incomplete — should not happen after splitIncompleteEsc
                        break
                    }
                }
                if (i + 1 < input.length) {
                    val n = input[i + 1]
                    if (n == '(' || n == ')') {
                        i += minOf(3, input.length - i)
                        continue
                    }
                    if (n == ']') {
                        // incomplete OSC should be buffered; if present drop till BEL/ST or end
                        val belIdx = input.indexOf('\u0007', i)
                        val stIdx = input.indexOf("\u001B\\", i)
                        val cut = when {
                            belIdx >= 0 && (stIdx < 0 || belIdx < stIdx) -> belIdx + 1
                            stIdx >= 0 -> stIdx + 2
                            else -> input.length
                        }
                        i = cut
                        continue
                    }
                    if (n in '@'..'_') {
                        i += 2
                        continue
                    }
                }
                i++
                continue
            }
            sb.append(ch)
            i++
        }
        return filterControls(sb.toString())
    }

    private fun filterControls(s: String): String = buildString(s.length) {
        for (ch in s) {
            val c = ch.code
            if (ch == '\n' || ch == '\t' || ch == '\u001B' || c >= 32) {
                append(ch)
            }
        }
    }

    private data class StyleState(
        var fg: Color? = null,
        var bg: Color? = null,
        var bold: Boolean = false,
        var italic: Boolean = false,
        var dim: Boolean = false,
        var underline: Boolean = false
    )

    private fun parseSgr(text: String, defaultColor: Color): AnnotatedString {
        val state = StyleState()
        return buildAnnotatedString {
            var i = 0
            while (i < text.length) {
                if (text[i] == '\u001B' && i + 1 < text.length && text[i + 1] == '[') {
                    val end = (i + 2 until text.length).firstOrNull { text[it] == 'm' }
                    if (end != null) {
                        val params = text.substring(i + 2, end)
                        applySgr(state, params)
                        i = end + 1
                        continue
                    }
                }
                val next = text.indexOf('\u001B', i).let { if (it < 0) text.length else it }
                val chunk = text.substring(i, next)
                if (chunk.isNotEmpty()) {
                    val fg = when {
                        state.fg != null && state.dim -> state.fg!!.copy(alpha = 0.65f)
                        state.fg != null -> state.fg!!
                        state.dim -> defaultColor.copy(alpha = 0.65f)
                        else -> defaultColor
                    }
                    withStyle(
                        SpanStyle(
                            color = fg,
                            background = state.bg ?: Color.Unspecified,
                            fontWeight = if (state.bold) FontWeight.Bold else FontWeight.Normal,
                            fontStyle = if (state.italic) FontStyle.Italic else FontStyle.Normal
                        )
                    ) {
                        append(chunk)
                    }
                }
                i = next
            }
        }
    }

    private fun applySgr(state: StyleState, params: String) {
        if (params.isBlank()) {
            state.fg = null
            state.bg = null
            state.bold = false
            state.italic = false
            state.dim = false
            state.underline = false
            return
        }
        // Support broken/split param forms: also handle "38;2;r;g;b" fully
        val parts = params.split(';').mapNotNull { p ->
            p.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()
        }
        var idx = 0
        while (idx < parts.size) {
            when (val code = parts[idx]) {
                0 -> {
                    state.fg = null
                    state.bg = null
                    state.bold = false
                    state.italic = false
                    state.dim = false
                    state.underline = false
                }
                1 -> state.bold = true
                2 -> state.dim = true
                3 -> state.italic = true
                4 -> state.underline = true
                22 -> {
                    state.bold = false
                    state.dim = false
                }
                23 -> state.italic = false
                24 -> state.underline = false
                in 30..37 -> state.fg = basic[code - 30]
                38 -> {
                    val (color, consumed) = extendedColor(parts, idx + 1)
                    if (color != null) state.fg = color
                    idx += consumed
                }
                39 -> state.fg = null
                in 40..47 -> state.bg = basic[code - 40].copy(alpha = 0.40f)
                48 -> {
                    val (color, consumed) = extendedColor(parts, idx + 1)
                    if (color != null) state.bg = color.copy(alpha = 0.40f)
                    idx += consumed
                }
                49 -> state.bg = null
                in 90..97 -> state.fg = bright[code - 90]
                in 100..107 -> state.bg = bright[code - 100].copy(alpha = 0.40f)
            }
            idx++
        }
    }

    private fun extendedColor(parts: List<Int>, start: Int): Pair<Color?, Int> {
        if (start >= parts.size) return null to 0
        return when (parts[start]) {
            5 -> {
                if (start + 1 >= parts.size) return null to 1
                val n = parts[start + 1].coerceIn(0, 255)
                xterm256(n) to 2
            }
            2 -> {
                if (start + 3 >= parts.size) return null to 1
                val r = parts[start + 1].coerceIn(0, 255)
                val g = parts[start + 2].coerceIn(0, 255)
                val b = parts[start + 3].coerceIn(0, 255)
                Color(r, g, b) to 4
            }
            else -> null to 1
        }
    }

    private fun xterm256(n: Int): Color {
        if (n < 8) return basic[n]
        if (n < 16) return bright[n - 8]
        if (n < 232) {
            val v = n - 16
            val r = v / 36
            val g = (v % 36) / 6
            val b = v % 6
            fun c(x: Int) = if (x == 0) 0 else 55 + x * 40
            return Color(c(r), c(g), c(b))
        }
        val gray = 8 + (n - 232) * 10
        return Color(gray, gray, gray)
    }
}
