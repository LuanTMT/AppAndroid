// ApiService.kt
package com.example.firstapp.network

import com.example.firstapp.data.User
import com.example.firstapp.data.ApiResponse
import com.example.firstapp.data.AttendanceRecord
import com.example.firstapp.data.TodayAttendance
import com.example.firstapp.data.AttendanceRequest
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @GET("users")
    suspend fun getUsers(): List<User>

    @GET("attendance/today")
    suspend fun getTodayAttendance(): ApiResponse<TodayAttendance>

    @POST("attendance")
    suspend fun submitAttendance(@Body attendance: AttendanceRequest): ApiResponse<AttendanceRecord>

    @GET("users/{id}")
    suspend fun getUserDetail(@Path("id") id: String): ApiResponse<User>

    @PUT("users/{id}")
    suspend fun updateUser(@Path("id") id: String, @Body user: User): ApiResponse<User>

    @POST("users/change-password")
    suspend fun changePassword(@Body body: Map<String, String>): Response<Unit>

    @Multipart
    @POST("users/{id}/avatar")
    suspend fun uploadAvatar(
        @Path("id") id: String,
        @Part image: MultipartBody.Part
    ): Response<Unit>
}
