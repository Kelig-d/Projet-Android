package com.example.projetandroid

data class Song(
    val name: String,
    val artist: String,
    val locked: Boolean? = null, // Champ optionnel
    val path: String? = null     // Champ optionnel
)
