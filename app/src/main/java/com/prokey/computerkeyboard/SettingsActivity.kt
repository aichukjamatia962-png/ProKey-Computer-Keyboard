package com.prokey.computerkeyboard

import android.os.Bundle
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (12 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        // Type here with ProKey selected: the keyboard updates live as you change settings below.
        root.addView(
            EditText(this).apply {
                hint = getString(R.string.try_hint)
                setPadding(pad, pad, pad, pad)
            },
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        )
        val container = FrameLayout(this).apply { id = R.id.settings_container }
        root.addView(container, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.settings_container, SettingsFragment())
                .commit()
        }
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)
            findPreference<ListPreference>(Prefs.THEME)?.apply {
                entries = Array<CharSequence>(Themes.all.size) { Themes.all[it].name }
                entryValues = Array<CharSequence>(Themes.all.size) { Themes.all[it].id }
            }
            findPreference<Preference>("privacy_info")?.setOnPreferenceClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.privacy_title)
                    .setMessage(R.string.privacy_body)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
                true
            }
        }
    }
}
