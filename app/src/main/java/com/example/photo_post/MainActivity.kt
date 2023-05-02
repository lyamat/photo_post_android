package com.example.photo_post

import com.example.photo_post.models.Project
import com.example.photo_post.image_work.*

import android.os.Bundle
import android.content.Intent
import java.io.File
import java.io.BufferedReader
import java.io.FileReader

import androidx.appcompat.app.AppCompatActivity
import androidx.exifinterface.media.ExifInterface

import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.text.SimpleDateFormat
import java.util.*
import android.os.Environment

import android.graphics.*

import android.util.Log
import android.widget.*

import okhttp3.*
import java.io.IOException

import org.json.JSONArray
import org.json.JSONException

import android.graphics.Bitmap
import android.graphics.Matrix
import android.text.TextUtils
import android.view.View
import androidx.appcompat.app.AlertDialog
import android.content.Context
import android.net.wifi.WifiManager
import android.text.format.Formatter
import android.view.Menu
import android.view.MenuItem
import androidx.preference.PreferenceManager

import kotlinx.coroutines.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import java.net.Socket
import java.net.InetSocketAddress

import com.example.photo_post.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private val TAG = "MyActivity"
    private val REQUEST_IMAGE_CAPTURE = 1
    private var currentPhotoPath: String? = null

    private lateinit var projectAdapter: ArrayAdapter<String>
    private var imageBase64: String = ""
    private var comment: String = ""

    private var selectedProjectName: String = ""
    private var selectedIp: String = ""
    private var projectCount: Int = 0

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val projectSpinner: Spinner = findViewById(R.id.projectSpinner)
        projectAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item)
        projectSpinner.adapter = projectAdapter

        populateProjectSpinner()
        // getAvailableIps()

        findViewById<Button>(R.id.button_camera).setOnClickListener {
            dispatchTakePictureIntent()
        }

        findViewById<Button>(R.id.updateProjectListButton).setOnClickListener {
            updateProjectList()
        }

        findViewById<Button>(R.id.sendToServerButton).setOnClickListener {
            showDialog()
        }


        projectSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedProject = parent.getItemAtPosition(position) as String
                selectedProjectName = selectedProject
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }
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
            val reader = BufferedReader(FileReader(projectsFile))
            reader.useLines { lines ->
                lines.forEach {
                    val parts = it.split(":")
                    if (parts.size == 2) {
                        val projectName = parts[1].trim()
                        projectNames.add(projectName)
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

        projectCount = adapter.count
    }

    private fun updateProjectList() {
        val updateButton: Button = findViewById(R.id.updateProjectListButton)
        updateButton.isEnabled = false

        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        val project_list_addresses_post = sharedPrefs.getString("project_list_addresses_post", "")

        checkServerAvailability(project_list_addresses_post!!) { isAvailable, response ->
            if (isAvailable) {
                getProjectList { projectList, message ->
                    val file = File(filesDir, "projects.txt")
                    val sb = StringBuilder()
                    val projectNames = projectList.map { it.projectName }
                    if (projectList.isNotEmpty()) {
                        for (project in projectList) {
                            sb.append("${project.projectId}: ${project.projectName}\n")
                        }
                        file.writeText(sb.toString())
                    } else {
                        file.writeText("")
                        if (message.isEmpty()) {
                            runOnUiThread {
                                Toast.makeText(
                                    this,
                                    "Received empty project list",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }

                    if (message == "Project list address is empty") {

                    }

                    else if (message.isEmpty()) {
                        runOnUiThread {
                            projectAdapter.clear()
                            projectAdapter.addAll(projectNames)
                            projectAdapter.notifyDataSetChanged()
                            populateProjectSpinner()

                            runOnUiThread {
                                Toast.makeText(
                                    this,
                                    "Success. Received ${projectAdapter.count} projects.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                        }
                    }
                    runOnUiThread {
                        updateButton.isEnabled =
                            true // разблокируем кнопку обновления списка проектов
                    }
                }
            } else {
                runOnUiThread {
                    if (response != "") {
                        Toast.makeText(this, response, Toast.LENGTH_SHORT).show()
                    }
                    updateButton.isEnabled = true
                }
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
                    "com.example.android.fileprovider",
                    photoFile)
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    private fun createImageFile(): File? {
        // Создаем уникальное имя для файла
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* префикс */
            ".jpg", /* суффикс */
            storageDir /* директория */
        ).apply {
            // Сохраняем путь к файлу
            currentPhotoPath = absolutePath
        }
    }

    private fun saveImageToGallery() {
        val exif = ExifInterface(currentPhotoPath!!)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_UNDEFINED
        )
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }
        val bitmap = BitmapFactory.decodeFile(currentPhotoPath)
        val orientedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        MediaStore.Images.Media.insertImage(
            contentResolver, orientedBitmap, "PhotoPost", null
        )
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
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        val server_address_post_image = sharedPrefs.getString("server_address_post_image", "")

        checkServerAvailability(server_address_post_image!!) { isAvailable, response ->
            if (isAvailable) {
                if (TextUtils.isEmpty(server_address_post_image)) {
                    runOnUiThread {
                        Toast.makeText(this, "Settings. Post image address is empty", Toast.LENGTH_SHORT).show()
                        findViewById<Button>(R.id.sendToServerButton).isEnabled = true
                    }
                }
                else {
                    val commentEditText: EditText = findViewById(R.id.commentEditText)
                    comment = commentEditText.text.toString()

                    if (projectCount != 0) {
                        if (TextUtils.isEmpty(server_address_post_image)) {
                            runOnUiThread {
                                findViewById<Button>(R.id.sendToServerButton).isEnabled = true
                                Toast.makeText(this, "Settings. Server address is empty", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        } else {
                            if (imageBase64.isNotEmpty()) {
                                val client = OkHttpClient()

                                val requestBody: RequestBody = FormBody.Builder()
                                    .add("imageBase64", imageBase64)
                                    .add("project_name", selectedProjectName)
                                    .add("comment", comment)
                                    .build()

                                val request: Request = Request.Builder()
                                    .url("$server_address_post_image")
                                    .post(requestBody)
                                    .build()

                                client.newCall(request).enqueue(object : Callback {
                                    override fun onResponse(call: Call, response: Response) {
                                        if (response.isSuccessful) {
                                            val logMessage = "Successfully sent to the server."
                                            runOnUiThread {
                                                findViewById<Button>(R.id.sendToServerButton).isEnabled =
                                                    true
                                                Toast.makeText(
                                                    this@MainActivity,
                                                    logMessage,
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                            deletePhotoFile()

                                            Log.e(TAG, logMessage)
//                                        saveLog(logMessage)
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
                                    findViewById<Button>(R.id.sendToServerButton).isEnabled = true
                                    Toast.makeText(this, "No image to send", Toast.LENGTH_SHORT)
                                        .show()
                                }
                            }
                        }
                    } else {
                        runOnUiThread {
                            findViewById<Button>(R.id.sendToServerButton).isEnabled = true
                            Toast.makeText(this, "Project list is empty", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                runOnUiThread{
                    findViewById<Button>(R.id.sendToServerButton).isEnabled = true
                    if (response != "") {
                        Toast.makeText(this, response, Toast.LENGTH_SHORT).show()
                    }
                }

            }
        }

    }

    private fun getProjectList(callback: (List<Project>, String) -> Unit) {

//        val serverAddressEditText: EditText = findViewById(R.id.serverAddressEditText)
//        val serverAddress: String = serverAddressEditText.text.toString()

        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        val server_address = sharedPrefs.getString("server_address", "")
        val project_list_addresses_post = sharedPrefs.getString("project_list_addresses_post", "")


        if (TextUtils.isEmpty(project_list_addresses_post)) {
            runOnUiThread {
                Toast.makeText(this, "Settings. Project list address is empty", Toast.LENGTH_SHORT).show()
                findViewById<Button>(R.id.updateProjectListButton).isEnabled = true
                callback(emptyList(), "Project list address is empty")
            }
        }
        else {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("$project_list_addresses_post")
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
//            runOnUiThread {
//                findViewById<Button>(R.id.updateProjectListButton).isEnabled = true
//            }
        }
    }

    private fun parseProjectList(json: String?): List<Project> {
        val projectList = mutableListOf<Project>()
        json?.let {
            try {
                val jsonArray = JSONArray(it)
                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    val projectId = jsonObject.getInt("project_id")
                    val projectName = jsonObject.getString("project_name")
                    projectList.add(Project(projectId, projectName))
                }
            } catch (e: JSONException) {
                Log.e(TAG, "Error parsing project list: ${e.message}")
            }
        }
        return projectList
    }

    private fun checkServerAvailability(serverAddress: String, callback: (Boolean, String) -> Unit) {

//        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
//        val server_address = sharedPrefs.getString("server_address", "")
//        val project_list_addresses_post = sharedPrefs.getString("project_list_addresses_post", "")


        if (TextUtils.isEmpty(serverAddress)) {
            runOnUiThread {
                Toast.makeText(this, "Settings. Address is empty", Toast.LENGTH_SHORT).show()
                findViewById<Button>(R.id.updateProjectListButton).isEnabled = true
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
//                        findViewById<Button>(R.id.updateProjectListButton).isEnabled = true
                        callback(true, "")
                    }

                }

                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "Server is unavailable: ${e.message}")
                    runOnUiThread {
                        findViewById<Button>(R.id.updateProjectListButton).isEnabled = true
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

    private fun saveLog(logMessage: String) {
        val logFile = File("some path")
        if (!logFile.exists()) {
            logFile.createNewFile()
        }

        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
        val currentTime = System.currentTimeMillis()
        val currentDateString = dateFormat.format(currentTime)

        logFile.appendText("[$currentDateString]: $logMessage\n")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            val photoImageView = findViewById<ImageView>(R.id.photoImageView)
            val rotatedBitmap = getRotatedImageWithExif(currentPhotoPath!!)
            photoImageView.setImageBitmap(rotatedBitmap)

            CoroutineScope(Dispatchers.IO).launch {
                imageBase64 = convertImageToBase64(rotatedBitmap)
            }
        }
    }
}
