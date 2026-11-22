package com.prokey.computerkeyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.os.Handler
import android.os.Looper
import android.util.SparseArray
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.ColorUtils
import kotlin.math.abs
import kotlin.math.min

/**
 * Canvas-drawn keyboard. Supports multi-touch (fast two-thumb typing), key repeat,
 * slide-to-correct and spacebar-swipe cursor control. All logic lives in [Listener].
 */
class KeyboardView(context: Context) : View(context) {

    interface Listener {
        fun onPress(key: Key)
        fun onKey(key: Key)
        fun onSpaceSwipe(steps: Int)
    }

    data class Look(val rowHeightDp: Float, val gapDp: Float, val radiusDp: Float)

    var listener: Listener? = null
    var state = KbState()

    private var rows: List<List<Key>> = emptyList()
    private var theme: KbTheme = Themes.all[0]
    private var look = Look(46f, 3f, 8f)
    private var accentFg = Color.WHITE
    private var bgShader: Shader? = null

    private val dp = resources.displayMetrics.density
    private val uiHandler = Handler(Looper.getMainLooper())
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 2f * dp
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val tmp = RectF()
    private val path = Path()

    // touch tracking
    private val pressed = SparseArray<Key>()
    private var repeatPointer = -1
    private var repeatKey: Key? = null
    private var repeatFired = false
    private var spacePointer = -1
    private var spaceStartX = 0f
    private var spaceSteppedX = 0f
    private var swiping = false

    private val repeatRunnable = object : Runnable {
        override fun run() {
            val k = repeatKey ?: return
            repeatFired = true
            listener?.onKey(k)
            uiHandler.postDelayed(this, REPEAT_INTERVAL_MS)
        }
    }

    fun setup(rows: List<List<Key>>, theme: KbTheme, look: Look) {
        stopAll()
        this.rows = rows
        this.theme = theme
        this.look = look
        accentFg = if (ColorUtils.calculateLuminance(theme.accent) > 0.55) Color.BLACK else Color.WHITE
        bgShader = null
        requestLayout()
        if (width > 0) layoutKeys()
        invalidate()
    }

    // ---- layout -----------------------------------------------------------------------------

    private fun topPad() = 4f * dp
    private fun rowHeight() = look.rowHeightDp * dp

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val h = (rows.size * rowHeight() + 2 * topPad()).toInt()
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), h)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        bgShader = null
        layoutKeys()
    }

    private fun layoutKeys() {
        val padX = 2f * dp
        val usable = width - 2 * padX
        val rowH = rowHeight()
        var y = topPad()
        for (row in rows) {
            var total = 0f
            for (k in row) total += k.weight
            var x = padX
            for (k in row) {
                val kw = usable * k.weight / total
                k.rect.set(x, y, x + kw, y + rowH)
                x += kw
            }
            y += rowH
        }
    }

    // ---- drawing ----------------------------------------------------------------------------

    override fun onDraw(canvas: Canvas) {
        val end = theme.bgEnd
        if (end != null) {
            val sh = bgShader ?: LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(), theme.bg, end, Shader.TileMode.CLAMP
            ).also { bgShader = it }
            fillPaint.shader = sh
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), fillPaint)
            fillPaint.shader = null
        } else {
            canvas.drawColor(theme.bg)
        }
        val inset = look.gapDp * dp / 2f
        val radius = look.radiusDp * dp
        for (row in rows) for (k in row) {
            if (k.type == KeyType.SPACER) continue
            tmp.set(k.rect)
            tmp.inset(inset, inset)
            drawKey(canvas, k, tmp, radius)
        }
    }

    private fun isHeld(k: Key): Boolean {
        for (i in 0 until pressed.size()) if (pressed.valueAt(i) === k) return true
        return false
    }

    private fun isActive(k: Key): Boolean = when (k.type) {
        KeyType.SHIFT -> state.shift.on
        KeyType.CAPS -> state.caps
        KeyType.CTRL -> state.ctrl.on
        KeyType.ALT -> state.alt.on
        KeyType.META -> state.meta.on
        KeyType.FN -> state.fnRow
        else -> false
    }

    private fun labelFor(k: Key): String {
        if (k.type != KeyType.CHAR) return k.label
        val st = k.shiftText ?: return k.label
        val upper = if (k.isLetter) state.shift.on != state.caps else state.shift.on
        return if (upper) st else k.label
    }

    private fun drawKey(c: Canvas, k: Key, r: RectF, radius: Float) {
        val held = isHeld(k)
        val accented = isActive(k) || k.type == KeyType.ENTER
        val fill = when {
            held -> theme.keyPressed
            accented -> theme.accent
            k.type == KeyType.CHAR || k.type == KeyType.SPACE -> theme.key
            else -> theme.keySpecial
        }
        fillPaint.color = fill
        c.drawRoundRect(r, radius, radius, fillPaint)
        if (theme.border != 0) {
            strokePaint.color = theme.border
            c.drawRoundRect(r, radius, radius, strokePaint)
        }
        val fg = if (accented && !held) accentFg else theme.text
        if (k.icon == Icon.NONE) {
            drawLabel(c, labelFor(k), r, fg)
        } else {
            drawIcon(c, k, r.centerX(), r.centerY(), min(r.height(), r.width()) * 0.22f, fg)
        }
    }

    private fun drawLabel(c: Canvas, text: String, r: RectF, color: Int) {
        if (text.isEmpty()) return
        val base = r.height() * (if (text.length == 1) 0.42f else 0.34f)
        textPaint.textSize = base
        val maxW = r.width() * 0.88f
        val tw = textPaint.measureText(text)
        if (tw > maxW) textPaint.textSize = base * maxW / tw
        textPaint.color = color
        val fm = textPaint.fontMetrics
        c.drawText(text, r.centerX(), r.centerY() - (fm.ascent + fm.descent) / 2f, textPaint)
    }

    private fun drawIcon(c: Canvas, k: Key, cx: Float, cy: Float, s: Float, color: Int) {
        iconPaint.color = color
        iconPaint.style = Paint.Style.STROKE
        val p = path
        p.reset()
        when (k.icon) {
            Icon.SHIFT -> {
                p.moveTo(cx, cy - s); p.lineTo(cx + s, cy); p.lineTo(cx + s * 0.4f, cy)
                p.lineTo(cx + s * 0.4f, cy + s); p.lineTo(cx - s * 0.4f, cy + s)
                p.lineTo(cx - s * 0.4f, cy); p.lineTo(cx - s, cy); p.close()
                if (state.shift.state == Mod.LOCKED) iconPaint.style = Paint.Style.FILL_AND_STROKE
            }
            Icon.BACKSPACE -> {
                p.moveTo(cx - s * 1.3f, cy); p.lineTo(cx - s * 0.5f, cy - s * 0.9f)
                p.lineTo(cx + s * 1.2f, cy - s * 0.9f); p.lineTo(cx + s * 1.2f, cy + s * 0.9f)
                p.lineTo(cx - s * 0.5f, cy + s * 0.9f); p.close()
                p.moveTo(cx - s * 0.1f, cy - s * 0.4f); p.lineTo(cx + s * 0.7f, cy + s * 0.4f)
                p.moveTo(cx + s * 0.7f, cy - s * 0.4f); p.lineTo(cx - s * 0.1f, cy + s * 0.4f)
            }
            Icon.ENTER -> {
                p.moveTo(cx + s, cy - s * 0.8f); p.lineTo(cx + s, cy + s * 0.2f); p.lineTo(cx - s, cy + s * 0.2f)
                p.moveTo(cx - s * 0.35f, cy - s * 0.4f); p.lineTo(cx - s, cy + s * 0.2f); p.lineTo(cx - s * 0.35f, cy + s * 0.8f)
            }
            Icon.NONE -> return
        }
        c.drawPath(p, iconPaint)
    }

    // ---- touch ------------------------------------------------------------------------------

    private fun keyAt(x: Float, y: Float): Key? {
        if (rows.isEmpty()) return null
        val idx = ((y - topPad()) / rowHeight()).toInt().coerceIn(0, rows.size - 1)
        val row = rows[idx]
        if (row.isEmpty()) return null
        val xx = x.coerceIn(row.first().rect.left, row.last().rect.right - 0.01f)
        var i = row.indexOfFirst { xx < it.rect.right }
        if (i < 0) i = row.lastIndex
        val k = row[i]
        if (k.type != KeyType.SPACER) return k
        // Small edge spacers snap to the nearest real key; larger gaps are dead space.
        if (k.weight > 0.6f) return null
        return when (i) {
            0 -> row.getOrNull(1)
            row.lastIndex -> row.getOrNull(i - 1)
            else -> null
        }?.takeIf { it.type != KeyType.SPACER }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val i = e.actionIndex
                down(e.getPointerId(i), e.getX(i), e.getY(i))
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until e.pointerCount) move(e.getPointerId(i), e.getX(i), e.getY(i))
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> up(e.getPointerId(e.actionIndex))
            MotionEvent.ACTION_CANCEL -> stopAll()
        }
        return true
    }

    private fun down(id: Int, x: Float, y: Float) {
        val k = keyAt(x, y) ?: return
        pressed.put(id, k)
        listener?.onPress(k)
        if (k.type == KeyType.SPACE) {
            spacePointer = id
            spaceStartX = x
            spaceSteppedX = x
            swiping = false
        }
        if (k.repeat) startRepeat(k, id)
        invalidate()
    }

    private fun move(id: Int, x: Float, y: Float) {
        val k = pressed.get(id) ?: return
        if (id == spacePointer) {
            val step = SPACE_STEP_DP * dp
            if (!swiping && abs(x - spaceStartX) > step) swiping = true
            if (swiping) {
                val n = ((x - spaceSteppedX) / step).toInt()
                if (n != 0) {
                    listener?.onSpaceSwipe(n)
                    spaceSteppedX += n * step
                }
            }
            return
        }
        val nk = keyAt(x, y)
        if (nk != null && nk !== k) {
            if (id == repeatPointer) stopRepeat()
            pressed.put(id, nk)
            listener?.onPress(nk)
            if (nk.repeat) startRepeat(nk, id)
            invalidate()
        }
    }

    private fun up(id: Int) {
        val k = pressed.get(id) ?: return
        pressed.remove(id)
        when {
            id == repeatPointer -> {
                val fired = repeatFired
                stopRepeat()
                if (!fired) listener?.onKey(k)
            }
            id == spacePointer -> {
                val wasSwiping = swiping
                spacePointer = -1
                swiping = false
                if (!wasSwiping) listener?.onKey(k)
            }
            else -> listener?.onKey(k)
        }
        invalidate()
    }

    private fun startRepeat(k: Key, id: Int) {
        stopRepeat()
        repeatKey = k
        repeatPointer = id
        repeatFired = false
        uiHandler.postDelayed(repeatRunnable, REPEAT_DELAY_MS)
    }

    private fun stopRepeat() {
        uiHandler.removeCallbacks(repeatRunnable)
        repeatKey = null
        repeatPointer = -1
        repeatFired = false
    }

    private fun stopAll() {
        pressed.clear()
        stopRepeat()
        spacePointer = -1
        swiping = false
        invalidate()
    }

    override fun onDetachedFromWindow() {
        stopAll()
        super.onDetachedFromWindow()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility != VISIBLE) stopAll()
    }

    private companion object {
        const val REPEAT_DELAY_MS = 400L
        const val REPEAT_INTERVAL_MS = 50L
        const val SPACE_STEP_DP = 14f
    }
}
