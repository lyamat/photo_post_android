package com.example.photo_post.models

data class Instrument(
    val instrumentName: String,
    val instrumentQrCode: String,
    val instrumentProperties: List<String>
)
