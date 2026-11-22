package com.prokey.computerkeyboard

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

/** Thin wrapper over the app's default SharedPreferences. Defaults match res/xml/preferences.xml. */
class Prefs(context: Context) {
    val sp: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    var layoutMode: String
        get() = sp.getString(LAYOUT, "mobile") ?: "mobile"
        set(v) = sp.edit().putString(LAYOUT, v).apply()

    val pc: Boolean get() = layoutMode == "pc"
    val numberRow: Boolean get() = sp.getBoolean(NUMBER_ROW, true)
    var functionRow: Boolean
        get() = sp.getBoolean(FUNCTION_ROW, true)
        set(v) = sp.edit().putBoolean(FUNCTION_ROW, v).apply()
    val numpad: Boolean get() = sp.getBoolean(NUMPAD, false)
    val autoCap: Boolean get() = sp.getBoolean(AUTO_CAP, true)
    val doubleSpace: Boolean get() = sp.getBoolean(DOUBLE_SPACE, true)
    val theme: String get() = sp.getString(THEME, "classic_black") ?: "classic_black"
    val heightPct: Int get() = sp.getInt(HEIGHT, 100)
    val gap: Int get() = sp.getInt(GAP, 3)
    val radius: Int get() = sp.getInt(RADIUS, 8)
    val soundOn: Boolean get() = sp.getBoolean(SOUND_ON, false)
    val soundVolume: Int get() = sp.getInt(SOUND_VOLUME, 50)
    val vibrateOn: Boolean get() = sp.getBoolean(VIBRATE_ON, true)
    val vibrateLevel: Int get() = sp.getInt(VIBRATE_LEVEL, 50)

    companion object {
        const val LAYOUT = "layout_mode"
        const val NUMBER_ROW = "number_row"
        const val FUNCTION_ROW = "function_row"
        const val NUMPAD = "numeric_keypad"
        const val AUTO_CAP = "auto_cap"
        const val DOUBLE_SPACE = "double_space"
        const val THEME = "theme"
        const val HEIGHT = "key_height"
        const val GAP = "key_gap"
        const val RADIUS = "key_radius"
        const val SOUND_ON = "sound_on"
        const val SOUND_VOLUME = "sound_volume"
        const val VIBRATE_ON = "vibrate_on"
        const val VIBRATE_LEVEL = "vibrate_level"
    }
}
