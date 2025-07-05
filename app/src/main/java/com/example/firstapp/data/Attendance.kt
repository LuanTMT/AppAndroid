package com.example.firstapp.data

import java.util.Date

data class Attendance(
    val id: String = "",
    val userId: String = "",
    val timestamp: Date = Date(),
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String = "",
    val imagePath: String = "",
    val type: AttendanceType = AttendanceType.CHECK_IN
)

enum class AttendanceType {
    CHECK_IN,
    CHECK_OUT
} 