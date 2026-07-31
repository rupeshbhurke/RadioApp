package com.example.radioapp.model

import kotlinx.serialization.Serializable

@Serializable
data class Station(
    val id: String,
    val name: String,
    val streamUrl: String,
    val city: String,
    val language: String,
    val logoUrl: String = ""
)
