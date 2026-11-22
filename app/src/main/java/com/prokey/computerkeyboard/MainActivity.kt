package com.prokey.computerkeyboard

import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import android.view.inputmethod.InputMethodManager

class MainActivity : AppCompatActivity() {

    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        col.addView(TextView(this).apply {
            text = getString(R.string.app_name)
            textSize = 24f
            setTypeface(null, Typeface.BOLD)
        })
        status = TextView(this).apply {
            textSize = 14f
            setPadding(0, dp(4), 0, dp(12))
        }
        col.addView(status)

        col.addView(card(R.string.step_enable, R.string.step_enable_sub) {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        })
        col.addView(card(R.string.step_select, R.string.step_select_sub) {
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
        })
        col.addView(card(R.string.step_settings, R.string.step_settings_sub) {
            startActivity(Intent(this, SettingsActivity::class.java))
        })
        col.addView(card(R.string.step_privacy, R.string.step_privacy_sub) {
            showText(getString(R.string.privacy_title), getString(R.string.privacy_body))
        })
        col.addView(card(R.string.step_about, R.string.step_about_sub) {
            @Suppress("DEPRECATION")
            val version = packageManager.getPackageInfo(packageName, 0).versionName ?: "?"
            showText(getString(R.string.about_title), getString(R.string.about_body, version, packageName))
        })
        col.addView(EditText(this).apply {
            hint = getString(R.string.try_hint)
            setPadding(dp(8), dp(16), dp(8), dp(16))
        })
        setContentView(ScrollView(this).apply { addView(col) })
    }

    override fun onResume() {
        super.onResume()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        val enabled = imm.enabledInputMethodList.any { it.packageName == packageName }
        val current = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        val selected = current?.startsWith("$packageName/") == true
        status.setText(
            when {
                !enabled -> R.string.status_not_enabled
                !selected -> R.string.status_not_selected
                else -> R.string.status_ready
            }
        )
    }

    private fun showText(title: String, body: String) {
        AlertDialog.Builder(this).setTitle(title).setMessage(body)
            .setPositiveButton(android.R.string.ok, null).show()
    }

    private fun card(title: Int, sub: Int, onClick: () -> Unit): View {
        val tv = TypedValue()
        theme.resolveAttribute(android.R.attr.colorBackgroundFloating, tv, true)
        val bg = GradientDrawable().apply {
            cornerRadius = dp(14).toFloat()
            setColor(tv.data)
            setStroke(dp(1), 0x44888888)
        }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = bg
            setPadding(dp(16), dp(14), dp(16), dp(14))
            isClickable = true
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(10) }
        }
        box.addView(TextView(this).apply {
            setText(title)
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
        })
        box.addView(TextView(this).apply {
            setText(sub)
            textSize = 13f
            alpha = 0.7f
        })
        return box
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
