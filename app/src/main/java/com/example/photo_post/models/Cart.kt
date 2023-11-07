package com.example.photo_post.models

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class Cart (
    val cartId: Int,
    val cartUserPass: String,
    var cartName: String,
    val cartItems: MutableList<CartItem> = mutableListOf(),
)



