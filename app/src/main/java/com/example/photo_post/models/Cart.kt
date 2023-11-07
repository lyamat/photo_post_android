package com.example.photo_post.models

class Cart (
    val cartId: Long,
    var cartUserPass: String,
    var cartName: String,
    val cartItems: MutableList<CartItem> = mutableListOf(),
)



