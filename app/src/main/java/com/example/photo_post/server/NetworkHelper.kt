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

    fun checkServerAvailability(callback: (Boolean, String) -> Unit) {

        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        val server_address = sharedPrefs.getString("server_address_post", "")

        if (TextUtils.isEmpty(server_address)) {
                callback(false, "Settings. Address is empty")
        } else {
            val client = OkHttpClient()
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

                    val gson = GsonBuilder()
                        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                        .create()
                    viewModel.currentCart.cartUserPass = change_password.toString()
                    val cartJson = gson.toJson(viewModel.currentCart)

                    if (!TextUtils.isEmpty(server_address_post)) {
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
                                }
                                else {
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
                        callback(false, "Empty server address")
                    }
                }
            }
        }
        else {
            callback(false, "Server is unavailable")
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

                    if (TextUtils.isEmpty(server_address_post)) {
                        callback(ArrayList(), "Empty server address")
                    } else {
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
                                    val listType = object : TypeToken<List<JsonModel>>() {}.type
                                    val jsonModels: List<JsonModel> = gson.fromJson(response.body?.string(), listType)


                                    val carts: ArrayList<Cart> = arrayListOf()
                                    jsonModels.forEach { jsonModel ->
                                        var cart = carts.find { it.cartId == jsonModel.cart_id }

                                        if (cart == null) {
                                            cart = Cart(jsonModel.cart_id, jsonModel.cart_user_pass, jsonModel.cart_name)
                                            carts.add(cart)
                                        }

                                        // Создаем инструмент и элемент корзины
                                        val instrument = Instrument(jsonModel.instr_id, jsonModel.instr_name, jsonModel.instr_qr, jsonModel.instr_props)
                                        val cartItem = CartItem(instrument, jsonModel.quantity)

                                        // Добавляем элемент в корзину
                                        cart.cartItems.add(cartItem)
                                    }
                                    callback(carts, "Success. Received ${carts.size} carts.")
                                }
                                else {
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
                }
            }
        }
        else {
            callback(ArrayList(), "Server is unavailable")
        }
    }
}

