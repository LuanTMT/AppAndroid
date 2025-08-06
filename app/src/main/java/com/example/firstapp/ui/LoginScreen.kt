package com.example.firstapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.example.firstapp.data.UserPreferences
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Đăng nhập", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Tên đăng nhập") },
            singleLine = true
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Mật khẩu") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(Modifier.height(24.dp))
        if (error != null) {
            Text(error!!, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
        }
        Button(
            onClick = {
                isLoading = true
                error = null
                scope.launch {
                    try {
                        // Chuyển network call sang background thread
                        withContext(Dispatchers.IO) {
                            val client = OkHttpClient()
                            val json = JSONObject()
                            json.put("email", email)
                            json.put("password", password)
                            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                            val body: RequestBody = json.toString().toRequestBody(mediaType)
                            val request = Request.Builder()
                                .url("http://192.168.200.196:5021/auth/login")
                                .post(body)
                                .build()

                            // Log request để debug
                            println("Sending request to: ${request.url}")
                            println("Request body: ${json.toString()}")

                            val response = client.newCall(request).execute()
                            println("Response code: ${response.code}")
                            println("Response message: ${response.message}")

                            if (response.isSuccessful) {
                                val responseBody = response.body?.string()
                                println("Response body: $responseBody")

                                if (responseBody.isNullOrEmpty()) {
                                    error = "Lỗi kết nối: Không nhận được phản hồi từ server"
                                    isLoading = false
                                    return@withContext
                                }

                                try {
                                    val jsonResponse = JSONObject(responseBody)
                                    val token = jsonResponse.optString("token", null)
                                    if (!token.isNullOrEmpty()) {
                                        UserPreferences.saveToken(context, token)
                                        isLoading = false
                                        onLoginSuccess()
                                    } else {
                                        error = "Đăng nhập thất bại: Không nhận được token. Server trả về: $responseBody"
                                        isLoading = false
                                    }
                                } catch (e: Exception) {
                                    error = "Lỗi parse JSON: $responseBody"
                                    isLoading = false
                                }
                            } else {
                                val errorBody = response.body?.string()
                                error = "Sai tài khoản hoặc mật khẩu (${response.code}): $errorBody"
                                isLoading = false
                            }
                        }
                    } catch (e: Exception) {
                        println("Exception: ${e.message}")
                        e.printStackTrace()
                        error = "Lỗi kết nối: ${e.message}"
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Đăng nhập")
            }
        }
    }
}