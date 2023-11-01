package com.example.photo_post

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.os.Build
import android.widget.ImageView
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.os.Environment
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.widget.Button
import android.widget.AdapterView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager

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

private val TAG = "PhotoFragment"

private val CAMERA_PERMISSION_REQUEST_CODE = 1001
private val REQUEST_IMAGE_CAPTURE = 1

private var currentPhotoPath: String? = null
private var imageBase64: String = ""
private var selectedProjectName: String = ""
private var projectListIds: MutableList<Int> = mutableListOf()
private var selectedProjectId: String = ""
private lateinit var projectAdapter: ArrayAdapter<String>

class PhotoViewModel : ViewModel() {
    var commentText: String = ""
    var rotatedBitmap: Bitmap? = null
    var currentPhotoPath: String? = null
}


class PhotoFragment : Fragment() {
    private var projectList: List<Project> = emptyList()
    private var projectNames: List<String> = emptyList()

    private lateinit var viewModel: PhotoViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity()).get(PhotoViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_photo, container, false)

        val photoImageView = view.findViewById<ImageView>(R.id.photoImageView)

        if (viewModel.rotatedBitmap != null) {
            photoImageView.setImageBitmap(viewModel.rotatedBitmap)
        } else {
            photoImageView.setImageResource(R.drawable.no_picture)
        }


        val projectSpinner = view.findViewById<Spinner>(R.id.projectSpinner)
        projectAdapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item)
        projectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        projectSpinner.adapter = projectAdapter

        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val savedProjectNames = sharedPrefs.getStringSet("projectNames", emptySet())?.toMutableList() ?: mutableListOf()
//        val projectAdapterData = savedProjectNames?.toMutableList() ?: mutableListOf()
        val savedSelectedProjectName = sharedPrefs.getString("selectedProjectName", "")

        val projectListIdsSet = sharedPrefs.getStringSet("projectListIds", emptySet())
        projectListIds = projectListIdsSet?.map { it.toInt() }?.toMutableList() ?: mutableListOf()


        projectAdapter.clear()
        projectAdapter.addAll(savedProjectNames)
        projectAdapter.notifyDataSetChanged()

        val spinnerPosition = projectAdapter.getPosition(savedSelectedProjectName)
        projectSpinner.setSelection(spinnerPosition)

        projectSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedProjectName = parent.getItemAtPosition(position) as String
                sharedPrefs.edit().putString("selectedProjectName", selectedProjectName).apply()
                if(projectListIds.isNotEmpty()) {
                    selectedProjectId = projectListIds[position].toString()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }

        val commentEditText = view.findViewById<EditText>(R.id.commentEditText)
        commentEditText.setText(viewModel.commentText)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            commentEditText.showSoftInputOnFocus = true
        }

        view.findViewById<ImageView>(R.id.button_camera).setOnClickListener {
            checkCameraPermission()
        }

        view.findViewById<ImageView>(R.id.button_refresh).setOnClickListener {
            updateProjectListInFragment()
        }

        view.findViewById<Button>(R.id.sendToServerButton).setOnClickListener {
            showDialog()
        }

        return view
    }

    override fun onPause() {
        super.onPause()
        viewModel.commentText = view?.findViewById<EditText>(R.id.commentEditText)?.text.toString()
    }

    public fun updateProjectListInFragment() {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext())

        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        val isConnected = networkInfo != null && networkInfo.isConnected && networkInfo.type == ConnectivityManager.TYPE_WIFI

        if (isConnected) {
            getProjectList { newProjectList, message ->
                val file = File(requireContext().filesDir, "projects.txt")
                val sb = StringBuilder()
                projectList = newProjectList

                if (newProjectList.isNotEmpty()) {
                    projectListIds.clear()
                    for (project in newProjectList) {
                        sb.append("${project.projectId}: ${project.projectName}\n")
                        projectListIds += project.projectId
                    }
                    val projectListIdsSet = projectListIds.map { it.toString() }.toSet()
                    sharedPrefs.edit().putStringSet("projectListIds", projectListIdsSet).apply()
                    file.writeText(sb.toString())
                } else {
                    file.writeText("")
                }

                if (message == "Project list address is empty") {
                    // Handle empty address
                } else if (message.isEmpty()) {
                    requireActivity().runOnUiThread {
                        projectNames = projectList.map { it.projectName }

                        projectAdapter.clear()
                        projectAdapter.addAll(projectNames)
                        projectAdapter.notifyDataSetChanged()

                        val editor = sharedPrefs.edit()
                        editor.putStringSet("projectNames", projectNames.toSet()).apply()
                        editor.putString("selectedProjectName", projectNames[0]).apply()
                        if (newProjectList.isNotEmpty()) {
                            requireActivity().runOnUiThread {
                                Toast.makeText(
                                    requireContext(),
                                    "Success. Received ${projectAdapter.count} projects.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getProjectList(callback: (List<Project>, String) -> Unit) {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireActivity())

        val server_address_post = sharedPrefs.getString("server_address_post", "")

        val change_password = sharedPrefs.getString("change_password", "")

        if (TextUtils.isEmpty(server_address_post)) {
            if (isAdded) {
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Settings. Project list address is empty", Toast.LENGTH_SHORT).show()
                    callback(emptyList(), "Project list address is empty")
                }
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

    private fun checkCameraPermission() {
        val activity = requireActivity()
        if (ContextCompat.checkSelfPermission(activity, android.Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(android.Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        } else {
            dispatchTakePictureIntent()
        }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        val activity = requireActivity()
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent()
            } else {
                Toast.makeText(activity, "Разрешение на использование камеры отклонено", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun dispatchTakePictureIntent() {
        val activity = requireActivity()
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            val photoFile: File? = try {
                createImageFile()
            } catch (ex: IOException) {
                null
            }
            photoFile?.also {
                val photoURI = FileProvider.getUriForFile(activity,
                    "com.example.photo_post.fileprovider",
                    photoFile)
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    private fun createImageFile(): File? {
        val activity = requireActivity()
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
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
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == AppCompatActivity.RESULT_OK) {
            val photoImageView = view?.findViewById<ImageView>(R.id.photoImageView)
            val rotatedBitmap = getRotatedImageWithExif(currentPhotoPath!!)
            photoImageView?.setImageBitmap(rotatedBitmap)

            CoroutineScope(Dispatchers.IO).launch {
                imageBase64 = convertImageToBase64(rotatedBitmap)
                deletePhotoFile()
            }

            val photoViewModel = ViewModelProvider(requireActivity()).get(PhotoViewModel::class.java)
            photoViewModel.rotatedBitmap = rotatedBitmap
            photoViewModel.currentPhotoPath = currentPhotoPath
        }
    }

    private fun showDialog() {
        val commentEditText = view?.findViewById<EditText>(R.id.commentEditText)
        val comment = commentEditText?.text.toString()
        if (imageBase64.isNotEmpty()) {
            val builder = AlertDialog.Builder(requireActivity())
            builder.setTitle("Отправить на сервер?")
            builder.setMessage("Комментарий: $comment\nВыбранный проект: $selectedProjectName")
            builder.setPositiveButton("Да") { dialog, which ->
                view?.findViewById<Button>(R.id.sendToServerButton)?.isEnabled = false
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
            requireActivity().runOnUiThread {
                Toast.makeText(requireActivity(), "No image to send", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendToServer() {

        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        val isConnected = networkInfo != null && networkInfo.isConnected && networkInfo.type == ConnectivityManager.TYPE_WIFI

        if (isConnected) {
            val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireActivity())
            val server_address_post = sharedPrefs.getString("server_address_post", "")
            val change_password = sharedPrefs.getString("change_password", "")
            val projectNames = sharedPrefs.getStringSet("projectNames", emptySet())

            if (TextUtils.isEmpty(change_password)) {
                requireActivity().runOnUiThread {
                    Toast.makeText(
                        requireActivity(),
                        "Settings. Password is empty",
                        Toast.LENGTH_SHORT
                    ).show()
                    view?.findViewById<Button>(R.id.sendToServerButton)?.isEnabled = true
                }
            }
            else {
                if (TextUtils.isEmpty(server_address_post)) {
                    requireActivity().runOnUiThread {
                        Toast.makeText(
                            requireActivity(),
                            "Settings. Server address is empty",
                            Toast.LENGTH_SHORT
                        ).show()
                        view?.findViewById<Button>(R.id.sendToServerButton)?.isEnabled = true
                    }
                } else {
                    val commentEditText = view?.findViewById<EditText>(R.id.commentEditText)
                    val comment = commentEditText?.text.toString()

                    if (!projectNames.isNullOrEmpty()) {
                        if (!TextUtils.isEmpty(server_address_post)) {
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
                                            requireActivity().runOnUiThread {
                                                view?.findViewById<Button>(R.id.sendToServerButton)?.isEnabled =
                                                    true
                                                Toast.makeText(
                                                    requireActivity(),
                                                    logMessage,
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }

                                            Log.e(TAG, logMessage)
                                        } else {
                                            val logMessage = "Error sending request"
                                            requireActivity().runOnUiThread {
                                                view?.findViewById<Button>(R.id.sendToServerButton)?.isEnabled =
                                                    true
                                                Toast.makeText(
                                                    requireActivity(),
                                                    "Error sending request",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                            Log.e(TAG, "$logMessage: ${response.message}")
                                        }
                                    }

                                    override fun onFailure(call: Call, e: IOException) {
                                        requireActivity().runOnUiThread {
                                            view?.findViewById<Button>(R.id.sendToServerButton)?.isEnabled =
                                                true
                                            Toast.makeText(
                                                requireActivity(),
                                                "Failure sending request. Check url",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        Log.e(TAG, "Error sending request: ${e.message}")
                                    }
                                })

                            }
                        }
                    }
                }
            }
        }
        else {
            requireActivity().runOnUiThread {
                Toast.makeText(requireActivity(), "Check Wi-fi connection", Toast.LENGTH_SHORT).show()
                view?.findViewById<Button>(R.id.sendToServerButton)?.isEnabled = true
            }
        }

    }

}