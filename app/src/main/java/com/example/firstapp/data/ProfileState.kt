// ProfileState.kt
package com.example.firstapp.data

import com.example.firstapp.data.User

// Trạng thái tổng cho màn hình Profile

data class ProfileState(
    val user: User? = null,
    val loading: Boolean = true,
    val error: String? = null,
    val successMessage: String? = null,
    val passwordChangeStatus: PasswordChangeStatus = PasswordChangeStatus.Idle
)

sealed class PasswordChangeStatus {
    object Idle : PasswordChangeStatus()
    object Loading : PasswordChangeStatus()
    object Success : PasswordChangeStatus()
    data class Error(val message: String) : PasswordChangeStatus()
}
