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
import com.example.photo_post.server.NetworkHelper

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
        val savedProjectNames = sharedPrefs.getStringSet("projectNames", emptySet())?.toMutableList() ?: mutableListOf()
//        val savedSelectedProjectName = sharedPrefs.getString("selectedProjectName", "")

        val projectSpinner = view.findViewById<Spinner>(R.id.projectSpinner)
        projectAdapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item, savedProjectNames)
        projectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        projectSpinner.adapter = projectAdapter

        projectSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                sharedPrefs.edit().putString("selectedProjectName", parent.selectedItem as String).apply()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }

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

    override fun onResume() {
        super.onResume()
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val projectNames = sharedPrefs.getStringSet("projectNames", emptySet()) ?: emptySet()

        projectAdapter.clear()
        if(projectNames.isNotEmpty()) {
            projectAdapter.addAll(projectNames)
            sharedPrefs.edit().putString("selectedProjectName", projectAdapter.getItem(0)).apply()
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
        val projectNames = sharedPrefs.getStringSet("projectNames", emptySet()) ?: emptySet()
        val selectedProjectName = sharedPrefs.getString("selectedProjectName", "") ?: ""

        val commentEditText = view?.findViewById<EditText>(R.id.commentEditText)
        val comment = commentEditText?.text.toString()


        if (TextUtils.isEmpty(change_password)) {
            requireActivity().runOnUiThread {
                Toast.makeText(
                    requireActivity(),
                    "Settings. Password is empty",
                    Toast.LENGTH_SHORT
                ).show()
                view?.findViewById<Button>(R.id.sendToServerButton)?.isEnabled = true
            }
        } else {
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
                if (projectNames.isEmpty()) {
                    requireActivity().runOnUiThread {
                        Toast.makeText(
                            requireActivity(),
                            "Empty project list",
                            Toast.LENGTH_SHORT
                        ).show()
                        view?.findViewById<Button>(R.id.sendToServerButton)?.isEnabled = true
                    }
                } else {
                    if (imageBase64.isEmpty()) {
                        requireActivity().runOnUiThread {
                            Toast.makeText(
                                requireActivity(),
                                "No image to send",
                                Toast.LENGTH_SHORT
                            ).show()
                            view?.findViewById<Button>(R.id.sendToServerButton)?.isEnabled = true
                        }
                    } else {
                        val builder = AlertDialog.Builder(requireActivity())
                        builder.setTitle("Отправить на сервер?")
                        builder.setMessage("Комментарий: $comment\nВыбранный проект: $selectedProjectName")
                        builder.setPositiveButton("Да") { dialog, which ->
                            dialog.dismiss()
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
                }
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
            val projectNames = sharedPrefs.getStringSet("projectNames", emptySet()) ?: emptySet()
            val selectedProjectName = sharedPrefs.getString("selectedProjectName", "") ?: ""
//            val selectedProjectId = sharedPrefs.getString("selectedProjectId", "") ?: ""



                            val commentEditText = view?.findViewById<EditText>(R.id.commentEditText)
                            val comment = commentEditText?.text.toString()

                            if (!projectNames.isNullOrEmpty()) {
                                if (!TextUtils.isEmpty(server_address_post)) {
                                    if ("imageBase64".isNotEmpty()) {
                                        val client = OkHttpClient()

                                        val requestBody: RequestBody = FormBody.Builder()
                                            .add("request_command", "upload_image")
                                            .add("password", change_password!!)
                                            .add("imageBase64", imageBase64)
                                            .add("project_name", selectedProjectName)
//                                    .add("project_id", selectedProjectId)
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

//

        }
        else {
            requireActivity().runOnUiThread {
                Toast.makeText(requireActivity(), "Check Wi-fi connection", Toast.LENGTH_SHORT).show()
                view?.findViewById<Button>(R.id.sendToServerButton)?.isEnabled = true
            }
        }

    }

}