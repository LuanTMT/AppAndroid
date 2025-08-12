// User.kt
package com.example.firstapp.data

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName(value = "_id", alternate = ["id", "userId"]) val id: String? = null,
    @SerializedName(value = "name", alternate = ["fullName"]) val name: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("position") val position: String? = null,
    @SerializedName(value = "accountNumber", alternate = ["account_number"]) val accountNumber: String? = null,
    @SerializedName(value = "bankName", alternate = ["bank_name"]) val bankName: String? = null,
    @SerializedName("avatar") val avatar: String? = null
)