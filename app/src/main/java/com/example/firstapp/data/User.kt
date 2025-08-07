// User.kt
package com.example.firstapp.data

data class User(
    val id: String,
    val name: String,
    val email: String,
    val position: String? = null,
    val accountNumber: String? = null,
    val bankName: String? = null,
    val avatar: String? = null
)