package com.example.firstapp.data

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    @SerializedName(value = "success", alternate = ["ok", "status"]) val success: Boolean = false,
    @SerializedName(value = "message", alternate = ["msg", "error"]) val message: String? = null,
    @SerializedName(value = "data", alternate = ["result", "record", "attendance", "item"]) val data: T? = null,
    @SerializedName(value = "timestamp", alternate = ["time"]) val timestamp: String? = null
)


