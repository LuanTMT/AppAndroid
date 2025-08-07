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


@Composable
fun ProfileScreen(viewModel: ProfileViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()

    var fullName by remember { mutableStateOf(state.user?.name ?: "") }
    var accountNumber by remember { mutableStateOf(state.user?.accountNumber ?: "") }
    var bankName by remember { mutableStateOf(state.user?.bankName ?: "") }
    var showChangePasswordDialog by remember { mutableStateOf(false) }

    if (state.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Column(modifier = Modifier.padding(16.dp)) {
            state.user?.let { user ->
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Họ tên") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = user.email,
                    onValueChange = {},
                    enabled = false,
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = accountNumber,
                    onValueChange = { accountNumber = it },
                    label = { Text("Số tài khoản") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = bankName,
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
        Text("❌ $it", color = Color.Red, modifier = Modifier.padding(8.dp))
    }

    state.successMessage?.let {
        Text("✅ $it", color = Color.Green, modifier = Modifier.padding(8.dp))
    }

    if (showChangePasswordDialog) {
        ChangePasswordDialog(onDismiss = { showChangePasswordDialog = false }, viewModel = viewModel)
    }
}
