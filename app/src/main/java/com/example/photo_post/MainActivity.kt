package com.example.photo_post

import com.example.photo_post.models.Project
import com.example.photo_post.image_work.*

import android.os.Bundle
import android.content.Intent
import java.io.File
import java.io.BufferedReader

import java.io.FileReader


import androidx.appcompat.app.AppCompatActivity
import android.media.ExifInterface


import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.text.SimpleDateFormat
import java.util.*
import android.os.Environment


import androidx.camera.*
import androidx.camera.core.*
import androidx.camera.lifecycle.*
import androidx.camera.view.*

import android.graphics.*
import android.util.Base64
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



class MainActivity : AppCompatActivity() {
    private val TAG = "MyActivity"
    private val REQUEST_IMAGE_CAPTURE = 1
    private var currentPhotoPath: String? = null
    private lateinit var projectAdapter: ArrayAdapter<String>
    private var imageBase64: String = ""
//    private var projectNameSpinner: String = ""
    private var comment: String = ""
    private var selectedProjectName: String = ""
    private var project_count: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val projectSpinner: Spinner = findViewById(R.id.projectSpinner)
        projectAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item)
        projectSpinner.adapter = projectAdapter

        populateProjectSpinner()

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

//                val tw = findViewById<TextView>(R.id.textView)
//                tw.text = projectAdapter.count.toString()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
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

        project_count = adapter.count
    }

    private fun updateProjectList() {
        checkServerAvailability { isAvailable, response ->
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
                    }
                    else {
                        file.writeText("")
                        if (message.isEmpty()) {
                            runOnUiThread {
                                Toast.makeText(this, "Received empty project list", Toast.LENGTH_LONG)
                                    .show()
                            }
                        }
                    }
                    if (message.isEmpty()) {
                        runOnUiThread {
                            projectAdapter.clear()
                            projectAdapter.addAll(projectNames)
                            projectAdapter.notifyDataSetChanged()
                            populateProjectSpinner()

                            runOnUiThread {
                                Toast.makeText(this, "Success. Received ${projectAdapter.count} projects.",
                                    Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    else {
                        runOnUiThread {
                            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } else {
                runOnUiThread{
                    Toast.makeText(this, response, Toast.LENGTH_SHORT).show()
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
        checkServerAvailability { isAvailable, response ->
            if (isAvailable) {
                val serverAddressEditText: EditText = findViewById(R.id.serverAddressEditText)
                val serverAddress: String = serverAddressEditText.text.toString()

                val commentEditText: EditText = findViewById(R.id.commentEditText)
                comment = commentEditText.text.toString()

                if (project_count != 0) {
                    if (TextUtils.isEmpty(serverAddress)) {
                        runOnUiThread {
                            Toast.makeText(this, "Server address is empty", Toast.LENGTH_SHORT)
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
                                .url("http://$serverAddress/myproject/upload.php")
                                .post(requestBody)
                                .build()

                            client.newCall(request).enqueue(object : Callback {
                                override fun onResponse(call: Call, response: Response) {
                                    if (response.isSuccessful) {
                                        val logMessage = "Successfully sent to the server."
                                        runOnUiThread {
                                            Toast.makeText(
                                                this@MainActivity,
                                                logMessage,
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        deletePhotoFile()

                                        Log.e(TAG, logMessage)
                                        saveLog(logMessage)
                                    } else {
                                        val logMessage = "Error sending request"
                                        runOnUiThread {
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
                                        Toast.makeText(
                                            this@MainActivity,
                                            "No image to send",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    Log.e(TAG, "Error sending request: ${e.message}")
                                }
                            })

                        } else {
                            runOnUiThread {
                                Toast.makeText(this, "No image to send", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                else {
                    runOnUiThread {
                        Toast.makeText(this, "Project list is empty", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
//                tw.text = response
                runOnUiThread{
                    Toast.makeText(this, response, Toast.LENGTH_SHORT).show()
                }

            }
        }

    }

    private fun getProjectList(callback: (List<Project>, String) -> Unit) {

        val serverAddressEditText: EditText = findViewById(R.id.serverAddressEditText)
        val serverAddress: String = serverAddressEditText.text.toString()

        val client = OkHttpClient()
        val request = Request.Builder()
            .url("http://$serverAddress:80/myproject/get_project_list.php")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val projectList = parseProjectList(responseBody)
                    callback(projectList, "")
                } else {
                    var logMsg = "Response unsuccessful, getting project list"
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

    private fun checkServerAvailability(callback: (Boolean, String) -> Unit) {
        val serverAddressEditText: EditText = findViewById(R.id.serverAddressEditText)
        val serverAddress: String = serverAddressEditText.text.toString()

        if (TextUtils.isEmpty(serverAddress)) {
            runOnUiThread {
                Toast.makeText(this, "Server address is empty", Toast.LENGTH_SHORT).show()
            }
        } else {
            val client = OkHttpClient()
            val request: Request = Request.Builder()
                .url("http://$serverAddress/")
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    // Обработка успешного ответа от сервера
                    Log.d(TAG, "The server is available")
                    callback(true, "")
                }

                override fun onFailure(call: Call, e: IOException) {
                    // Обработка ошибки при отправке запроса
                    Log.e(TAG, "Server is unavailable: ${e.message}")
                    callback(false, e.message!!)
                }
            })
        }
    }

    private fun deletePhotoFile() {
        // Удаление файла с фотографией
        val file = File(currentPhotoPath)
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
            imageBase64 = convertImageToBase64(rotatedBitmap)
        }
    }
}

//пользователь подтвержадет даынне для отправка
//русские комменты
