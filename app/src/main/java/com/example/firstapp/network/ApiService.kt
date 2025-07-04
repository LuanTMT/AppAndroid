package com.example.firstapp.network

import retrofit2.http.GET
import com.example.firstapp.data.User

interface ApiService {
    @GET("users")
    suspend fun getUsers(): List<User>
}
