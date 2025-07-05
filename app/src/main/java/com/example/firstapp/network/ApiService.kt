package com.example.firstapp.network

import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Body
import com.example.firstapp.data.User
import com.example.firstapp.data.Attendance

interface ApiService {
    @GET("users")
    suspend fun getUsers(): List<User>
    
    @POST("attendance")
    suspend fun submitAttendance(@Body attendance: Attendance): Attendance
}
