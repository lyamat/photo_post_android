package com.example.photo_post

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils


interface MainActivityProvider {
    fun getMainActivity(): MainActivity?
}

class SettingsActivity : AppCompatActivity(), MainActivityProvider {

    override fun getMainActivity(): MainActivity? {
        return if (isFinishing) {
            null
        } else {
            MainActivity.getInstance()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager
            .beginTransaction()
            .replace(android.R.id.content, SettingsFragment())
            .commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                if (supportFragmentManager.backStackEntryCount > 0) {
                    supportFragmentManager.popBackStack()
                } else {
                    NavUtils.navigateUpFromSameTask(this)
                }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }


    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    class MyPreferenceFragment : SettingsFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.preferences)
        }
    }
}
