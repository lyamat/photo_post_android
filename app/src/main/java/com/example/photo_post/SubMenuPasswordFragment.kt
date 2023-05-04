package com.example.photo_post


import android.os.Bundle
import android.text.InputType
import android.widget.Toast
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import java.net.MalformedURLException
import java.net.URL

class SubMenuPasswordFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.sub_menu_password_preferences, rootKey)

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        val change_password: EditTextPreference? = findPreference("change_password")
        change_password?.text = sharedPreferences.getString("change_password", "")

//        change_password?.setOnBindEditTextListener { editText ->
//            editText?.let {
//                it.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
//                it.setSelection(it.text.length)
//            }
//        }

        change_password?.setSummaryProvider {
            val password = sharedPreferences.getString("change_password", "")
            "•".repeat(password?.length ?: 0)
        }

        change_password?.setOnPreferenceChangeListener { preference, newValue ->
            preference.summaryProvider = null
            preference.summary = "•".repeat(newValue.toString().length)
            sharedPreferences.edit().putString("change_password", newValue.toString()).apply()
            true
        }
    }
}

