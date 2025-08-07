// ProfileViewModel.kt
package com.example.firstapp.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firstapp.data.ProfileState
import com.example.firstapp.data.PasswordChangeStatus
import com.example.firstapp.network.ApiClient
import com.example.firstapp.data.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProfileViewModel(private val context: Context) : ViewModel() {
    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state

    init {
        fetchUser()
    }

    private suspend fun extractUserIdFromToken(): String? {
        val token = UserPreferences.getToken(context) ?: return null
        val parts = token.split(".")
        if (parts.size != 3) return null
        return try {
            val payload = String(Base64.decode(parts[1], Base64.URL_SAFE))
            val json = JSONObject(payload)
            json.getString("id")
        } catch (e: Exception) {
            null
        }
    }

    fun fetchUser() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                val userId = extractUserIdFromToken()
                if (userId == null) {
                    withContext(Dispatchers.Main) {
                        _state.value = _state.value.copy(error = "Không thể lấy user ID từ token", loading = false)
                    }
                    return@launch
                }
                val user = ApiClient.api.getUserDetail(userId)
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(user = user, loading = false)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(error = e.message ?: "Lỗi không xác định", loading = false)
                }
            }
        }
    }

    fun updateUser(name: String, accountNumber: String, bankName: String) {
        val user = _state.value.user ?: return
        _state.value = _state.value.copy(error = null, successMessage = null)
        viewModelScope.launch {
            try {
                val updated = ApiClient.api.updateUser(
                    id = user.id,
                    user = user.copy(
                        name = name,
                        accountNumber = accountNumber,
                        bankName = bankName
                    )
                )
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(user = updated, successMessage = "Cập nhật thành công")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(error = "Không thể cập nhật: ${e.message}")
                }
            }
        }
    }

    fun changePassword(current: String, new: String) {
        _state.value = _state.value.copy(passwordChangeStatus = PasswordChangeStatus.Loading)
        viewModelScope.launch {
            try {
                ApiClient.api.changePassword(mapOf("currentPassword" to current, "newPassword" to new))
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(passwordChangeStatus = PasswordChangeStatus.Success)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(passwordChangeStatus = PasswordChangeStatus.Error(e.message ?: "Lỗi không xác định"))
                }
            }
        }
    }

    fun clearMessages() {
        _state.value = _state.value.copy(error = null, successMessage = null, passwordChangeStatus = PasswordChangeStatus.Idle)
    }
}
