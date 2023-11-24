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
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager

import com.example.photo_post.image_work.convertImageToBase64
import com.example.photo_post.image_work.getRotatedImageWithExif
import com.example.photo_post.models.Project
import com.example.photo_post.server.NetworkHelper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

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

private const val TAG = "PhotoFragment"

private val REQUEST_IMAGE_CAPTURE = 1

private var currentPhotoPath: String? = null
private var imageBase64: String = ""

class PhotoViewModel : ViewModel() {
    var commentText: String = ""
    var rotatedBitmap: Bitmap? = null
    var currentPhotoPath: String? = null
}


class PhotoFragment : Fragment() {

    private lateinit var projectAdapter: ArrayAdapter<String>
    private lateinit var viewModel: PhotoViewModel
    private lateinit var sharedViewModel: SharedViewModel

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

        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext())

        val gson = Gson()
        val jsonProjects = sharedPrefs.getString("projects", "")
        val projects: List<Project> = if (jsonProjects?.isNotEmpty() == true) {
            val listType = TypeToken.getParameterized(List::class.java, Project::class.java).type
            gson.fromJson(jsonProjects, listType)
        } else {
            emptyList()
        }


        val projectNames = projects.map { it.projectName }

        val projectSpinner = view.findViewById<Spinner>(R.id.projectSpinner)
        projectAdapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item, projectNames)
        projectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        projectSpinner.adapter = projectAdapter

        val commentEditText = view.findViewById<EditText>(R.id.commentEditText)
        commentEditText.setText(viewModel.commentText)

        view.findViewById<ImageView>(R.id.button_camera).setOnClickListener {
            dispatchTakePictureIntent()
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)

        sharedViewModel.message.observe(viewLifecycleOwner, Observer { message ->
            if (message.isNotEmpty()) {
                Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
                sharedViewModel.message.postValue("")
            }
        })

        sharedViewModel.isButtonEnabled.observe(viewLifecycleOwner, Observer { isEnabled ->
            view?.findViewById<Button>(R.id.sendToServerButton)?.isEnabled = isEnabled
        })
    }


    override fun onResume() {
        super.onResume()
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext())

        val gson = Gson()
        val jsonProjects = sharedPrefs.getString("projects", "")
        val projects: List<Project> = if (jsonProjects?.isNotEmpty() == true) {
            val listType = TypeToken.getParameterized(List::class.java, Project::class.java).type
            gson.fromJson(jsonProjects, listType)
        } else {
            emptyList()
        }


        val projectNames = projects.map { it.projectName }

        projectAdapter.clear()
        if(projectNames.isNotEmpty()) {
            projectAdapter.addAll(projectNames)
        }
        else {
            projectAdapter.add("no projects available")
        }
        projectAdapter.notifyDataSetChanged()
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
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireActivity())

        val server_address_post = sharedPrefs.getString("server_address_post", "")
        val change_password = sharedPrefs.getString("change_password", "")

        val projectSpinner = view?.findViewById<Spinner>(R.id.projectSpinner)

        val gson = Gson()
        val jsonProjects = sharedPrefs.getString("projects", "")
        val projects: List<Project> = if (jsonProjects?.isNotEmpty() == true) {
            val listType = TypeToken.getParameterized(List::class.java, Project::class.java).type
            gson.fromJson(jsonProjects, listType)
        } else {
            emptyList()
        }

        val projectNames = projects.map { it.projectName }

        val selectedProjectName = projectSpinner?.selectedItem.toString()

        val commentEditText = view?.findViewById<EditText>(R.id.commentEditText)
        val comment = commentEditText?.text.toString()

        if (TextUtils.isEmpty(change_password)) {
                toastAndLog("Settings. Password is empty")
                sharedViewModel.setButtonEnabled(true)

        } else {
            if (TextUtils.isEmpty(server_address_post)) {
                toastAndLog("Settings. Server address is empty")
                sharedViewModel.setButtonEnabled(true)
            } else {
                if (projectNames.isEmpty()) {
                    toastAndLog("Empty project list")
                    sharedViewModel.setButtonEnabled(true)
                } else {
                    if (imageBase64.isEmpty()) {
                        toastAndLog("No image to send")
                        sharedViewModel.setButtonEnabled(true)
                    } else {
                        val builder = AlertDialog.Builder(requireActivity())
                        builder.setTitle("Send to server?")
                        builder.setMessage("Comment: $comment\nSelected project: $selectedProjectName")
                        builder.setPositiveButton("Да") { dialog, which ->
                            dialog.dismiss()
                            view?.findViewById<Button>(R.id.sendToServerButton)?.isEnabled = false
                            sendToServer()
                            Log.d(
                                TAG,
                                "Send to server: Photo, selected project - $selectedProjectName, comment - $comment"
                            )
                        }
                        builder.setNegativeButton("Cancel") { dialog, which ->
                            dialog.dismiss()
                        }
                        builder.show()
                    }
                }
            }
        }
    }


    private fun sendToServer() {

        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        val isConnected = networkInfo != null && networkInfo.isConnected && networkInfo.type == ConnectivityManager.TYPE_WIFI

        if (isConnected) {
            NetworkHelper(requireContext()).checkServerAvailability() { isServerAvailable, message ->
                if (isServerAvailable) {
                    val sharedPrefs =
                        PreferenceManager.getDefaultSharedPreferences(requireActivity())
                    val server_address_post = sharedPrefs.getString("server_address_post", "")
                    val change_password = sharedPrefs.getString("change_password", "")

                    val projectSpinner = view?.findViewById<Spinner>(R.id.projectSpinner)

                    val gson = Gson()
                    val jsonProjects = sharedPrefs.getString("projects", "")
                    val projects: List<Project> = if (jsonProjects?.isNotEmpty() == true) {
                        val listType = TypeToken.getParameterized(List::class.java, Project::class.java).type
                        gson.fromJson(jsonProjects, listType)
                    } else {
                        emptyList()
                    }

                    val projectNames = projects.map { it.projectName }

                    val selectedProjectName = projectSpinner?.selectedItem.toString()
                    val selectedProject = projects.find { it.projectName == selectedProjectName }
                    val selectedProjectId = selectedProject?.projectId.toString()


                    val commentEditText = view?.findViewById<EditText>(R.id.commentEditText)
                    val comment = commentEditText?.text.toString()

                    if (!projectNames.isNullOrEmpty()) {
                        if (!TextUtils.isEmpty(server_address_post)) {
                            if (imageBase64.isNotEmpty()) {
                                val client = OkHttpClient()

                                val requestBody: RequestBody = FormBody.Builder()
                                    .add("request_command", "upload_image")
                                    .add("password", change_password!!)
                                    .add("imageBase64", imageBase64)
                                    .add("project_id", selectedProjectId)
                                    .add("project_name", selectedProjectName)
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
                                            toastAndLog("Successfully sent to the server: ${response.message}")
                                            sharedViewModel.setButtonEnabled(true)
                                        } else {
                                            toastAndLog("Error sending request: ${response.message}")
                                            sharedViewModel.setButtonEnabled(true)
                                        }
                                    }

                                    override fun onFailure(call: Call, e: IOException) {
                                        toastAndLog("Failure sending request: ${e.message}")
                                        sharedViewModel.setButtonEnabled(true)
                                    }
                                })
                            }
                        }
                    }
                }
                else {
                    toastAndLog(message)
                    sharedViewModel.setButtonEnabled(true)
                }
            }
        }
        else {
            toastAndLog("Check Wi-fi connection")
            sharedViewModel.setButtonEnabled(true)
        }

    }

    private fun toastAndLog(message: String) {
        sharedViewModel.message.postValue(message)
        sharedViewModel.setButtonEnabled(true)
        Log.e(TAG, message)
    }

}