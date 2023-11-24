package com.example.photo_post.server

import android.content.Context
import android.net.ConnectivityManager
import android.text.TextUtils
import android.util.Log
import androidx.preference.PreferenceManager
import com.example.photo_post.SharedViewModel
import com.example.photo_post.models.Cart
import com.example.photo_post.models.CartItem
import com.example.photo_post.models.Instrument
import com.example.photo_post.models.JsonModel
import com.example.photo_post.models.JsonModelGetCartByProcess
import com.example.photo_post.models.Process
import com.example.photo_post.models.Project
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import java.io.IOException
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.concurrent.TimeUnit

class NetworkHelper(private val context: Context) {

    fun updateProjectList(callback: (List<Project>, String) -> Unit) {
        getProjectList { projectList, errorMessage ->
            if (errorMessage.isEmpty()) {
                callback(projectList, "Success updated project list")
            }
            else {
                Log.e("updateProjectList", "${errorMessage}")
                callback(emptyList(), errorMessage)
            }
        }
    }

    private fun getProjectList(callback: (List<Project>, String) -> Unit) {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        val server_address_post = sharedPrefs.getString("server_address_post", "")
        val change_password = sharedPrefs.getString("change_password", "")

        if (TextUtils.isEmpty(server_address_post)) {
            callback(emptyList(), "Server url address is empty")
        } else {
            val client = OkHttpClient()

            val requestBody: RequestBody = FormBody.Builder()
                .add("request_command", "get_project_list")
                .add("password", change_password!!)
                .build()

            val request: Request = Request.Builder()
                .url("$server_address_post")
                .post(requestBody)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        val projectList = parseProjectList(responseBody)
                        if (projectList.isNotEmpty()) {
                            callback(projectList, "")
                        }
                        else{
                            callback(emptyList(), "Response have empty project list")
                        }
                    } else {
                        val logMsg = "Response unsuccessful, getting project list"
                        Log.e("onResponse", "$logMsg: ${response.message}")
                        callback(emptyList(), "$logMsg: ${response.message}")
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    Log.e("onFailure", "Failure, getting project list: ${e.message}")
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
                    val projectId = jsonObject.getLong("id")
                    val projectName = jsonObject.getString("name")
                    projectList.add(Project(projectId, projectName))
                }
            } catch (e: JSONException) {
                Log.e("parseProjectList", "Error parsing project list: ${e.message}")
            }
        }
        return projectList
    }

    fun getProcessesByProject(projectName: String, callback: (List<Process>, String) -> Unit) {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        val server_address_post = sharedPrefs.getString("server_address_post", "")
        val change_password = sharedPrefs.getString("change_password", "")

        if (TextUtils.isEmpty(server_address_post)) {
            callback(emptyList(), "Server url address is empty")
        } else {
            val client = OkHttpClient()

            val requestBody: RequestBody = FormBody.Builder()
                .add("request_command", "get_processes_by_project")
                .add("password", change_password!!)
                .add("project_name", projectName)
                .build()

            val request: Request = Request.Builder()
                .url("$server_address_post")
                .post(requestBody)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        val processList = parseProcessList(responseBody)
                        if (processList.isNotEmpty()) {
                            callback(processList, "")
                        }
                        else{
                            callback(emptyList(), "Response have empty process list")
                        }
                    } else {
                        val logMsg = "Response unsuccessful, getting process list"
                        Log.e("onResponse", "$logMsg: ${response.message}")
                        callback(emptyList(), "$logMsg: ${response.message}")
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    Log.e("onFailure", "Failure, getting process list: ${e.message}")
                    callback(emptyList(), e.message ?: "")
                }
            })
        }
    }

    private fun parseProcessList(json: String?): List<Process> {
        val processList = mutableListOf<Process>()
        json?.let {
            try {
                val jsonArray = JSONArray(it)
                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    val processId = jsonObject.getLong("process_id")
                    val processName = jsonObject.getString("process_name")
                    processList.add(Process(processId, processName))
                }
            } catch (e: JSONException) {
                Log.e("parseProcessList", "Error parsing process list: ${e.message}")
            }
        }
        return processList
    }

    fun checkServerAvailability(callback: (Boolean, String) -> Unit) {

        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        val server_address = sharedPrefs.getString("server_address_post", "")

        if (TextUtils.isEmpty(server_address)) {
                callback(false, "Settings. Address is empty")
        } else {
            val client = OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build()

            val request: Request = Request.Builder()
                .url("$server_address")
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    Log.d("NetworkHelper", "The server is available")
                    callback(true, "")
                }

                override fun onFailure(call: Call, e: IOException) {
                    Log.e("NetworkHelper", "Server is unavailable: ${e.message}")
                        callback(false, e.message!!)
                }
            })
        }
    }

    fun uploadCart(viewModel: SharedViewModel, callback: (Boolean, String) -> Unit) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        val isConnected =
            networkInfo != null && networkInfo.isConnected && networkInfo.type == ConnectivityManager.TYPE_WIFI

        if (isConnected) {
            checkServerAvailability { isAvailable, errorMessage ->
                if (isAvailable) {
                    val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
                    val server_address_post = sharedPrefs.getString("server_address_post", "")
                    val change_password = sharedPrefs.getString("change_password", "")

                    if (!TextUtils.isEmpty(server_address_post)) {
                        if (!TextUtils.isEmpty(change_password)) {
                            val gson = GsonBuilder()
                                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                                .create()
                            val cartJson = gson.toJson(viewModel.currentCart)

                            val client = OkHttpClient()

                            val requestBody: RequestBody = FormBody.Builder()
                                .add("request_command", "upload_cart")
                                .add("password", change_password!!)
                                .add("cart", cartJson)
                                .build()

                            val request: Request = Request.Builder()
                                .url("$server_address_post")
                                .post(requestBody)
                                .build()

                            client.newCall(request).enqueue(object : Callback {
                                override fun onResponse(call: Call, response: Response) {
                                    if (response.isSuccessful) {
                                        callback(true, "Successfully sent to the server.")
                                    } else {
                                        val logMsg = "Response unsuccessful, uploading cart"
                                        Log.e("onResponse", "$logMsg: ${response.message}")
                                        callback(false, "$logMsg: ${response.message}")
                                    }
                                }

                                override fun onFailure(call: Call, e: IOException) {
                                    Log.e("onFailure", "Failure, uploading cart: ${e.message}")
                                    callback(false, e.message ?: "")
                                }
                            })

                        }
                        else {
                            callback(false, "Empty password")
                        }
                    }
                    else {
                        callback(false, "Empty server address")
                    }
                }
                else {
                    callback(false, errorMessage)
                }
            }
        }
        else {
            callback(false, "Check Wi-fi connection")
        }
    }

    fun getUserCarts(callback: (ArrayList<Cart>, String) -> Unit) {

        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        val isConnected =
            networkInfo != null && networkInfo.isConnected && networkInfo.type == ConnectivityManager.TYPE_WIFI

        if (isConnected) {
            checkServerAvailability { isAvailable, errorMessage ->
                if (isAvailable) {
                    val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
                    val server_address_post = sharedPrefs.getString("server_address_post", "")
                    val change_password = sharedPrefs.getString("change_password", "")

                    if (!TextUtils.isEmpty(server_address_post)) {
                        if(!TextUtils.isEmpty(change_password)) {
                            val client = OkHttpClient()

                            val requestBody: RequestBody = FormBody.Builder()
                                .add("request_command", "get_user_carts")
                                .add("password", change_password!!)
                                .build()

                            val request: Request = Request.Builder()
                                .url("$server_address_post")
                                .post(requestBody)
                                .build()

                            client.newCall(request).enqueue(object : Callback {
                                override fun onResponse(call: Call, response: Response) {
                                    if (response.isSuccessful) {
                                        val gson = Gson()
                                        val listType = TypeToken.getParameterized(
                                            List::class.java,
                                            JsonModel::class.java
                                        ).type
                                        val jsonModels: List<JsonModel> =
                                            gson.fromJson(response.body?.string(), listType)

                                        val carts: ArrayList<Cart> = arrayListOf()
                                        jsonModels.forEach { jsonModel ->
                                            var cart = carts.find { it.cartId == jsonModel.cart_id }

                                            if (cart == null) {
                                                cart = Cart(
                                                    jsonModel.cart_id,
                                                    jsonModel.cart_name,
                                                    jsonModel.cart_type,
                                                    0)
                                           carts.add(cart)
                                            }

                                            val instrument = Instrument(
                                                jsonModel.instr_id,
                                                jsonModel.instr_name,
                                                jsonModel.instr_qr,
                                                jsonModel.instr_props,
                                                jsonModel.instr_amount,
                                                jsonModel.instr_units
                                            )
                                            val cartItem = CartItem(instrument, jsonModel.amount_in_cart)

                                            cart.cartItems.add(cartItem)
                                        }
                                        callback(carts, "Success. Received ${carts.size} carts.")
                                    } else {
                                        val logMsg = "Response unsuccessful, getting carts"
                                        Log.e("onResponse", "$logMsg: ${response.message}")
                                        callback(ArrayList(), "$logMsg: ${response.message}")
                                    }
                                }

                                override fun onFailure(call: Call, e: IOException) {
                                    Log.e("onFailure", "Failure, carts: ${e.message}")
                                    callback(ArrayList(), e.message ?: "")
                                }
                            })
                        }
                        else {
                            callback(ArrayList(), "Empty password")
                        }
                    }
                    else {
                        callback(ArrayList(), "Empty server address")
                    }
                }
                else {
                    callback(ArrayList(), errorMessage)
                }
            }
        }
        else {
            callback(ArrayList(), "Check Wi-fi connection")
        }
    }

    fun getCartByProcess(processName: String, callback: (ArrayList<Cart>, String) -> Unit) {

        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        val isConnected =
            networkInfo != null && networkInfo.isConnected && networkInfo.type == ConnectivityManager.TYPE_WIFI

        if (isConnected) {
            checkServerAvailability { isAvailable, errorMessage ->
                if (isAvailable) {
                    val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
                    val server_address_post = sharedPrefs.getString("server_address_post", "")
                    val change_password = sharedPrefs.getString("change_password", "")

                    if (!TextUtils.isEmpty(server_address_post)) {
                        if(!TextUtils.isEmpty(change_password)) {
                            val client = OkHttpClient()

                            val requestBody: RequestBody = FormBody.Builder()
                                .add("request_command", "get_cart_by_process")
                                .add("password", change_password!!)
                                .add("process_name", processName)
                                .build()

                            val request: Request = Request.Builder()
                                .url("$server_address_post")
                                .post(requestBody)
                                .build()

                            client.newCall(request).enqueue(object : Callback {
                                override fun onResponse(call: Call, response: Response) {
                                    if (response.isSuccessful) {
                                        val gson = Gson()
                                        val listType = TypeToken.getParameterized(
                                            List::class.java,
                                            JsonModelGetCartByProcess::class.java
                                        ).type
                                        val jsonModels: List<JsonModelGetCartByProcess> =
                                            gson.fromJson(response.body?.string(), listType)

                                        val carts: ArrayList<Cart> = arrayListOf()
                                        jsonModels.forEach { jsonModelGetCartByProcess ->
                                            var cart = carts.find { it.cartId == jsonModelGetCartByProcess.cart_id }

                                            if (cart == null) {
                                                cart = Cart(
                                                    jsonModelGetCartByProcess.cart_id,
                                                    jsonModelGetCartByProcess.cart_name,
                                                    jsonModelGetCartByProcess.cart_type,
                                                    jsonModelGetCartByProcess.process_id)
                                                carts.add(cart)
                                            }

                                            val instrument = Instrument(
                                                jsonModelGetCartByProcess.instr_id,
                                                jsonModelGetCartByProcess.instr_name,
                                                jsonModelGetCartByProcess.instr_qr,
                                                jsonModelGetCartByProcess.instr_props,
                                                jsonModelGetCartByProcess.instr_amount,
                                                jsonModelGetCartByProcess.instr_units
                                            )
                                            val cartItem = CartItem(instrument, 0.0)

                                            cart.cartItems.add(cartItem)
                                        }
                                        if (carts.size > 0) {
                                            carts.subList(1, carts.size).clear()
                                            carts[0].cartId = 0
                                        }

                                        callback(carts, "Success. Received ${carts.size} carts.")
                                    } else {
                                        val logMsg = "Response unsuccessful, getting carts"
                                        Log.e("onResponse", "$logMsg: ${response.message}")
                                        callback(ArrayList(), "$logMsg: ${response.message}")
                                    }
                                }

                                override fun onFailure(call: Call, e: IOException) {
                                    Log.e("onFailure", "Failure, carts: ${e.message}")
                                    callback(ArrayList(), e.message ?: "")
                                }
                            })
                        }
                        else {
                            callback(ArrayList(), "Empty password")
                        }
                    }
                    else {
                        callback(ArrayList(), "Empty server address")
                    }
                }
                else {
                    callback(ArrayList(), errorMessage)
                }
            }
        }
        else {
            callback(ArrayList(), "Check Wi-fi connection")
        }
    }

    fun getPrevNextCart(prevOrNext: String, cartId: Long, callback: (ArrayList<Cart>, String) -> Unit) {

        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        val isConnected =
            networkInfo != null && networkInfo.isConnected && networkInfo.type == ConnectivityManager.TYPE_WIFI

        if (isConnected) {
            checkServerAvailability { isAvailable, errorMessage ->
                if (isAvailable) {
                    val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
                    val server_address_post = sharedPrefs.getString("server_address_post", "")
                    val change_password = sharedPrefs.getString("change_password", "")

                    if (!TextUtils.isEmpty(server_address_post)) {
                        if(!TextUtils.isEmpty(change_password)) {
                            val client = OkHttpClient()

                            val requestBody: RequestBody = FormBody.Builder()
                                .add("request_command", "get_prev_next_cart")
                                .add("password", change_password!!)
                                .add("prev_or_next", prevOrNext) //"prev" or "next"
                                .add("cart_id", cartId.toString())
                                .build()

                            val request: Request = Request.Builder()
                                .url("$server_address_post")
                                .post(requestBody)
                                .build()

                            client.newCall(request).enqueue(object : Callback {
                                override fun onResponse(call: Call, response: Response) {
                                    if (response.isSuccessful) {
                                        val gson = Gson()
                                        val listType = TypeToken.getParameterized(
                                            List::class.java,
                                            JsonModel::class.java
                                        ).type
                                        val jsonModels: List<JsonModel> =
                                            gson.fromJson(response.body?.string(), listType)

                                        if (jsonModels.isNotEmpty()) {
                                            val carts: ArrayList<Cart> = arrayListOf()
                                            var cart = Cart(
                                                jsonModels[0].cart_id,
                                                jsonModels[0].cart_name,
                                                jsonModels[0].cart_type,
                                                jsonModels[0].process_id
                                            )

                                            jsonModels.forEach { jsonModel ->
                                                val instrument = Instrument(
                                                    jsonModel.instr_id,
                                                    jsonModel.instr_name,
                                                    jsonModel.instr_qr,
                                                    jsonModel.instr_props,
                                                    jsonModel.instr_amount,
                                                    jsonModel.instr_units
                                                )
                                                val cartItem = CartItem(instrument, jsonModel.amount_in_cart)

                                                cart.cartItems.add(cartItem)
                                            }
                                            carts.add(cart)
                                            callback(carts, "Success. Received ${carts.size} boxes.")
                                        } else {
                                            callback(ArrayList(), "Response have no box")
                                        }
                                    } else {
                                        val logMsg = "Response unsuccessful, getting boxes"
                                        Log.e("onResponse", "$logMsg: ${response.message}")
                                        callback(ArrayList(), "$logMsg: ${response.message}")
                                    }
                                }

                                override fun onFailure(call: Call, e: IOException) {
                                    Log.e("onFailure", "Failure, carts: ${e.message}")
                                    callback(ArrayList(), e.message ?: "")
                                }
                            })

                        }
                        else {
                            callback(ArrayList(), "Empty password")
                        }
                    }
                    else {
                        callback(ArrayList(), "Empty server address")
                    }
                }
                else {
                    callback(ArrayList(), errorMessage)
                }
            }
        }
        else {
            callback(ArrayList(), "Check Wi-fi connection")
        }
    }
}

