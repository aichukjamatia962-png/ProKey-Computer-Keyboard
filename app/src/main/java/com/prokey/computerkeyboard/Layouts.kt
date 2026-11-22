package com.prokey.computerkeyboard

import android.graphics.RectF
import android.view.KeyEvent as KE

enum class KeyType {
    CHAR, SPACE, ENTER, BACKSPACE, SHIFT, CAPS, CTRL, ALT, META, FN,
    CODE, SYMBOLS, SYMBOL_PAGE, MODE, GLOBE, SETTINGS, SPACER
}

enum class Icon { NONE, SHIFT, BACKSPACE, ENTER }

class Key(
    val type: KeyType,
    val label: String = "",
    val weight: Float = 1f,
    val text: String = "",
    val shiftText: String? = null,
    val keyCode: Int = KE.KEYCODE_UNKNOWN,
    val icon: Icon = Icon.NONE,
    val repeat: Boolean = false,
) {
    /** Filled in by KeyboardView when the layout is measured. */
    val rect = RectF()
    val isLetter: Boolean get() = type == KeyType.CHAR && text.length == 1 && text[0].isLetter()
}

/** Pure layout definitions. Every call returns fresh Key objects (they carry their own screen rect). */
object Layouts {

    // ---- builders ---------------------------------------------------------------------------

    private fun ch(t: String, shift: String? = null, code: Int = KE.KEYCODE_UNKNOWN, w: Float = 1f) =
        Key(KeyType.CHAR, t, w, t, shift, code)

    private fun letter(c: Char, w: Float = 1f) =
        ch(c.toString(), c.uppercaseChar().toString(), KE.KEYCODE_A + (c - 'a'), w)

    private fun letters(s: String) = s.map { letter(it) }

    private fun code(label: String, keyCode: Int, w: Float = 1f, repeat: Boolean = false) =
        Key(KeyType.CODE, label, w, keyCode = keyCode, repeat = repeat)

    private fun spacer(w: Float) = Key(KeyType.SPACER, weight = w)

    private fun special(type: KeyType, label: String = "", w: Float = 1f, icon: Icon = Icon.NONE, repeat: Boolean = false) =
        Key(type, label, w, icon = icon, repeat = repeat)

    private fun space(w: Float) = Key(KeyType.SPACE, "", w, " ", null, KE.KEYCODE_SPACE)

    private fun digits() = "1234567890".map { ch(it.toString(), null, KE.KEYCODE_0 + (it - '0')) }

    // ---- mobile -----------------------------------------------------------------------------

    private fun mobileBottom(symLabel: String) = listOf(
        special(KeyType.SYMBOLS, symLabel, 1.2f),
        special(KeyType.GLOBE, "\uD83C\uDF10", 0.9f),
        special(KeyType.SETTINGS, "\u2699", 0.9f),
        ch(",", null, KE.KEYCODE_COMMA, 0.8f),
        space(3.0f),
        ch(".", null, KE.KEYCODE_PERIOD, 0.8f),
        special(KeyType.MODE, "PC", 1.0f),
        special(KeyType.ENTER, "", 1.4f, Icon.ENTER),
    )

    fun mobile(numberRow: Boolean): List<List<Key>> {
        val rows = ArrayList<List<Key>>()
        if (numberRow) rows.add(digits())
        rows.add(letters("qwertyuiop"))
        rows.add(listOf(spacer(0.5f)) + letters("asdfghjkl") + spacer(0.5f))
        rows.add(
            listOf(special(KeyType.SHIFT, "", 1.5f, Icon.SHIFT)) + letters("zxcvbnm") +
                special(KeyType.BACKSPACE, "", 1.5f, Icon.BACKSPACE, true)
        )
        rows.add(mobileBottom("?123"))
        return rows
    }

    fun symbols(page: Int): List<List<Key>> {
        val r2 = if (page == 0) listOf("@", "#", "\$", "_", "&", "-", "+", "(", ")", "/")
        else listOf("[", "]", "{", "}", "<", ">", "=", "^", "\\", "|")
        val r3 = if (page == 0) listOf("*", "\"", "'", ":", ";", "!", "?")
        else listOf("~", "`", "\u00A3", "\u20AC", "\u00A5", "\u00B0", "%")
        return listOf(
            digits(),
            r2.map { ch(it) },
            listOf(special(KeyType.SYMBOL_PAGE, if (page == 0) "1/2" else "2/2", 1.5f)) +
                r3.map { ch(it) } +
                special(KeyType.BACKSPACE, "", 1.5f, Icon.BACKSPACE, true),
            mobileBottom("ABC"),
        )
    }

    // ---- PC ---------------------------------------------------------------------------------

    private fun pair(t: String, s: String, code: Int, w: Float = 1f) = ch(t, s, code, w)

    private fun fRow() = (1..12).map { code("F$it", KE.KEYCODE_F1 + it - 1) }

    private fun navRow() = listOf(
        code("Esc", KE.KEYCODE_ESCAPE),
        code("Ins", KE.KEYCODE_INSERT),
        code("Del", KE.KEYCODE_FORWARD_DEL, repeat = true),
        code("Home", KE.KEYCODE_MOVE_HOME),
        code("End", KE.KEYCODE_MOVE_END),
        code("PgUp", KE.KEYCODE_PAGE_UP, repeat = true),
        code("PgDn", KE.KEYCODE_PAGE_DOWN, repeat = true),
        code("PrtSc", KE.KEYCODE_SYSRQ),
        code("Menu", KE.KEYCODE_MENU),
    )

    private fun pcNumberRow(): List<Key> {
        val shifted = "!@#\$%^&*()"
        return listOf(pair("`", "~", KE.KEYCODE_GRAVE)) +
            "1234567890".mapIndexed { i, d -> pair(d.toString(), shifted[i].toString(), KE.KEYCODE_0 + (d - '0')) } +
            pair("-", "_", KE.KEYCODE_MINUS) +
            pair("=", "+", KE.KEYCODE_EQUALS) +
            special(KeyType.BACKSPACE, "", 2f, Icon.BACKSPACE, true)
    }

    private fun pcQRow() = listOf(code("Tab", KE.KEYCODE_TAB, 1.5f)) + letters("qwertyuiop") +
        pair("[", "{", KE.KEYCODE_LEFT_BRACKET) +
        pair("]", "}", KE.KEYCODE_RIGHT_BRACKET) +
        pair("\\", "|", KE.KEYCODE_BACKSLASH, 1.5f)

    private fun pcARow() = listOf(special(KeyType.CAPS, "Caps", 1.8f)) + letters("asdfghjkl") +
        pair(";", ":", KE.KEYCODE_SEMICOLON) +
        pair("'", "\"", KE.KEYCODE_APOSTROPHE) +
        special(KeyType.ENTER, "Enter", 2.2f)

    private fun pcZRow() = listOf(special(KeyType.SHIFT, "Shift", 2.3f)) + letters("zxcvbnm") +
        pair(",", "<", KE.KEYCODE_COMMA) +
        pair(".", ">", KE.KEYCODE_PERIOD) +
        pair("/", "?", KE.KEYCODE_SLASH) +
        special(KeyType.SHIFT, "Shift", 2.7f)

    private fun bottomCompact() = listOf(
        special(KeyType.MODE, "ABC", 1.4f),
        special(KeyType.CTRL, "Ctrl", 1.4f),
        special(KeyType.FN, "Fn", 1.0f),
        special(KeyType.META, "Win", 1.1f),
        special(KeyType.ALT, "Alt", 1.1f),
        space(4.2f),
        code("\u2190", KE.KEYCODE_DPAD_LEFT, 1.2f, true),
        code("\u2193", KE.KEYCODE_DPAD_DOWN, 1.2f, true),
        code("\u2191", KE.KEYCODE_DPAD_UP, 1.2f, true),
        code("\u2192", KE.KEYCODE_DPAD_RIGHT, 1.2f, true),
    )

    private fun bottomFull() = listOf(
        special(KeyType.MODE, "ABC", 1.4f),
        special(KeyType.CTRL, "Ctrl", 1.4f),
        special(KeyType.FN, "Fn", 1.2f),
        special(KeyType.META, "Win", 1.2f),
        special(KeyType.ALT, "Alt", 1.2f),
        space(4.8f),
        special(KeyType.ALT, "Alt", 1.2f),
        code("Menu", KE.KEYCODE_MENU, 1.2f),
        special(KeyType.CTRL, "Ctrl", 1.4f),
    )

    private fun cluster() = listOf(
        listOf(code("PrtSc", KE.KEYCODE_SYSRQ), spacer(1f), spacer(1f)),
        listOf(code("Ins", KE.KEYCODE_INSERT), code("Home", KE.KEYCODE_MOVE_HOME), code("PgUp", KE.KEYCODE_PAGE_UP, repeat = true)),
        listOf(code("Del", KE.KEYCODE_FORWARD_DEL, repeat = true), code("End", KE.KEYCODE_MOVE_END), code("PgDn", KE.KEYCODE_PAGE_DOWN, repeat = true)),
        listOf(spacer(3f)),
        listOf(spacer(1f), code("\u2191", KE.KEYCODE_DPAD_UP, repeat = true), spacer(1f)),
        listOf(
            code("\u2190", KE.KEYCODE_DPAD_LEFT, repeat = true),
            code("\u2193", KE.KEYCODE_DPAD_DOWN, repeat = true),
            code("\u2192", KE.KEYCODE_DPAD_RIGHT, repeat = true),
        ),
    )

    private fun numpadRows(): List<List<Key>> {
        fun n(t: String, kc: Int, w: Float = 1f) = ch(t, null, kc, w)
        fun d(c: Char) = n(c.toString(), KE.KEYCODE_NUMPAD_0 + (c - '0'))
        return listOf(
            listOf(spacer(4f)),
            listOf(n("/", KE.KEYCODE_NUMPAD_DIVIDE), n("*", KE.KEYCODE_NUMPAD_MULTIPLY), n("-", KE.KEYCODE_NUMPAD_SUBTRACT), n("+", KE.KEYCODE_NUMPAD_ADD)),
            listOf(d('7'), d('8'), d('9'), special(KeyType.BACKSPACE, "", 1f, Icon.BACKSPACE, true)),
            listOf(d('4'), d('5'), d('6'), n("=", KE.KEYCODE_NUMPAD_EQUALS)),
            listOf(d('1'), d('2'), d('3'), n(",", KE.KEYCODE_NUMPAD_COMMA)),
            listOf(n("0", KE.KEYCODE_NUMPAD_0, 2f), n(".", KE.KEYCODE_NUMPAD_DOT), special(KeyType.ENTER, "", 1f, Icon.ENTER)),
        )
    }

    /**
     * PC layout.
     * compact (portrait): optional F1-F12 row, Esc/nav row, main block, bottom row with arrows.
     * full (landscape): Esc+F row, main block, navigation cluster (+ optional numeric keypad).
     */
    fun pc(fnRow: Boolean, full: Boolean, numpad: Boolean): List<List<Key>> {
        if (!full) {
            val rows = ArrayList<List<Key>>()
            if (fnRow) rows.add(fRow())
            rows.add(navRow())
            rows.add(pcNumberRow())
            rows.add(pcQRow())
            rows.add(pcARow())
            rows.add(pcZRow())
            rows.add(bottomCompact())
            return rows
        }
        val top = listOf(code("Esc", KE.KEYCODE_ESCAPE)) +
            (if (fnRow) fRow() else listOf(spacer(12f))) + spacer(2f)
        val main = listOf(top, pcNumberRow(), pcQRow(), pcARow(), pcZRow(), bottomFull())
        val cl = cluster()
        val pad = numpadRows()
        return main.indices.map { i ->
            var row = main[i] + spacer(0.5f) + cl[i]
            if (numpad) row = row + spacer(0.5f) + pad[i]
            row
        }
    }
}
