package com.openwrt.mgr.util

/**
 * Lightweight VT-ish text buffer for interactive SSH.
 * Applies CR/LF/BS and common CSI erase/cursor ops so shell echo
 * (backspace, oh-my-posh redraw) updates display correctly.
 * SGR color sequences are kept as zero-width markers for AnsiStrip rendering.
 */
class TerminalBuffer(private val maxChars: Int = 120_000) {
    private val lines = mutableListOf(StringBuilder())
    private var row = 0
    private var col = 0
    private val pendingEsc = StringBuilder()

    fun reset() {
        lines.clear()
        lines += StringBuilder()
        row = 0
        col = 0
        pendingEsc.clear()
    }

    fun feed(chunk: String): String {
        if (chunk.isEmpty() && pendingEsc.isEmpty()) return snapshot()
        val src = if (pendingEsc.isNotEmpty()) {
            val merged = pendingEsc.toString() + chunk
            pendingEsc.clear()
            merged
        } else {
            chunk
        }
        var i = 0
        while (i < src.length) {
            val ch = src[i]
            when {
                ch == ESC -> {
                    val rest = src.substring(i)
                    val consumed = consumeEsc(rest)
                    if (consumed < 0) {
                        pendingEsc.append(rest)
                        break
                    }
                    i += consumed
                }
                ch == '\r' -> {
                    col = 0
                    i++
                }
                ch == '\n' -> {
                    newLine()
                    i++
                }
                ch == BS -> {
                    // BS moves cursor left only; erase is typically BS SPACE BS
                    col = (col - 1).coerceAtLeast(0)
                    i++
                }
                ch == DEL -> {
                    if (col > 0) {
                        col--
                        deleteChars(1)
                    }
                    i++
                }
                ch == '\t' -> {
                    val spaces = 4 - (col % 4)
                    repeat(spaces) { putPrintable(' ') }
                    i++
                }
                ch == BEL -> i++
                ch.code < 32 -> i++
                else -> {
                    putPrintable(ch)
                    i++
                }
            }
        }
        trimSize()
        return snapshot()
    }

    fun snapshot(): String = buildString(lines.sumOf { it.length } + lines.size) {
        lines.forEachIndexed { idx, sb ->
            append(sb)
            if (idx < lines.lastIndex) append('\n')
        }
    }

    private fun consumeEsc(seq: String): Int {
        if (seq.length < 2) return -1
        return when (seq[1]) {
            '[' -> {
                var j = 2
                while (j < seq.length) {
                    val c = seq[j]
                    if (c in '@'..'~') {
                        applyCsi(seq.substring(2, j), c)
                        return j + 1
                    }
                    j++
                }
                -1
            }
            ']' -> {
                val bel = seq.indexOf(BEL)
                val st = seq.indexOf(ST)
                when {
                    bel >= 0 && (st < 0 || bel < st) -> bel + 1
                    st >= 0 -> st + 2
                    else -> -1
                }
            }
            'P', 'X', '^', '_' -> {
                val st = seq.indexOf(ST)
                if (st >= 0) st + 2 else -1
            }
            '(', ')' -> if (seq.length >= 3) 3 else -1
            in '@'..'_' -> 2
            else -> 2
        }
    }

    private fun applyCsi(params: String, final: Char) {
        val nums = if (params.isBlank()) {
            emptyList()
        } else {
            params.split(';').map { it.toIntOrNull() ?: 0 }
        }
        fun p(i: Int, def: Int = 1) = nums.getOrNull(i)?.takeIf { it > 0 } ?: def
        fun p0(i: Int, def: Int = 0) = nums.getOrNull(i) ?: def

        when (final) {
            'm' -> insertRawAtCursor("$ESC[${params}m")
            'A' -> row = (row - p(0)).coerceAtLeast(0)
            'B' -> {
                repeat(p(0)) {
                    if (row >= lines.lastIndex) newLine() else row++
                }
                col = col.coerceAtMost(visibleLength(lines[row]))
            }
            'C' -> col += p(0)
            'D' -> col = (col - p(0)).coerceAtLeast(0)
            'G' -> col = (p(0) - 1).coerceAtLeast(0)
            'H', 'f' -> {
                val r = (p(0) - 1).coerceAtLeast(0)
                val c = (p0(1, 1) - 1).coerceAtLeast(0)
                ensureRow(r)
                row = r
                col = c
            }
            'J' -> when (p0(0, 0)) {
                0 -> {
                    eraseLineToEnd()
                    for (r in (row + 1)..lines.lastIndex) lines[r] = StringBuilder()
                }
                1 -> {
                    eraseLineToStart()
                    for (r in 0 until row) lines[r] = StringBuilder()
                }
                2, 3 -> {
                    lines.clear()
                    lines += StringBuilder()
                    row = 0
                    col = 0
                }
            }
            'K' -> when (p0(0, 0)) {
                0 -> eraseLineToEnd()
                1 -> eraseLineToStart()
                2 -> {
                    lines[row] = StringBuilder()
                    // keep col; many apps reset separately
                }
            }
            'P' -> deleteChars(p(0))
            'X' -> eraseChars(p(0))
            '@' -> insertSpaces(p(0))
            'd' -> {
                val r = (p(0) - 1).coerceAtLeast(0)
                ensureRow(r)
                row = r
            }
            else -> Unit
        }
    }

    private fun putPrintable(ch: Char) {
        ensureCol()
        val sb = lines[row]
        val idx = indexOfVisibleCol(sb, col)
        if (idx >= sb.length) {
            sb.append(ch)
        } else if (isSgrStart(sb, idx)) {
            val after = skipSgr(sb, idx)
            if (after >= sb.length) sb.append(ch) else sb.setCharAt(after, ch)
        } else {
            sb.setCharAt(idx, ch)
        }
        col++
    }

    private fun deleteChars(n: Int) {
        if (n <= 0) return
        val sb = lines[row]
        var removed = 0
        while (removed < n) {
            val idx = indexOfVisibleCol(sb, col)
            if (idx >= sb.length) break
            if (isSgrStart(sb, idx)) {
                val after = skipSgr(sb, idx)
                if (after >= sb.length) break
                sb.deleteCharAt(after)
            } else {
                sb.deleteCharAt(idx)
            }
            removed++
        }
    }

    private fun eraseChars(n: Int) {
        val save = col
        repeat(n) {
            ensureCol()
            val sb = lines[row]
            val idx = indexOfVisibleCol(sb, col)
            if (idx >= sb.length) {
                sb.append(' ')
            } else if (isSgrStart(sb, idx)) {
                val after = skipSgr(sb, idx)
                if (after >= sb.length) sb.append(' ') else sb.setCharAt(after, ' ')
            } else {
                sb.setCharAt(idx, ' ')
            }
            col++
        }
        col = save
    }

    private fun insertSpaces(n: Int) {
        val sb = lines[row]
        val idx = indexOfVisibleCol(sb, col).coerceAtMost(sb.length)
        repeat(n) { sb.insert(idx, ' ') }
    }

    private fun eraseLineToEnd() {
        val sb = lines[row]
        val idx = indexOfVisibleCol(sb, col).coerceAtMost(sb.length)
        if (idx < sb.length) sb.delete(idx, sb.length)
    }

    private fun eraseLineToStart() {
        val sb = lines[row]
        val idx = indexOfVisibleCol(sb, col).coerceAtMost(sb.length)
        val tail = sb.substring(idx)
        val prefixSgr = extractLeadingSgr(sb, idx)
        lines[row] = StringBuilder(prefixSgr).append(tail)
    }

    private fun extractLeadingSgr(sb: StringBuilder, before: Int): String {
        val out = StringBuilder()
        var i = 0
        while (i < before) {
            if (isSgrStart(sb, i)) {
                val end = skipSgr(sb, i)
                out.append(sb, i, end)
                i = end
            } else {
                i++
            }
        }
        return out.toString()
    }

    private fun newLine() {
        if (row >= lines.lastIndex) {
            lines += StringBuilder()
            row = lines.lastIndex
        } else {
            row++
        }
        col = 0
    }

    private fun ensureRow(r: Int) {
        while (lines.size <= r) lines += StringBuilder()
    }

    private fun ensureCol() {
        val sb = lines[row]
        val vis = visibleLength(sb)
        if (col > vis) {
            repeat(col - vis) { sb.append(' ') }
        }
    }

    private fun visibleLength(sb: CharSequence): Int {
        var n = 0
        var i = 0
        while (i < sb.length) {
            if (isSgrStart(sb, i)) i = skipSgr(sb, i) else {
                n++
                i++
            }
        }
        return n
    }

    private fun indexOfVisibleCol(sb: CharSequence, targetCol: Int): Int {
        var n = 0
        var i = 0
        while (i < sb.length) {
            if (isSgrStart(sb, i)) {
                i = skipSgr(sb, i)
            } else {
                if (n == targetCol) return i
                n++
                i++
            }
        }
        return sb.length
    }

    private fun isSgrStart(sb: CharSequence, i: Int): Boolean =
        i + 1 < sb.length && sb[i] == ESC && sb[i + 1] == '['

    private fun skipSgr(sb: CharSequence, i: Int): Int {
        var j = i + 2
        while (j < sb.length) {
            if (sb[j] == 'm') return j + 1
            if (sb[j] in '@'..'~') return j + 1
            j++
        }
        return sb.length
    }

    private fun insertRawAtCursor(raw: String) {
        val sb = lines[row]
        val idx = indexOfVisibleCol(sb, col).coerceAtMost(sb.length)
        sb.insert(idx, raw)
    }

    private fun trimSize() {
        var total = lines.sumOf { it.length } + lines.size
        while (total > maxChars && lines.size > 1) {
            total -= lines.removeAt(0).length + 1
            row = (row - 1).coerceAtLeast(0)
        }
        if (total > maxChars && lines.isNotEmpty()) {
            val sb = lines[0]
            val drop = total - maxChars
            if (drop > 0 && drop < sb.length) sb.delete(0, drop)
        }
    }

    companion object {
        private const val ESC = '\u001B'
        private const val BEL = '\u0007'
        private const val BS = '\u0008'
        private const val DEL = '\u007F'
        private const val ST = "\u001B\\"
    }
}
