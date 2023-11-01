package com.example.photo_post

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.example.photo_post.image_work.convertImageToBase64
import com.example.photo_post.image_work.getRotatedImageWithExif
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
import java.io.IOException

private val REQUEST_CODE_SCANNER = 2001

private val TAG = "QRFragment"

class QrViewModel : ViewModel() {
    var qrResultText: String = ""
}

class QrFragment : Fragment() {

    private lateinit var viewModel: QrViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity()).get(QrViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_qr, container, false)

        val qrResultEditText = view.findViewById<EditText>(R.id.qrResultEditText)
        qrResultEditText.setText(viewModel.qrResultText)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            qrResultEditText.showSoftInputOnFocus = true
        }

        view.findViewById<ImageView>(R.id.button_qr).setOnClickListener {
            val intent = Intent(requireContext(), ScannerActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE_SCANNER)
        }
        return view
    }

    override fun onPause() {
        super.onPause()
        viewModel.qrResultText = view?.findViewById<EditText>(R.id.qrResultEditText)?.text.toString()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val activity = requireActivity()
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SCANNER && resultCode == Activity.RESULT_OK) {
            val qrCodeResult = data?.getStringExtra("qr_code_result")
            if (qrCodeResult != null) {

                // Check if the QR code result is a URL
                if (URLUtil.isValidUrl(qrCodeResult)) {
                    AlertDialog.Builder(activity)
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
                    val qrResultEditText = view?.findViewById<EditText>(R.id.qrResultEditText)
                    qrResultEditText?.text = Editable.Factory.getInstance().newEditable(qrInfoJson)
                }
            }
        }
    }

    private fun getQrInfo(qrCode: String?, callback: (String?, String) -> Unit) {
        val activity = requireActivity()
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity)
        val server_address_post = sharedPrefs.getString("server_address_post", "")

        val connectivityManager = activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        val isConnected = networkInfo != null && networkInfo.isConnected && networkInfo.type == ConnectivityManager.TYPE_WIFI

        if (isConnected) {
            val change_password = sharedPrefs.getString("change_password", "")
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
                        activity.runOnUiThread {
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
        }
}