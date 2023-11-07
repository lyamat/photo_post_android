package com.example.photo_post.models

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

data class JsonModel(
    val cart_id: Int,
    val cart_name: String,
    val instr_id: Int,
    val cart_user_pass: String,
    val quantity: Double,
    val instr_name: String,
    val instr_props: String,
    val instr_qr: String
)



