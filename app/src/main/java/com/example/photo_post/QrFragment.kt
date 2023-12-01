package com.example.photo_post

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.text.TextUtils
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
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
import java.util.concurrent.TimeUnit

private val REQUEST_CODE_SCANNER = 2001

private const val TAG = "QrFragment"

class SharedViewModel : ViewModel() {
    var currentCart: Cart = Cart(0,"Get materials", "0", 0)
    var cartListFromServer: ArrayList<Cart> = ArrayList()
    var isCurrentCartIsTemplate: Boolean = false
    val instruments: MutableList<Instrument> = mutableListOf()

    val cartListFromServerLiveData = MutableLiveData<List<Cart>>()

    var message = MutableLiveData<String>()

    private val _isButtonEnabled = MutableLiveData<Boolean>()
    val isButtonEnabled: LiveData<Boolean> get() = _isButtonEnabled
    fun setButtonEnabled(isEnabled: Boolean) {
        _isButtonEnabled.postValue(isEnabled)
    }
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

        view.findViewById<ImageView>(R.id.scanQrButton).setOnClickListener {
            val intent = Intent(requireContext(), ScannerActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE_SCANNER)
        }
        view.findViewById<ImageView>(R.id.inputQrButton).setOnClickListener {
            val builder = AlertDialog.Builder(it.context)
            builder.setTitle("Input QR:")

            val input = EditText(it.context)
            input.inputType = InputType.TYPE_CLASS_NUMBER
            input.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(20))

            input.setText("123")
            input.requestFocus()

            builder.setView(input)

            builder.setPositiveButton("Get item") { dialog, _ ->
                if (input.text.isNotEmpty()) {
                    val qrCodeResult = input.text.toString()
                    if (qrCodeResult != null) {
                        getQrInfo(qrCodeResult) { qrInfoJson, message ->
                            if (qrInfoJson != "") {
                                if (qrInfoJson != null && isJSONValid(qrInfoJson)) {
                                    if (!TextUtils.isEmpty(qrInfoJson)) {
                                        val jsonObject = JSONObject(qrInfoJson)
                                        val instrId: Long
                                        val instrQr: String
                                        val instrName: String
                                        val instrAmount: Double
                                        val instrProps: String
                                        val instrUnits: String

                                        if (jsonObject.has("instr_id") &&
                                            jsonObject.has("instr_qr") &&
                                            jsonObject.has("instr_name") &&
                                            jsonObject.has("instr_props") &&
                                            jsonObject.has("instr_amount") &&
                                            jsonObject.has("instr_units")
                                        ) {
                                            instrId = jsonObject.getString("instr_id").toLong()
                                            instrQr = jsonObject.getString("instr_qr")
                                            instrName = jsonObject.getString("instr_name")
                                            instrProps = jsonObject.getString("instr_props")
                                            instrAmount = jsonObject.getString("instr_amount").toDouble()
                                            instrUnits = jsonObject.getString("instr_units")
                                        } else if (jsonObject.has("info")) {
                                            instrId = 1
                                            instrQr = qrCodeResult
                                            instrName = qrCodeResult
                                            instrProps = jsonObject.getString("info")
                                            instrAmount = 1.0
                                            instrUnits = "un"
                                        }
                                        else {
                                            toastAndLog("Response (json) must contain:\n" +
                                                    "instr_id, instr_qr, instr_name, instr_props, instr_amount, instr_units \n" +
                                                    "or 'info'")
                                            return@getQrInfo
                                        }

                                        val instrument = Instrument(instrId,instrName,instrQr,instrProps,instrAmount,instrUnits,
                                            isAddToCartEnabled = !jsonObject.has("info")
                                        )
                                        val existingInstrument = viewModel.instruments.find { it.instrQr == instrQr }

                                        if (existingInstrument != null) {
                                            val builder = AlertDialog.Builder(requireContext())
                                            builder.setTitle("A tool with such a QR code already exists")
                                            builder.setMessage("Do you want to replace it??")

                                            builder.setPositiveButton("Yes") { dialog, _ ->
                                                val index = viewModel.instruments.indexOf(existingInstrument)
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
                                        toastAndLog("Get empty response body from server")
                                    }
                                } else {
                                    toastAndLog("Response from server not json serializable")
                                }
                            } else {
                                toastAndLog(message)
                            }
                        }
                    } else {
                        toastAndLog("Cannot get info for $qrCodeResult\"")
                    }
                }
            }
            builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

            val dialog = builder.create()
            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
            dialog.show()

            input.postDelayed({
                val imm = it.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
            }, 100)
        }
        return view
    }


    override fun onPause() {
        super.onPause()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)

        viewModel.message.observe(viewLifecycleOwner, Observer { message ->
            if (message.isNotEmpty()) {
                Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
                viewModel.message.postValue("")
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SCANNER && resultCode == Activity.RESULT_OK) {
            val qrCodeResult = data?.getStringExtra("qr_code_result")
            if (qrCodeResult != null) {
                getQrInfo(qrCodeResult) { qrInfoJson, message ->
                    if (qrInfoJson != "") {
                        if (qrInfoJson != null && isJSONValid(qrInfoJson)) {
                            if (!TextUtils.isEmpty(qrInfoJson)) {
                                val jsonObject = JSONObject(qrInfoJson)
                                val instrId: Long
                                val instrQr: String
                                val instrName: String
                                val instrProps: String
                                val instrAmount: Double
                                val instrUnits: String

                                if (jsonObject.has("instr_id") &&
                                    jsonObject.has("instr_qr") &&
                                    jsonObject.has("instr_name") &&
                                    jsonObject.has("instr_props") &&
                                    jsonObject.has("instr_amount") &&
                                    jsonObject.has("instr_units")
                                ) {
                                    instrId = jsonObject.getString("instr_id").toLong()
                                    instrQr = jsonObject.getString("instr_qr")
                                    instrName = jsonObject.getString("instr_name")
                                    instrProps = jsonObject.getString("instr_props")
                                    instrAmount = jsonObject.getString("instr_amount").toDouble()
                                    instrUnits = jsonObject.getString("instr_units")

                                } else if (jsonObject.has("info")) {
                                    instrId = 1
                                    instrQr = qrCodeResult
                                    instrName = qrCodeResult
                                    instrProps = jsonObject.getString("info")
                                    instrAmount = 1.0
                                    instrUnits = "un"
                                }
                                else {
                                    toastAndLog("Response (json) must contain:\n" +
                                            "instr_id, instr_qr, instr_name, instr_props, instr_amount, instr_units \n" +
                                            "or 'info'")
                                    return@getQrInfo
                                }

                                val instrument = Instrument(instrId,instrName,instrQr,instrProps,instrAmount,instrUnits,
                                                            isAddToCartEnabled = !jsonObject.has("info")
                                )
                                val existingInstrument = viewModel.instruments.find { it.instrQr == instrQr }

                                if (existingInstrument != null) {
                                    val builder = AlertDialog.Builder(requireContext())
                                    builder.setTitle("A tool with such a QR code already exists")
                                    builder.setMessage("Do you want to replace it??")

                                    builder.setPositiveButton("Yes") { dialog, _ ->
                                        val index = viewModel.instruments.indexOf(existingInstrument)
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
                                toastAndLog("Get empty response body from server")
                            }
                        } else {
                            toastAndLog("Response from server not json serializable")
                        }
                    } else {
                        toastAndLog(message)
                    }
                }
            } else {
                toastAndLog("Cannot get info for $qrCodeResult\"")
            }
        }
    }



    private fun getQrInfo(instr_qr: String?, callback: (String?, String) -> Unit) {
        val activity = requireActivity()
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity)
        val server_address_post = sharedPrefs.getString("server_address_post", "")

        val connectivityManager = activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        val isConnected = networkInfo != null && networkInfo.isConnected && networkInfo.type == ConnectivityManager.TYPE_WIFI

        if (isConnected) {
            if (!TextUtils.isEmpty(server_address_post)) {
                val change_password = sharedPrefs.getString("change_password", "")
                val client = OkHttpClient.Builder()
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .writeTimeout(5, TimeUnit.SECONDS)
                    .readTimeout(5, TimeUnit.SECONDS)
                    .build()

                val requestBody: RequestBody = FormBody.Builder()
                    .add("request_command", "get_instr_by_qr")
                    .add("password", change_password!!)
                    .add("instr_qr", instr_qr!!)
                    .build()

                val request: Request = Request.Builder()
                    .url("$server_address_post")
                    .post(requestBody)
                    .build()


                client.newCall(request).enqueue(object : Callback {
                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) {
                            activity.runOnUiThread {
                                val responseBody = response.body?.string()
                                if (responseBody != null) {
                                    callback(
                                        responseBody,
                                        "Response is success. Get ${responseBody.take(8)}... json"
                                    )
                                }
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
            else {
                callback("", "Empty server address")
            }
        }
        else {
            callback("", "Check Wi-fi connection")
        }
    }

    private fun isJSONValid(json: String): Boolean {
        return try {
            JSONObject(json)
            true
        } catch (e: JSONException) {
            false
        }
    }

    private fun toastAndLog(message: String) {
        viewModel.message.postValue(message)
        viewModel.setButtonEnabled(true)
        Log.e(TAG, message)
    }
}