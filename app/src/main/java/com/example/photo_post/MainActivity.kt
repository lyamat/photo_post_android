package com.example.photo_post

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.example.photo_post.databinding.ActivityMainBinding
import java.util.Date
import androidx.lifecycle.ViewModelProvider
import com.example.photo_post.models.Cart
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.Locale


class MainActivity : AppCompatActivity() {

    companion object {
        private var INSTANCE: MainActivity? = null

        fun getInstance(): MainActivity {
            return INSTANCE ?: throw IllegalStateException("MainActivity not initialized")
        }
    }

    private lateinit var binding: ActivityMainBinding
    private val CAMERA_PERMISSION_REQUEST_CODE = 1

    private lateinit var viewModel: SharedViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkCameraPermission()

        INSTANCE = this

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

            val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
            bottomNavigationView.setOnNavigationItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.photoItem -> {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.frameLayout, PhotoFragment())
                            .commit()
                        return@setOnNavigationItemSelectedListener true
                    }
                    R.id.qrItem -> {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.frameLayout, QrFragment())
                            .commit()
                        return@setOnNavigationItemSelectedListener true
                    }
                    R.id.cartItem -> {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.frameLayout, CartFragment())
                            .commit()
                        return@setOnNavigationItemSelectedListener true
                    }
                }
                false
            }

        supportFragmentManager.beginTransaction()
            .replace(R.id.frameLayout, PhotoFragment())
            .commit()

        viewModel = ViewModelProvider(this).get(SharedViewModel::class.java)

        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        val change_password = sharedPrefs.getString("change_password", "")

        val currentDateTime = android.icu.text.SimpleDateFormat(
            "yyyyMMddHHmmss",
            Locale.getDefault()
        ).format(Date())

        val cartName = "Cart ${currentDateTime.takeLast(6)}"
        viewModel.currentCart = Cart(currentDateTime.toLong(), change_password.toString(), cartName)

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        } else {
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_PERMISSION_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                } else {
                }
                return
            }
            else -> {
            }
        }
    }


}
