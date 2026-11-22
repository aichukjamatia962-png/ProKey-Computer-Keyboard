package com.prokey.computerkeyboard

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.text.InputType
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.preference.PreferenceManager
import kotlin.math.abs

/**
 * The real Android keyboard (InputMethodService). All text goes through InputConnection;
 * nothing the user types is stored, logged or transmitted.
 */
class ProKeyService : InputMethodService(), KeyboardView.Listener,
    SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var prefs: Prefs
    private lateinit var audio: AudioManager
    private var vibrator: Vibrator? = null

    private val state = KbState()
    private var keyboard: KeyboardView? = null
    private var editorInfo: EditorInfo? = null
    private var symbols = false
    private var symbolPage = 0
    private var lastSpaceTime = 0L

    // cached feedback / typing prefs (read on every key press)
    private var soundOn = false
    private var soundVol = 0.5f
    private var vibOn = true
    private var vibLevel = 50
    private var autoCap = true
    private var doubleSpace = true

    // ---- lifecycle --------------------------------------------------------------------------

    override fun onCreate() {
        super.onCreate()
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)
        prefs = Prefs(this)
        audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        vibrator = findVibrator()
        loadCachedPrefs()
        prefs.sp.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onDestroy() {
        prefs.sp.unregisterOnSharedPreferenceChangeListener(this)
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    private fun findVibrator(): Vibrator? =
        if (Build.VERSION.SDK_INT >= 31) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

    private fun loadCachedPrefs() {
        soundOn = prefs.soundOn
        soundVol = prefs.soundVolume / 100f
        vibOn = prefs.vibrateOn
        vibLevel = prefs.vibrateLevel
        autoCap = prefs.autoCap
        doubleSpace = prefs.doubleSpace
    }

    override fun onSharedPreferenceChanged(sp: SharedPreferences?, key: String?) {
        loadCachedPrefs()
        rebuild()
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onCreateInputView(): View {
        val v = KeyboardView(this)
        v.listener = this
        v.state = state
        keyboard = v
        rebuild()
        return v
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        rebuild()
    }

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        editorInfo = info
        if (!restarting) {
            state.resetAll()
            val cls = info.inputType and InputType.TYPE_MASK_CLASS
            symbols = !prefs.pc &&
                (cls == InputType.TYPE_CLASS_NUMBER || cls == InputType.TYPE_CLASS_PHONE)
            symbolPage = 0
            lastSpaceTime = 0L
        }
        rebuild()
        updateAutoCap()
        keyboard?.invalidate()
    }

    override fun onUpdateSelection(
        oldSelStart: Int, oldSelEnd: Int, newSelStart: Int, newSelEnd: Int,
        candidatesStart: Int, candidatesEnd: Int
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        updateAutoCap()
        keyboard?.invalidate()
    }

    private fun rebuild() {
        val v = keyboard ?: return
        val landscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val pc = prefs.pc
        state.fnRow = prefs.functionRow
        if (!pc) state.caps = false
        val rows = when {
            pc -> Layouts.pc(state.fnRow, landscape, landscape && prefs.numpad)
            symbols -> Layouts.symbols(symbolPage)
            else -> Layouts.mobile(prefs.numberRow)
        }
        val baseDp = if (pc) (if (landscape) 34f else 40f) else (if (landscape) 38f else 46f)
        v.setup(
            rows,
            Themes.byId(prefs.theme),
            KeyboardView.Look(baseDp * prefs.heightPct / 100f, prefs.gap.toFloat(), prefs.radius.toFloat())
        )
    }

    private fun updateAutoCap() {
        if (prefs.pc || !autoCap || state.shift.state != Mod.OFF) return
        val ei = editorInfo ?: return
        val ic = currentInputConnection ?: return
        if (ic.getCursorCapsMode(ei.inputType) != 0) state.shift.state = Mod.ONCE
    }

    // ---- feedback ---------------------------------------------------------------------------

    override fun onPress(key: Key) {
        if (vibOn) vibrate()
        if (soundOn && soundVol > 0f) {
            val fx = when (key.type) {
                KeyType.SPACE -> AudioManager.FX_KEYPRESS_SPACEBAR
                KeyType.BACKSPACE -> AudioManager.FX_KEYPRESS_DELETE
                KeyType.ENTER -> AudioManager.FX_KEYPRESS_RETURN
                else -> AudioManager.FX_KEYPRESS_STANDARD
            }
            audio.playSoundEffect(fx, soundVol)
        }
    }

    private fun vibrate() {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        val effect = if (v.hasAmplitudeControl()) {
            VibrationEffect.createOneShot(14L, (vibLevel * 255 / 100).coerceIn(1, 255))
        } else {
            VibrationEffect.createOneShot((6 + vibLevel / 3).toLong(), VibrationEffect.DEFAULT_AMPLITUDE)
        }
        v.vibrate(effect)
    }

    // ---- key handling -----------------------------------------------------------------------

    override fun onKey(key: Key) {
        when (key.type) {
            KeyType.CHAR -> typeChar(key)
            KeyType.SPACE -> typeSpace()
            KeyType.ENTER -> typeEnter()
            KeyType.BACKSPACE -> sendCode(KeyEvent.KEYCODE_DEL, prefs.pc)
            KeyType.CODE -> {
                if (key.keyCode == KeyEvent.KEYCODE_SYSRQ) {
                    Toast.makeText(this, "Print Screen: use your phone screenshot shortcut", Toast.LENGTH_SHORT).show()
                }
                sendCode(key.keyCode, true)
            }
            KeyType.SHIFT -> state.shift.tap()
            KeyType.CAPS -> state.caps = !state.caps
            KeyType.CTRL -> state.ctrl.tap()
            KeyType.ALT -> state.alt.tap()
            KeyType.META -> state.meta.tap()
            KeyType.FN -> prefs.functionRow = !prefs.functionRow
            KeyType.SYMBOLS -> { symbols = !symbols; symbolPage = 0; rebuild() }
            KeyType.SYMBOL_PAGE -> { symbolPage = 1 - symbolPage; rebuild() }
            KeyType.MODE -> { symbols = false; prefs.layoutMode = if (prefs.pc) "mobile" else "pc" }
            KeyType.GLOBE -> switchKeyboard()
            KeyType.SETTINGS -> openSettings()
            KeyType.SPACER -> Unit
        }
        keyboard?.invalidate()
    }

    override fun onSpaceSwipe(steps: Int) {
        val code = if (steps < 0) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT
        repeat(abs(steps)) { sendCode(code, false) }
    }

    private fun outputText(k: Key): String {
        val st = k.shiftText ?: return k.text
        val upper = if (k.isLetter) state.shift.on != state.caps else state.shift.on
        return if (upper) st else k.text
    }

    private fun typeChar(key: Key) {
        val ic = currentInputConnection ?: return
        if (prefs.pc && state.anyCtrlAltMeta() && key.keyCode != KeyEvent.KEYCODE_UNKNOWN) {
            val handled = state.ctrl.on && !state.alt.on && !state.meta.on && ctrlShortcut(ic, key.keyCode)
            if (handled) state.consumeMods() else sendCode(key.keyCode, true)
            return
        }
        ic.commitText(outputText(key), 1)
        state.shift.consume()
    }

    /** Ctrl+A/C/X/V go straight to the editor; everything else is sent as a Ctrl key event. */
    private fun ctrlShortcut(ic: InputConnection, code: Int): Boolean = when (code) {
        KeyEvent.KEYCODE_A -> ic.performContextMenuAction(android.R.id.selectAll)
        KeyEvent.KEYCODE_C -> ic.performContextMenuAction(android.R.id.copy)
        KeyEvent.KEYCODE_X -> ic.performContextMenuAction(android.R.id.cut)
        KeyEvent.KEYCODE_V -> ic.performContextMenuAction(android.R.id.paste)
        else -> false
    }

    private fun typeSpace() {
        val ic = currentInputConnection ?: return
        if (prefs.pc && state.anyCtrlAltMeta()) {
            sendCode(KeyEvent.KEYCODE_SPACE, true)
            return
        }
        val now = SystemClock.uptimeMillis()
        if (!prefs.pc && doubleSpace && now - lastSpaceTime < 500L) {
            val before = ic.getTextBeforeCursor(2, 0)
            if (before != null && before.length == 2 && before[1] == ' ' && before[0].isLetterOrDigit()) {
                ic.beginBatchEdit()
                ic.deleteSurroundingText(1, 0)
                ic.commitText(". ", 1)
                ic.endBatchEdit()
                lastSpaceTime = 0L
                return
            }
        }
        ic.commitText(" ", 1)
        lastSpaceTime = now
        state.shift.consume()
    }

    private fun typeEnter() {
        val ic = currentInputConnection ?: return
        if (prefs.pc && state.anyCtrlAltMeta()) {
            sendCode(KeyEvent.KEYCODE_ENTER, true)
            return
        }
        val opts = editorInfo?.imeOptions ?: 0
        val action = opts and EditorInfo.IME_MASK_ACTION
        val noEnterAction = (opts and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0
        if (!noEnterAction && action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
            ic.performEditorAction(action)
        } else {
            sendCode(KeyEvent.KEYCODE_ENTER, false)
        }
    }

    /** Sends a down+up KeyEvent; with [withMods] the active Shift/Ctrl/Alt/Win state is attached and consumed. */
    private fun sendCode(code: Int, withMods: Boolean) {
        val ic = currentInputConnection ?: return
        val meta = if (withMods) state.metaState() else 0
        val t = SystemClock.uptimeMillis()
        val flags = KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE
        ic.sendKeyEvent(KeyEvent(t, t, KeyEvent.ACTION_DOWN, code, 0, meta, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, flags))
        ic.sendKeyEvent(KeyEvent(t, t, KeyEvent.ACTION_UP, code, 0, meta, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, flags))
        if (withMods) state.consumeMods()
    }

    private fun switchKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        @Suppress("DEPRECATION")
        val switched = if (Build.VERSION.SDK_INT >= 28) {
            switchToNextInputMethod(false)
        } else {
            imm.switchToNextInputMethod(window.window?.attributes?.token, false)
        }
        if (!switched) imm.showInputMethodPicker()
    }

    private fun openSettings() {
        val i = Intent(this, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(i)
        requestHideSelf(0)
    }
}
