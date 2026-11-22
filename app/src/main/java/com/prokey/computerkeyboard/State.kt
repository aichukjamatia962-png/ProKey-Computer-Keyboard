package com.prokey.computerkeyboard

import android.os.SystemClock
import android.view.KeyEvent

enum class Mod { OFF, ONCE, LOCKED }

/** A sticky modifier: tap = one-shot, double-tap = locked, tap again = off. */
class ModKey {
    var state = Mod.OFF
    private var lastTap = 0L

    val on: Boolean get() = state != Mod.OFF

    fun tap() {
        val now = SystemClock.uptimeMillis()
        state = when {
            state == Mod.LOCKED -> Mod.OFF
            state == Mod.ONCE && now - lastTap < DOUBLE_TAP_MS -> Mod.LOCKED
            state == Mod.ONCE -> Mod.OFF
            else -> Mod.ONCE
        }
        lastTap = now
    }

    fun consume() {
        if (state == Mod.ONCE) state = Mod.OFF
    }

    fun reset() {
        state = Mod.OFF
    }

    private companion object {
        const val DOUBLE_TAP_MS = 350L
    }
}

/** Keyboard state shared between the service (logic) and the view (drawing). */
class KbState {
    val shift = ModKey()
    val ctrl = ModKey()
    val alt = ModKey()
    val meta = ModKey()
    var caps = false
    var fnRow = true

    fun anyCtrlAltMeta() = ctrl.on || alt.on || meta.on

    fun consumeMods() {
        shift.consume(); ctrl.consume(); alt.consume(); meta.consume()
    }

    fun resetAll() {
        shift.reset(); ctrl.reset(); alt.reset(); meta.reset(); caps = false
    }

    fun metaState(): Int {
        var m = 0
        if (shift.on) m = m or KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON
        if (ctrl.on) m = m or KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
        if (alt.on) m = m or KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON
        if (meta.on) m = m or KeyEvent.META_META_ON or KeyEvent.META_META_LEFT_ON
        return m
    }
}
