package com.example.firstapp.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl("http://192.168.200.196:5021/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
