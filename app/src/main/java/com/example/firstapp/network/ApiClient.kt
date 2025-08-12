package com.example.firstapp.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.util.Log

private class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = TokenProvider.token
        val requestBuilder = original.newBuilder()
        if (!token.isNullOrEmpty()) {
            Log.d("AuthInterceptor", "Adding Authorization header. Url=${original.url}")
            requestBuilder.addHeader("Authorization", "Bearer $token")
        } else {
            Log.w("AuthInterceptor", "No token present. Url=${original.url}")
        }
        val request = requestBuilder.build()
        Log.d("AuthInterceptor", "Proceeding request: ${request.method} ${request.url}")
        val response = chain.proceed(request)
        Log.d("AuthInterceptor", "Response code: ${response.code}")
        return response
    }
}

object ApiClient {
    const val BASE_URL: String = "http://192.168.200.196:5021/"
    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor())
            .build()
    }

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
