// ProfileScreen.kt
package com.example.firstapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.firstapp.viewmodel.ProfileViewModel
import androidx.compose.ui.Alignment
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import com.example.firstapp.viewmodel.ProfileViewModelFactory
import com.example.firstapp.data.UserPreferences
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import android.util.Log

@Composable
fun ProfileScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var userId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        userId = UserPreferences.getUserId(context)
        Log.d("ProfileScreen", "Loaded userId from prefs: $userId")
    }

    if (userId == null) {
        Log.d("ProfileScreen", "userId is null -> showing loading spinner")
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    } else if (userId!!.isEmpty()) {
        Log.e("ProfileScreen", "userId is empty -> cannot load profile")
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Không tìm thấy người dùng")
        }
        return
    }

    val viewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModelFactory(userId!!)
    )

    val state by viewModel.state.collectAsState()

    LaunchedEffect(viewModel) {
        Log.d("ProfileScreen", "ProfileViewModel created for userId=$userId")
    }

    var fullName by remember { mutableStateOf(state.user?.name ?: "") }
    var accountNumber by remember { mutableStateOf(state.user?.accountNumber ?: "") }
    var bankName by remember { mutableStateOf(state.user?.bankName ?: "") }

    LaunchedEffect(state.user) {
        fullName = state.user?.name ?: ""
        accountNumber = state.user?.accountNumber ?: ""
        bankName = state.user?.bankName ?: ""
    }
    var showChangePasswordDialog by remember { mutableStateOf(false) }

    if (state.loading) {
        Log.d("ProfileScreen", "state.loading = true, waiting for user data")
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Log.d("ProfileScreen", "state.loading = false, user loaded? ${state.user != null}, error=${state.error}")
        Column(modifier = Modifier.padding(16.dp)) {
            state.user?.let { user ->
                OutlinedTextField(
                    value = fullName.ifEmpty { "" },
                    onValueChange = { fullName = it },
                    label = { Text("Họ tên") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = user.email ?: "",
                    onValueChange = {},
                    enabled = false,
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = accountNumber.ifEmpty { "" },
                    onValueChange = { accountNumber = it },
                    label = { Text("Số tài khoản") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = bankName.ifEmpty { "" },
                    onValueChange = { bankName = it },
                    label = { Text("Ngân hàng") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                Button(onClick = {
                    viewModel.updateUser(fullName, accountNumber, bankName)
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Lưu thay đổi")
                }

                Spacer(Modifier.height(8.dp))
                Button(onClick = { showChangePasswordDialog = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Đổi mật khẩu")
                }

                Spacer(Modifier.height(8.dp))
                Button(onClick = { /* mở picker ảnh avatar */ }, modifier = Modifier.fillMaxWidth()) {
                    Text("Cập nhật Avatar")
                }
            }
        }
    }

    state.error?.let {
        Log.e("ProfileScreen", "Error: $it")
        Text("❌ $it", color = Color.Red, modifier = Modifier.padding(8.dp))
    }

    state.successMessage?.let {
        Text("✅ $it", color = Color.Green, modifier = Modifier.padding(8.dp))
    }

    if (showChangePasswordDialog) {
        ChangePasswordDialog(onDismiss = { showChangePasswordDialog = false }, viewModel = viewModel)
    }
}
