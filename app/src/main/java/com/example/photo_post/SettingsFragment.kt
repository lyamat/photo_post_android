package com.example.photo_post

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Toast
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import java.net.MalformedURLException
import java.net.URL


open class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        val getProjectListAddressPreference: EditTextPreference? = findPreference("project_list_addresses_post")
        val sendToServerAddressPreference: EditTextPreference? = findPreference("server_address_post_image")

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())


        getProjectListAddressPreference?.text = sharedPreferences.getString("project_list_addresses_post", "")
        sendToServerAddressPreference?.text = sharedPreferences.getString("server_address_post_image", "")

        getProjectListAddressPreference?.setOnBindEditTextListener { editText ->
            if (editText.text.toString().isEmpty()) {
                editText.setText("http://<server_address>/myproject/get_project_list.php")
            }
        }

        sendToServerAddressPreference?.setOnBindEditTextListener { editText ->
            if (editText.text.toString().isEmpty()) {
                editText.setText("http://<server_address>/myproject/upload.php")
            }
        }

        val urlRegex = Regex("(http|https)://[a-zA-Z0-9./-]+")

        getProjectListAddressPreference?.setOnPreferenceChangeListener { preference, newValue ->
            val isValidUrl = urlRegex.matches(newValue.toString())
            if (isValidUrl) {
                preference.summary = newValue.toString()
                sharedPreferences.edit().putString("project_list_addresses_post", newValue.toString()).apply()
                true
            } else {
                AlertDialog.Builder(requireContext())
                    .setTitle("Невалидный URL-адрес")
                    .setMessage("Пожалуйста, введите корректный URL-адрес.")
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
                false
            }
        }

        sendToServerAddressPreference?.setOnPreferenceChangeListener { preference, newValue ->
            preference.summary = newValue.toString()
            sharedPreferences.edit().putString("server_address_post_image", newValue.toString()).apply()
            true
        }

        getProjectListAddressPreference?.summary = getProjectListAddressPreference?.text
        sendToServerAddressPreference?.summary = sendToServerAddressPreference?.text
    }
}

