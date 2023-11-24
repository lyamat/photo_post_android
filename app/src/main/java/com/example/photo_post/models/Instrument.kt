package com.example.photo_post.models

data class Instrument(
    val instrId: Long,
    @Transient
    val instrName: String,
    val instrQr: String,
    @Transient
    val instrProps: String,
    @Transient
    var instrAmount: Double,
    @Transient
    val instrUnits: String,
    @Transient
    val isAddToCartEnabled: Boolean = true
)
