package com.example.photo_post.models

data class JsonModelGetCartByProcess(
    val cart_id: String,
    val cart_name: String,
    val cart_type: String,

    val process_id: String,

    val instr_id: String,
    val instr_name: String,
    val instr_props: String,
    val instr_amount: String,
    val instr_units: String,
    val instr_qr: String
)



