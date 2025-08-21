package com.example.firstapp.data

import java.util.Date
import com.google.gson.annotations.SerializedName

// Dữ liệu record trả về từ server
data class AttendanceRecord(
    @SerializedName(value = "_id", alternate = ["id"]) val _id: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName(value = "image", alternate = ["avatar", "imageUrl", "image_url", "imagePath", "image_path", "photo", "photoUrl"]) val image: String? = null,
    // Address-like string if server returns text instead of object
    @SerializedName(value = "address", alternate = ["place", "locationText", "location_text", "position"]) val location: String? = null,
    @SerializedName(value = "timestamp", alternate = ["time", "createdAt", "created_at", "date"]) val timestamp: String? = null,
    @SerializedName(value = "type", alternate = ["checkType", "attendanceType", "action"]) val type: String? = null,
    @SerializedName(value = "status", alternate = ["state"]) val status: String? = null
)

// Shape thực tế của API /attendance/today theo log: data { email, checkIn { _id, image } }, timestamp ngoài
data class TodayAttendance(
    @SerializedName("email") val email: String? = null,
    @SerializedName("checkIn") val checkIn: TodayCheck? = null,
    @SerializedName("checkOut") val checkOut: TodayCheck? = null
)

data class TodayCheck(
    @SerializedName("_id") val id: String? = null,
    @SerializedName("image") val image: String? = null,
    @SerializedName(value = "location", alternate = ["address", "place", "locationText", "location_text"]) val location: String? = null,
    @SerializedName(value = "timestamp", alternate = ["time", "createdAt", "created_at", "date"]) val timestamp: String? = null
)

// Dữ liệu gửi lên server
data class AttendanceRequest(
    val image: String,
    val location: String,
    val type: String // "check_in" | "check_out"
)

enum class AttendanceType {
    CHECK_IN,
    CHECK_OUT
}