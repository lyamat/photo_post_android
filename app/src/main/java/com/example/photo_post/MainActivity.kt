package com.example.photo_post

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import androidx.preference.PreferenceManager
import com.example.photo_post.databinding.ActivityMainBinding
import com.example.photo_post.image_work.convertImageToBase64
import com.example.photo_post.image_work.getRotatedImageWithExif
import com.example.photo_post.models.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.Result
import com.google.zxing.common.HybridBinarizer
import android.webkit.URLUtil
import android.net.Uri
import com.example.photo_post.models.Qr
import com.google.android.material.bottomnavigation.BottomNavigationView


class MainActivity : AppCompatActivity() {

    companion object {
        private var INSTANCE: MainActivity? = null

        fun getInstance(): MainActivity {
            return INSTANCE ?: throw IllegalStateException("MainActivity not initialized")
        }
    }

    private val TAG = "MyActivity"
    private val REQUEST_IMAGE_CAPTURE = 1
    private var currentPhotoPath: String? = null

    private lateinit var projectAdapter: ArrayAdapter<String>
    private var imageBase64: String = ""
    private var comment: String = ""

    private var selectedProjectName: String = ""
    private var selectedProjectId: String = ""
    private var projectListIds: MutableList<Int> = mutableListOf()


    private var projectCount: Int = 0

    private lateinit var binding: ActivityMainBinding

    private val REQUEST_CODE_SCANNER = 2001


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
            }
            false
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.frameLayout, PhotoFragment())
            .commit()

//        val projectSpinner: Spinner = findViewById(R.id.projectSpinner)
//        projectAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item)
//        projectSpinner.adapter = projectAdapter
//
//        populateProjectSpinner()
//
//        val commentEditText = findViewById<EditText>(R.id.commentEditText)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            commentEditText.showSoftInputOnFocus = true;
//        }
//
//
//        findViewById<ImageView>(R.id.button_camera).setOnClickListener {
////            dispatchTakePictureIntent()
//            checkCameraPermission()
//        }
//
//        findViewById<ImageView>(R.id.button_qr).setOnClickListener {
//            val intent = Intent(this, ScannerActivity::class.java)
//            startActivityForResult(intent, REQUEST_CODE_SCANNER)
//        }
//
//
//        findViewById<Button>(R.id.sendToServerButton).setOnClickListener {
//            showDialog()
//        }
//
//        projectSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
//            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
//                val selectedProject = parent.getItemAtPosition(position) as String
//                selectedProjectName = selectedProject
//                if(projectListIds.isNotEmpty()) {
//                    selectedProjectId = projectListIds[position].toString()
//                }
//            }
//
//            override fun onNothingSelected(parent: AdapterView<*>) {
//                // Do nothing
//            }
//        }
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

    private fun populateProjectSpinner() {
        val projectsFile = File(filesDir, "projects.txt")
        val projectNames = mutableListOf<String>()

        if (projectsFile.exists()) {
            projectListIds.clear()
            val reader = BufferedReader(FileReader(projectsFile))
            reader.useLines { lines ->
                lines.forEach {
                    val parts = it.split(":")
                    if (parts.size == 2) {
                        val projectName = parts[1].trim()
                        projectNames.add(projectName)
                        projectListIds.add(parts[0].trim().toInt())
                    }
                }
            }
        }

        if (projectNames.isEmpty()) {
            projectNames.add("No projects")
        }

        val projectSpinner: Spinner = findViewById(R.id.projectSpinner)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, projectNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        projectSpinner.adapter = adapter

        projectCount = if (projectNames[0] == "No projects") 0 else adapter.count

    }

    public fun updateProjectList(callback: (Boolean) -> Unit) {

        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        val server_address_post = sharedPrefs.getString("server_address_post", "")

//        val project_list_addresses_post = sharedPrefs.getString("project_list_addresses_post", "")

        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        val isConnected = networkInfo != null && networkInfo.isConnected && networkInfo.type == ConnectivityManager.TYPE_WIFI

        if (isConnected) {
            val change_password = sharedPrefs.getString("change_password", "")

            if (TextUtils.isEmpty(change_password)) {
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "Settings. Password is empty",
                        Toast.LENGTH_SHORT
                    ).show()
                    callback(true)
                }
            }
            else {
                checkServerAvailability(server_address_post!!) { isAvailable, response ->
                    if (isAvailable) {
                        getProjectList { projectList, message ->
                            val file = File(filesDir, "projects.txt")
                            val sb = StringBuilder()
                            val projectNames = projectList.map { it.projectName }
                            if (projectList.isNotEmpty()) {
                                projectListIds.clear()
                                for (project in projectList) {
                                    sb.append("${project.projectId}: ${project.projectName}\n")
                                    projectListIds += project.projectId
                                }
                                file.writeText(sb.toString())
                            } else {
                                file.writeText("")
                                if (message.isEmpty()) {
                                    runOnUiThread {
                                        Toast.makeText(
                                            this,
                                            "Received empty response body",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }

                            if (message == "Project list address is empty") {

                            } else if (message.isEmpty()) {
                                runOnUiThread {
                                    projectAdapter.clear()
                                    projectAdapter.addAll(projectNames)
                                    projectAdapter.notifyDataSetChanged()
                                    populateProjectSpinner()
                                    if (projectList.isNotEmpty()) {
                                        runOnUiThread {
                                            Toast.makeText(
                                                this,
                                                "Success. Received ${projectAdapter.count} projects.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            } else {
                                runOnUiThread {
                                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                                }
                            }
                            runOnUiThread {
                                callback(true)
                            }
                        }
                    } else {
                        runOnUiThread {
                            if (response != "") {
                                Toast.makeText(this, response, Toast.LENGTH_SHORT).show()
                            }
                            callback(true)
                        }
                    }
                }
            }
        }
        else {
            runOnUiThread {
                Toast.makeText(this, "Check Wi-fi connection", Toast.LENGTH_SHORT).show()
                callback(true)
            }
        }
    }

    public fun showQrInfo(callback: (Boolean) -> Unit) {

        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        val server_address_post = sharedPrefs.getString("server_address_post", "")

//        val project_list_addresses_post = sharedPrefs.getString("project_list_addresses_post", "")

        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        val isConnected = networkInfo != null && networkInfo.isConnected && networkInfo.type == ConnectivityManager.TYPE_WIFI

        if (isConnected) {
            val change_password = sharedPrefs.getString("change_password", "")

            if (TextUtils.isEmpty(change_password)) {
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "Settings. Password is empty",
                        Toast.LENGTH_SHORT
                    ).show()
                    callback(true)
                }
            }
            else {
                checkServerAvailability(server_address_post!!) { isAvailable, response ->
                    if (isAvailable) {
                        getProjectList { projectList, message ->
                            val file = File(filesDir, "projects.txt")
                            val sb = StringBuilder()
                            val projectNames = projectList.map { it.projectName }
                            if (projectList.isNotEmpty()) {
                                projectListIds.clear()
                                for (project in projectList) {
                                    sb.append("${project.projectId}: ${project.projectName}\n")
                                    projectListIds += project.projectId
                                }
                                file.writeText(sb.toString())
                            } else {
                                file.writeText("")
                                if (message.isEmpty()) {
                                    runOnUiThread {
                                        Toast.makeText(
                                            this,
                                            "Received empty response body",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }

                            if (message == "Project list address is empty") {

                            } else if (message.isEmpty()) {
                                runOnUiThread {
                                    projectAdapter.clear()
                                    projectAdapter.addAll(projectNames)
                                    projectAdapter.notifyDataSetChanged()
                                    populateProjectSpinner()
                                    if (projectList.isNotEmpty()) {
                                        runOnUiThread {
                                            Toast.makeText(
                                                this,
                                                "Success. Received ${projectAdapter.count} projects.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            } else {
                                runOnUiThread {
                                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                                }
                            }
                            runOnUiThread {
                                callback(true)
                            }
                        }
                    } else {
                        runOnUiThread {
                            if (response != "") {
                                Toast.makeText(this, response, Toast.LENGTH_SHORT).show()
                            }
                            callback(true)
                        }
                    }
                }
            }
        }
        else {
            runOnUiThread {
                Toast.makeText(this, "Check Wi-fi connection", Toast.LENGTH_SHORT).show()
                callback(true)
            }
        }
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            val photoFile: File? = try {
                createImageFile()
            } catch (ex: IOException) {
                null
            }
            photoFile?.also {
                val photoURI = FileProvider.getUriForFile(this,
                    "com.example.photo_post.fileprovider",
                    photoFile)
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    private fun createImageFile(): File? {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun showDialog() {
            val commentEditText: EditText = findViewById(R.id.commentEditText)
            comment = commentEditText.text.toString()
            if (imageBase64.isNotEmpty()) {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Отправить на сервер?")
                builder.setMessage("Комментарий: $comment\nВыбранный проект: $selectedProjectName")
                builder.setPositiveButton("Да") { dialog, which ->
                    findViewById<Button>(R.id.sendToServerButton).isEnabled = false
                    sendToServer()
                    Log.d(
                        TAG,
                        "Отправка на сервер: Фото, выбранный проект - $selectedProjectName, комментарий - $comment"
                    )
                }
                builder.setNegativeButton("Отмена") { dialog, which ->
                    dialog.dismiss()
                }
                builder.show()
            }
            else {
                runOnUiThread {
                    Toast.makeText(this, "No image to send", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun sendToServer() {

        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        val isConnected = networkInfo != null && networkInfo.isConnected && networkInfo.type == ConnectivityManager.TYPE_WIFI

        if (isConnected) {
            val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
            val server_address_post = sharedPrefs.getString("server_address_post", "")
            val change_password = sharedPrefs.getString("change_password", "")

            if (TextUtils.isEmpty(change_password)) {
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "Settings. Password is empty",
                        Toast.LENGTH_SHORT
                    ).show()
                    findViewById<Button>(R.id.sendToServerButton).isEnabled = true
                }
            }
            else {
                checkServerAvailability(server_address_post!!) { isAvailable, response ->
                    if (isAvailable) {
                        if (TextUtils.isEmpty(server_address_post)) {
                            runOnUiThread {
                                Toast.makeText(
                                    this,
                                    "Settings. Server address is empty",
                                    Toast.LENGTH_SHORT
                                ).show()
                                findViewById<Button>(R.id.sendToServerButton).isEnabled = true
                            }
                        } else {
                            val commentEditText: EditText = findViewById(R.id.commentEditText)
                            comment = commentEditText.text.toString()

                            if (projectCount != 0) {
                                if (TextUtils.isEmpty(server_address_post)) {
                                    runOnUiThread {
                                        findViewById<Button>(R.id.sendToServerButton).isEnabled =
                                            true
                                        Toast.makeText(
                                            this,
                                            "Settings. Server address is empty",
                                            Toast.LENGTH_SHORT
                                        )
                                            .show()
                                    }
                                } else {
                                    if (imageBase64.isNotEmpty()) {
                                        val client = OkHttpClient()

                                        val requestBody: RequestBody = FormBody.Builder()
                                            .add("request_command", "upload")
                                            .add("password", change_password!!)
                                            .add("imageBase64", imageBase64)
                                            .add("project_name", selectedProjectName)
                                            .add("project_id", selectedProjectId)
                                            .add("comment", comment)
                                            .build()

                                        val request: Request = Request.Builder()
                                            .url("$server_address_post")
                                            .post(requestBody)
                                            .build()


                                        client.newCall(request).enqueue(object : Callback {
                                            override fun onResponse(
                                                call: Call,
                                                response: Response
                                            ) {
                                                if (response.isSuccessful) {
                                                    val logMessage =
                                                        "Successfully sent to the server."
                                                    runOnUiThread {
                                                        findViewById<Button>(R.id.sendToServerButton).isEnabled =
                                                            true
                                                        Toast.makeText(
                                                            this@MainActivity,
                                                            logMessage,
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }

                                                    Log.e(TAG, logMessage)
                                                } else {
                                                    val logMessage = "Error sending request"
                                                    runOnUiThread {
                                                        findViewById<Button>(R.id.sendToServerButton).isEnabled =
                                                            true
                                                        Toast.makeText(
                                                            this@MainActivity,
                                                            "Error sending request",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                    Log.e(TAG, "$logMessage: ${response.message}")
                                                }
                                            }

                                            override fun onFailure(call: Call, e: IOException) {
                                                runOnUiThread {
                                                    findViewById<Button>(R.id.sendToServerButton).isEnabled =
                                                        true
                                                    Toast.makeText(
                                                        this@MainActivity,
                                                        "Failure sending request. Check url",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                                Log.e(TAG, "Error sending request: ${e.message}")
                                            }
                                        })

                                    } else {
                                        runOnUiThread {
                                            findViewById<Button>(R.id.sendToServerButton).isEnabled =
                                                true
                                            Toast.makeText(
                                                this,
                                                "No image to send",
                                                Toast.LENGTH_SHORT
                                            )
                                                .show()
                                        }
                                    }
                                }
                            } else {
                                runOnUiThread {
                                    findViewById<Button>(R.id.sendToServerButton).isEnabled = true
                                    Toast.makeText(
                                        this,
                                        "Project list is empty",
                                        Toast.LENGTH_SHORT
                                    )
                                        .show()
                                }
                            }
                        }
                    } else {
                        runOnUiThread {
                            findViewById<Button>(R.id.sendToServerButton).isEnabled = true
                            if (response != "") {
                                Toast.makeText(this, response, Toast.LENGTH_SHORT).show()
                            }
                        }

                    }
                }
            }
        }
        else {
            runOnUiThread {
                Toast.makeText(this, "Check Wi-fi connection", Toast.LENGTH_SHORT).show()
                findViewById<Button>(R.id.sendToServerButton).isEnabled = true
            }
        }

    }

    private fun getProjectList(callback: (List<Project>, String) -> Unit) {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        val server_address_post = sharedPrefs.getString("server_address_post", "")

        val change_password = sharedPrefs.getString("change_password", "")

        if (TextUtils.isEmpty(server_address_post)) {
            runOnUiThread {
                Toast.makeText(this, "Settings. Project list address is empty", Toast.LENGTH_SHORT).show()
                callback(emptyList(), "Project list address is empty")
            }
        }
        else {
            val client = OkHttpClient()

            val requestBody: RequestBody = FormBody.Builder()
                .add("request_command", "get_project_list")
                .add("password", change_password!!)
                .build()

            val request: Request = Request.Builder()
                .url("$server_address_post") // http://192.168.100.5/myproject/api.php
                .post(requestBody)
                .build()


            client.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        val projectList = parseProjectList(responseBody)
                        callback(projectList, "")
                    } else {
                        val logMsg = "Response unsuccessful, getting project list"
                        Log.e(TAG, "$logMsg: ${response.message}")
                        callback(emptyList(), "$logMsg: ${response.message}")
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "Failure, getting project list: ${e.message}")
                    callback(emptyList(), e.message ?: "")
                }
            })
        }
    }

    private fun getQrInfo(qrCode: String?, callback: (String?, String) -> Unit) {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        val server_address_post = sharedPrefs.getString("server_address_post", "")

        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        val isConnected = networkInfo != null && networkInfo.isConnected && networkInfo.type == ConnectivityManager.TYPE_WIFI

        if (isConnected) {
            val change_password = sharedPrefs.getString("change_password", "")

            if (TextUtils.isEmpty(change_password)) {
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "Settings. Password is empty",
                        Toast.LENGTH_SHORT
                    ).show()
                    callback("","Settings. Password is empty")
                }
            }
            else {
                checkServerAvailability(server_address_post!!) { isAvailable, response ->
                    if (isAvailable) {
                        if (TextUtils.isEmpty(server_address_post)) {
                            runOnUiThread {
                                Toast.makeText(
                                    this,
                                    "Settings. Server address is empty (from qr_info)",
                                    Toast.LENGTH_SHORT
                                ).show()
                                callback("", "Server address is empty (from qr_info)")
                            }
                        } else {
                            val client = OkHttpClient()

                            val requestBody: RequestBody = FormBody.Builder()
                                .add("request_command", "get_qr_info")
                                .add("password", change_password!!)
                                .add("qr_code", qrCode!!)
                                .build()

                            val request: Request = Request.Builder()
                                .url("$server_address_post") // http://192.168.100.5/myproject/api.php
                                .post(requestBody)
                                .build()


                            client.newCall(request).enqueue(object : Callback {
                                override fun onResponse(call: Call, response: Response) {
                                    if (response.isSuccessful) {
                                        runOnUiThread {
                                            val responseBody = response.body?.string()
//                        val qrInfoList = parseQrInfo(responseBody)
                                            callback(responseBody, "")
                                        }
                                    } else {
                                        val logMsg = "Response unsuccessful, getting qr info"
                                        Log.e(TAG, "$logMsg: ${response.message}")
                                        callback("", "$logMsg: ${response.message}")
                                    }
                                }

                                override fun onFailure(call: Call, e: IOException) {
                                    Log.e(TAG, "Failure, getting qr info: ${e.message}")
                                    callback("", e.message ?: "")
                                }
                            })
                        }
                    } else {
                        runOnUiThread {
                            if (response != "") {
                                Toast.makeText(this, response, Toast.LENGTH_SHORT).show()
                            }
                            callback("","")
                        }
                    }
                }

            }
        }
        else {
            runOnUiThread {
                Toast.makeText(this, "Check Wi-fi connection", Toast.LENGTH_SHORT).show()
                callback("", "Check Wi-fi connection")
            }
        }
    }

    private fun parseProjectList(json: String?): List<Project> {
        val projectList = mutableListOf<Project>()
        json?.let {
            try {
                val jsonArray = JSONArray(it)
                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    val projectId = jsonObject.getInt("id")
                    val projectName = jsonObject.getString("name")
                    projectList.add(Project(projectId, projectName))
                }
            } catch (e: JSONException) {
                Log.e(TAG, "Error parsing project list: ${e.message}")
            }
        }
        return projectList
    }

    private fun checkServerAvailability(serverAddress: String, callback: (Boolean, String) -> Unit) {

        if (TextUtils.isEmpty(serverAddress)) {
            runOnUiThread {
                Toast.makeText(this, "Settings. Address is empty", Toast.LENGTH_SHORT).show()
                callback(false, "")
            }
        } else {
            val client = OkHttpClient()
            val request: Request = Request.Builder()
                .url("$serverAddress")
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    Log.d(TAG, "The server is available")
                    runOnUiThread {
                        callback(true, "")
                    }

                }

                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "Server is unavailable: ${e.message}")
                    runOnUiThread {
                        callback(false, e.message!!)
                    }
                }
            })
        }
    }

    private fun deletePhotoFile() {
        val file = File(currentPhotoPath!!)
        if (file.exists()) {
            file.delete()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SCANNER && resultCode == Activity.RESULT_OK) {
            val qrCodeResult = data?.getStringExtra("qr_code_result")
            if (qrCodeResult != null) {
                Toast.makeText(this, "QR result: $qrCodeResult", Toast.LENGTH_LONG).show()

                // Check if the QR code result is a URL
                if (URLUtil.isValidUrl(qrCodeResult)) {
                    AlertDialog.Builder(this)
                        .setTitle("Переход по URL")
                        .setMessage("Перейти по ссылке?\n$qrCodeResult")
                        .setPositiveButton("Да") { _, _ ->
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(qrCodeResult))
                            startActivity(intent)
                        }
                        .setNegativeButton("Нет", null)
                        .show()
                }

                getQrInfo(qrCodeResult) { qrInfoJson, message ->
                    val commentEditText: EditText = findViewById(R.id.commentEditText)
                    commentEditText.text = Editable.Factory.getInstance().newEditable(qrInfoJson)
                }
            }
        }
        else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            val photoImageView = findViewById<ImageView>(R.id.photoImageView)
            val rotatedBitmap = getRotatedImageWithExif(currentPhotoPath!!)
            photoImageView.setImageBitmap(rotatedBitmap)

            CoroutineScope(Dispatchers.IO).launch {
                imageBase64 = convertImageToBase64(rotatedBitmap)
                deletePhotoFile()
            }
        }
    }


    private val CAMERA_PERMISSION_REQUEST_CODE = 1001

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
        } else {
            dispatchTakePictureIntent()
        }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults) // Добавьте эту строку для вызова родительского метода
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent()
            } else {
                Toast.makeText(this, "Разрешение на использование камеры отклонено", Toast.LENGTH_SHORT).show()
            }
        }
    }

}
