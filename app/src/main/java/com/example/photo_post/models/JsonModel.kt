package com.example.photo_post.models

data class JsonModel(
    val cart_id: Long,
    val cart_name: String,
    val cart_type: Long,
    val process_id: Long,

    val instr_id: Long,
    val amount_in_cart: Double,
    val instr_name: String,
    val instr_props: String,
    val instr_amount: Double,
    val instr_units: String,
    val instr_qr: String
)



