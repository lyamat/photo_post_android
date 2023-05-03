package com.example.photo_post

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

        getProjectListAddressPreference?.setOnPreferenceChangeListener { preference, newValue ->
            if (!isValidUrl(newValue.toString())) {
                Toast.makeText(requireContext(), "Неверный формат URL-адреса", Toast.LENGTH_SHORT).show()
                return@setOnPreferenceChangeListener false
            }
            preference.summary = newValue.toString()
            sharedPreferences.edit().putString("project_list_addresses_post", newValue.toString()).apply()
            true
        }

        sendToServerAddressPreference?.setOnPreferenceChangeListener { preference, newValue ->
            if (!isValidUrl(newValue.toString())) {
                Toast.makeText(requireContext(), "Неверный формат URL-адреса", Toast.LENGTH_SHORT).show()
                return@setOnPreferenceChangeListener false
            }
            preference.summary = newValue.toString()
            sharedPreferences.edit().putString("server_address_post_image", newValue.toString()).apply()
            true
        }

        getProjectListAddressPreference?.summary = getProjectListAddressPreference?.text
        sendToServerAddressPreference?.summary = sendToServerAddressPreference?.text
    }
    private fun isValidUrl(url: String): Boolean {
        return try {
            URL(url)
            true
        } catch (e: MalformedURLException) {
            false
        }
    }
}



