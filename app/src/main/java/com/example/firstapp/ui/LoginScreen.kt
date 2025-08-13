// LoginScreen.kt
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
import com.example.firstapp.network.TokenProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.firstapp.data.JwtUtils

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
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var rememberLogin by remember { mutableStateOf(false) }

    fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun isValidPassword(password: String): Boolean {
        return password.length >= 8
    }

    LaunchedEffect(Unit) {
        // Prefill email and remember flag if enabled previously
        rememberLogin = UserPreferences.isRememberLogin(context)
        if (rememberLogin) {
            UserPreferences.getSavedEmail(context)?.let { email = it }
        }
    }

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
            onValueChange = {
                email = it
                emailError = null
            },
            label = { Text("Email") },
            singleLine = true,
            isError = emailError != null
        )
        if (emailError != null) {
            Text(emailError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                passwordError = null
            },
            label = { Text("Mật khẩu") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            isError = passwordError != null
        )
        if (passwordError != null) {
            Text(passwordError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = rememberLogin, onCheckedChange = { rememberLogin = it })
            Spacer(Modifier.width(8.dp))
            Text("Ghi nhớ đăng nhập")
        }
        Spacer(Modifier.height(24.dp))
        if (error != null) {
            Text(error!!, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
        }
        Button(
            onClick = {
                emailError = null
                passwordError = null
                error = null
                if (!isValidEmail(email)) {
                    emailError = "Email không đúng định dạng"
                    return@Button
                }
                if (!isValidPassword(password)) {
                    passwordError = "Mật khẩu phải từ 8 ký tự trở lên"
                    return@Button
                }
                isLoading = true
                scope.launch {
                    try {
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

                            println("Sending request to: ${request.url}")
                            println("Request body: ${json.toString()}")

                            val response = client.newCall(request).execute()
                            println("Response code: ${response.code}")
                            println("Response message: ${response.message}")

                            val responseBody = response.body?.string()
                            println("Response body: $responseBody")

                            withContext(Dispatchers.Main) {
                                if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                                    try {
                                        val jsonResponse = JSONObject(responseBody)

                                        // Lấy token từ nhiều khả năng cấu trúc JSON khác nhau
                                        val tokenCandidates = sequence {
                                            yield(jsonResponse.optString("token", null))
                                            yield(jsonResponse.optJSONObject("data")?.optString("token", null))
                                            yield(jsonResponse.optJSONObject("user")?.optString("token", null))
                                        }.firstOrNull { !it.isNullOrEmpty() }

                                        val token = tokenCandidates
                                        if (!token.isNullOrEmpty()) {
                                            UserPreferences.saveToken(context, token)
                                            TokenProvider.token = token

                                            // Thu thập userId theo nhiều khả năng
                                            val userIdCandidates = sequence {
                                                yield(jsonResponse.optString("userId", null))
                                                yield(jsonResponse.optString("id", null))
                                                yield(jsonResponse.optJSONObject("user")?.optString("id", null))
                                                yield(jsonResponse.optJSONObject("data")?.optString("userId", null))
                                                yield(jsonResponse.optJSONObject("data")?.optString("id", null))
                                                yield(jsonResponse.optJSONObject("data")?.optJSONObject("user")?.optString("id", null))
                                            }.firstOrNull { !it.isNullOrEmpty() }

                                            val finalUserId = userIdCandidates ?: JwtUtils.extractUserId(token)
                                            if (!finalUserId.isNullOrEmpty()) {
                                                println("Login success: saving userId=$finalUserId")
                                                UserPreferences.saveUserId(context, finalUserId)
                                            } else {
                                                println("Login success: userId not found in response or token payload")
                                            }

                                            // Persist remember choice and email
                                            UserPreferences.setRememberLogin(context, rememberLogin)
                                            if (rememberLogin) {
                                                UserPreferences.saveEmail(context, email)
                                            } else {
                                                UserPreferences.clearSavedLogin(context)
                                            }

                                            onLoginSuccess()
                                        } else {
                                            error = "Đăng nhập thất bại: Không nhận được token. Server trả về: $responseBody"
                                        }
                                    } catch (e: Exception) {
                                        error = "Lỗi parse JSON: $responseBody"
                                    }
                                } else {
                                    error = "Sai tài khoản hoặc mật khẩu (${response.code}): $responseBody"
                                }
                                isLoading = false
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            println("Exception: ${e.message}")
                            e.printStackTrace()
                            error = "Lỗi kết nối: ${e.message}"
                            isLoading = false
                        }
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
