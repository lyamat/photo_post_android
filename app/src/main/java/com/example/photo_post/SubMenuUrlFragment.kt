package com.example.photo_post

import android.os.Bundle
import android.widget.Toast
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import java.net.MalformedURLException
import java.net.URL


class SubMenuUrlFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.sub_menu_url, rootKey)

        val serverAddressPreference: EditTextPreference? = findPreference("server_address_post")
//        val sendToServerAddressPreference: EditTextPreference? = findPreference("server_address_post_image")

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        serverAddressPreference?.text = sharedPreferences.getString("server_address_post", "")
//        sendToServerAddressPreference?.text = sharedPreferences.getString("server_address_post_image", "")

        serverAddressPreference?.setOnBindEditTextListener { editText ->
            if (editText.text.toString().isEmpty()) {
                editText.setText("http://<server_address>/myproject/api.php")
            }
        }

//        sendToServerAddressPreference?.setOnBindEditTextListener { editText ->
//            if (editText.text.toString().isEmpty()) {
//                editText.setText("http://<server_address>/myproject/upload.php")
//            }
//        }

        serverAddressPreference?.setOnPreferenceChangeListener { preference, newValue ->
            if (!isValidUrl(newValue.toString())) {
                Toast.makeText(requireContext(), "Неверный формат URL-адреса", Toast.LENGTH_SHORT).show()
                return@setOnPreferenceChangeListener false
            }
            preference.summary = newValue.toString()
            sharedPreferences.edit().putString("server_address_post", newValue.toString()).apply()
            true
        }

//        sendToServerAddressPreference?.setOnPreferenceChangeListener { preference, newValue ->
//            if (!isValidUrl(newValue.toString())) {
//                Toast.makeText(requireContext(), "Неверный формат URL-адреса", Toast.LENGTH_SHORT).show()
//                return@setOnPreferenceChangeListener false
//            }
//            preference.summary = newValue.toString()
//            sharedPreferences.edit().putString("server_address_post_image", newValue.toString()).apply()
//            true
//        }

        serverAddressPreference?.summary = serverAddressPreference?.text
//        sendToServerAddressPreference?.summary = sendToServerAddressPreference?.text
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
