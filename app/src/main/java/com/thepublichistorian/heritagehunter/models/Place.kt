package com.thepublichistorian.heritagehunter.models

import com.google.firebase.Timestamp

data class Place(
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val counter: Int = 0,
    val visitors: String = "",             // Feld für visitors
    val category: String = ""
)