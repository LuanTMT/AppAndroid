// ProfileViewModel.kt
package com.example.firstapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firstapp.data.PasswordChangeStatus
import com.example.firstapp.data.ProfileState
import com.example.firstapp.network.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import android.util.Log
import com.example.firstapp.data.User

/**
 * ViewModel quản lý màn hình Profile.
 * Nhận sẵn userId (String) từ UserPreferences/đăng nhập, KHÔNG dùng Context trong ViewModel.
 */
class ProfileViewModel(private val userId: String) : ViewModel() {
    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state

    init {
        Log.d("ProfileViewModel", "init with userId=$userId")
        fetchUser()
    }

    /** Lấy thông tin user bằng userId đã inject */
    fun fetchUser() {
        Log.d("ProfileViewModel", "fetchUser() called for userId=$userId")
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                Log.d("ProfileViewModel", "Calling ApiClient.api.getUserDetail($userId)")
                val response = ApiClient.api.getUserDetail(userId)
                val user: User = response.data ?: throw IllegalStateException("Không lấy được dữ liệu người dùng")
                Log.d("ProfileViewModel", "getUserDetail success: $user")
                _state.value = _state.value.copy(user = user, loading = false)
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "getUserDetail error: ${e.message}", e)
                _state.value = _state.value.copy(
                    error = e.message ?: "Lỗi không xác định",
                    loading = false
                )
            }
        }
    }

    /** Cập nhật user */
    fun updateUser(name: String, accountNumber: String, bankName: String) {
        val currentUser = _state.value.user ?: return
        _state.value = _state.value.copy(error = null, successMessage = null)
        viewModelScope.launch {
            try {
                Log.d("ProfileViewModel", "updateUser() sending update for id=${currentUser.id}")
                val response = ApiClient.api.updateUser(
                    id = currentUser.id ?: userId,
                    user = currentUser.copy(
                        name = name,
                        accountNumber = accountNumber,
                        bankName = bankName
                    )
                )
                val updated = response.data ?: throw IllegalStateException("Cập nhật không thành công")
                Log.d("ProfileViewModel", "updateUser success: $updated")
                _state.value = _state.value.copy(
                    user = updated,
                    successMessage = "Cập nhật thành công"
                )
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "updateUser error: ${e.message}", e)
                _state.value = _state.value.copy(error = "Không thể cập nhật: ${e.message}")
            }
        }
    }

    /** Đổi mật khẩu */
    fun changePassword(current: String, new: String) {
        _state.value = _state.value.copy(passwordChangeStatus = PasswordChangeStatus.Loading)
        viewModelScope.launch {
            try {
                Log.d("ProfileViewModel", "changePassword() called")
                ApiClient.api.changePassword(
                    mapOf("currentPassword" to current, "newPassword" to new)
                )
                Log.d("ProfileViewModel", "changePassword success")
                _state.value = _state.value.copy(passwordChangeStatus = PasswordChangeStatus.Success)
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "changePassword error: ${e.message}", e)
                _state.value = _state.value.copy(
                    passwordChangeStatus = PasswordChangeStatus.Error(e.message ?: "Lỗi không xác định")
                )
            }
        }
    }

    /** Reset thông điệp */
    fun clearMessages() {
        _state.value = _state.value.copy(
            error = null,
            successMessage = null,
            passwordChangeStatus = PasswordChangeStatus.Idle
        )
    }

    // Raw parsing removed for cleanliness per request
}
