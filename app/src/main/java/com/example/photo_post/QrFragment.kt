package com.example.photo_post

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.photo_post.models.Cart
import com.example.photo_post.models.CartItem
import com.example.photo_post.models.Instrument
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

private val REQUEST_CODE_SCANNER = 2001

private val TAG = "QRFragment"

class SharedViewModel : ViewModel() {
    var currentCart: Cart = Cart(1,"0909","Current cart") // generate random Cart id, name+cur_time
    var cartListFromServer: ArrayList<Cart> = ArrayList()
    val instruments: MutableList<Instrument> = mutableListOf()

    val cartListFromServerLiveData = MutableLiveData<List<Cart>>()


    fun addToCart(instrument: Instrument, quantity: Double) {
        val cartItem = CartItem(instrument, quantity)
        currentCart.cartItems.add(cartItem)
    }
}


class QrFragment : Fragment() {
    private lateinit var viewModel: SharedViewModel
    private lateinit var instrumentRecyclerView: RecyclerView
    private lateinit var adapter: InstrumentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_qr, container, false)

        instrumentRecyclerView = view.findViewById<RecyclerView>(R.id.instrumentRecyclerView)
        instrumentRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = InstrumentAdapter(viewModel.instruments, viewModel)
        instrumentRecyclerView.adapter = adapter

        view.findViewById<ImageView>(R.id.button_qr).setOnClickListener {
            val intent = Intent(requireContext(), ScannerActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE_SCANNER)
        }
        return view
    }


    override fun onPause() {
        super.onPause()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SCANNER && resultCode == Activity.RESULT_OK) {
            val qrCodeResult = data?.getStringExtra("qr_code_result")
            if (qrCodeResult != null) {
                getQrInfo(qrCodeResult) { qrInfoJson, message ->
                    if (qrInfoJson != null && isJSONValid(qrInfoJson)) {
                        if (!TextUtils.isEmpty(qrInfoJson)) {
                            val jsonObject = JSONObject(qrInfoJson)
                            if (jsonObject.has("instr_id") && jsonObject.has("instr_qr") && jsonObject.has("instr_name") && jsonObject.has(
                                    "instr_props"
                                )
                            ) {
                                val jsonObject = JSONObject(qrInfoJson)
                                val instrId = jsonObject.getString("instr_id").toInt()
                                val instrQr = jsonObject.getString("instr_qr")
                                val instrName = jsonObject.getString("instr_name")
                                val instrProps =
                                    jsonObject.getString("instr_props")
                                val instrument =
                                    Instrument(
                                        instrId,
                                        instrName,
                                        instrQr,
                                        instrProps
                                    )
                                val existingInstrument =
                                    viewModel.instruments.find { it.instrQr == instrQr }

                                if (existingInstrument != null) {
                                    val builder = AlertDialog.Builder(requireContext())
                                    builder.setTitle("A tool with such a QR code already exists")
                                    builder.setMessage("Do you want to replace it??")

                                    builder.setPositiveButton("Yes") { dialog, _ ->
                                        val index =
                                            viewModel.instruments.indexOf(existingInstrument)
                                        viewModel.instruments[index] = instrument
                                        adapter.notifyItemChanged(index)
                                        dialog.dismiss()
                                    }
                                    builder.setNegativeButton("No") { dialog, _ -> dialog.cancel() }

                                    builder.show()
                                } else {
                                    adapter.addInstrument(instrument)
                                }
                            } else {
                                requireActivity().runOnUiThread {
                                    Toast.makeText(
                                        requireActivity(),
                                        "Response (json) must contain:\ninstr_id, instr_qr, instr_name, instr_props",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }

                        } else {
                            requireActivity().runOnUiThread {
                                Toast.makeText(
                                    requireActivity(),
                                    "Get empty response body from server",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                    else {
                        requireActivity().runOnUiThread {
                            Toast.makeText(
                                requireActivity(),
                                "Response from server not json serializable",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
            else {
                requireActivity().runOnUiThread {
                    Toast.makeText(
                        requireActivity(),
                        "Cannot get info for $qrCodeResult",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }


    private fun getQrInfo(instr_qr: String?, callback: (String?, String) -> Unit) {
        val activity = requireActivity()
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity)
        val server_address_post = sharedPrefs.getString("server_address_post", "no_server_address")

        val connectivityManager = activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        val isConnected = networkInfo != null && networkInfo.isConnected && networkInfo.type == ConnectivityManager.TYPE_WIFI

        if (isConnected) {
            val change_password = sharedPrefs.getString("change_password", "")
            val client = OkHttpClient()

            val requestBody: RequestBody = FormBody.Builder()
                .add("request_command", "get_instr_by_qr")
                .add("password", change_password!!)
                .add("instr_qr", instr_qr!!)
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
                            callback(responseBody, "")
                        }
                    } else {
                        val logMsg = "Response from server unsuccessful"
                        Log.e(TAG, "$logMsg: ${response.message}")
                        callback("", "$logMsg: ${response.message}")
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "Failure on request: ${e.message}")
                    callback("", e.message ?: "")
                }
            })
        }
    }

    fun isJSONValid(json: String): Boolean {
        try {
            JSONObject(json)
            return true
        } catch (e: JSONException) {
            return false
        }
    }
}