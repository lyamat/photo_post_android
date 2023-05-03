package com.example.photo_post

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.MalformedURLException
import java.net.URL


open class SettingsFragment : PreferenceFragmentCompat() {

    private var mainActivityProvider: MainActivityProvider? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is MainActivityProvider) {
            mainActivityProvider = context
        }
    }


    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        val updatePreference: Preference? = findPreference("update")
        val subMenuUrlPreference: Preference? = findPreference("sub_menu_url")
        val subMenuPasswordPreference: Preference? = findPreference("sub_menu_password")


        subMenuUrlPreference?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            fragmentManager?.beginTransaction()?.replace(android.R.id.content, SubMenuUrlFragment())?.addToBackStack(null)?.commit()
            true
        }

        subMenuPasswordPreference?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            fragmentManager?.beginTransaction()?.replace(android.R.id.content, SubMenuPasswordFragment())?.addToBackStack(null)?.commit()
            true
        }

        updatePreference?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            updatePreference?.isEnabled = false
            mainActivityProvider?.getMainActivity()?.updateProjectList { success ->
                updatePreference?.isEnabled = true
            }
            true
        }


    }
}



