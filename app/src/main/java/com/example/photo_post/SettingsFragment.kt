package com.example.photo_post

import android.os.Bundle
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.example.photo_post.server.NetworkHelper


open class SettingsFragment : PreferenceFragmentCompat() {
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
            val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext())

            updatePreference?.isEnabled = false
            NetworkHelper(requireContext()).checkServerAvailability() { isServerAvailable, message ->
                if (isServerAvailable) {
                    NetworkHelper(requireContext()).updateProjectList { projectList, message ->
                        if (projectList.isNotEmpty()) {
                            sharedPrefs.edit().putStringSet("projectNames", projectList.map { it.projectName }.toSet()).apply()
//                            sharedPrefs.edit().putString("selectedProjectName", projectList[0].projectName).apply()
//                            editor.putStringSet("projectListIds", projectList.map { it.projectId.toString() }.toSet()).apply()

                            requireActivity().runOnUiThread {
                                Toast.makeText(
                                    requireContext(),
                                    "Success. Received ${projectList.size} projects.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                updatePreference?.isEnabled = true
                            }
                        } else {
                            sharedPrefs.edit().putStringSet("projectNames", emptySet()).apply()
//                            sharedPrefs.edit().putString("selectedProjectName", projectList[0].projectName).apply()
                            requireActivity().runOnUiThread {
                                Toast.makeText(
                                    requireContext(),
                                    message,
                                    Toast.LENGTH_SHORT
                                ).show()
                                updatePreference?.isEnabled = true
                            }

                        }

                    }
                }
                else {
                    requireActivity().runOnUiThread {
                        Toast.makeText(
                            activity,
                            message,
                            Toast.LENGTH_SHORT
                        ).show()
                        updatePreference?.isEnabled = true
                    }
                }
            }

            true
        }

    }
}