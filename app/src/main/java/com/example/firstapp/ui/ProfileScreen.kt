// ProfileScreen.kt
package com.example.firstapp.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import com.example.firstapp.network.ApiClient

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
                // Header with avatar and basic info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val avatarUrl = user.avatar?.let { path ->
                        if (path.startsWith("http")) path else ApiClient.BASE_URL.trimEnd('/') + path
                    }
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(user.name ?: "", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(user.email ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Row(modifier = Modifier.padding(top = 6.dp)) {
                            if (!user.position.isNullOrBlank()) AssistChip(label = { Text(user.position) }, onClick = { })
                            if (!user.role.isNullOrBlank()) {
                                Spacer(Modifier.width(8.dp))
                                AssistChip(label = { Text(user.role) }, onClick = { })
                            }
                            if (!user.workType.isNullOrBlank()) {
                                Spacer(Modifier.width(8.dp))
                                AssistChip(label = { Text(user.workType) }, onClick = { })
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))

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
