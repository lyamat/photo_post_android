package com.example.photo_post.models

class Cart(
    var cartId: Long,
    var cartName: String,
    var cartType: String,
    val processId: Long,
    val cartItems: MutableList<CartItem> = mutableListOf(),
)



