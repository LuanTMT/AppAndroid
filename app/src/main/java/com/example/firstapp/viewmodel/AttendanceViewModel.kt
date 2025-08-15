package com.example.firstapp.viewmodel

import android.content.Context
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Date
import com.example.firstapp.data.Attendance
import com.example.firstapp.data.AttendanceType
import com.example.firstapp.network.ApiClient

class AttendanceViewModel : ViewModel() {
    private val _attendanceState = MutableStateFlow<AttendanceState>(AttendanceState.Idle)
    val attendanceState: StateFlow<AttendanceState> = _attendanceState

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation

    private val _imagePath = MutableStateFlow<String?>(null)
    val imagePath: StateFlow<String?> = _imagePath

    fun updateLocation(location: Location) {
        _currentLocation.value = location
    }

    fun updateImagePath(path: String) {
        _imagePath.value = path
    }

    fun checkIn(context: Context) {
        _attendanceState.value = AttendanceState.Loading
        
        viewModelScope.launch {
            try {
                val location = _currentLocation.value
                val imagePath = _imagePath.value
                
                if (location == null) {
                    _attendanceState.value = AttendanceState.Error("Không thể lấy vị trí")
                    return@launch
                }

                val attendance = Attendance(
                    id = System.currentTimeMillis().toString(),
                    userId = "user123", // Thay bằng user ID thực tế
                    timestamp = Date(),
                    latitude = location.latitude,
                    longitude = location.longitude,
                    address = "Đang lấy địa chỉ...", // Có thể dùng Geocoder để lấy địa chỉ
                    imagePath = imagePath ?: "",
                    type = AttendanceType.CHECK_IN
                )

                ApiClient.api.submitAttendance(attendance)
                _attendanceState.value = AttendanceState.Success("Chấm công vào thành công!")
                
            } catch (e: Exception) {
                _attendanceState.value = AttendanceState.Error("Lỗi: ${e.message}")
            }
        }
    }

    fun checkOut(context: Context) {
        _attendanceState.value = AttendanceState.Loading
        
        viewModelScope.launch {
            try {
                val location = _currentLocation.value
                val imagePath = _imagePath.value
                
                if (location == null) {
                    _attendanceState.value = AttendanceState.Error("Không thể lấy vị trí")
                    return@launch
                }

                val attendance = Attendance(
                    id = System.currentTimeMillis().toString(),
                    userId = "user123", // Thay bằng user ID thực tế
                    timestamp = Date(),
                    latitude = location.latitude,
                    longitude = location.longitude,
                    address = "Đang lấy địa chỉ...",
                    imagePath = imagePath ?: "",
                    type = AttendanceType.CHECK_OUT
                )

                ApiClient.api.submitAttendance(attendance)
                _attendanceState.value = AttendanceState.Success("Chấm công ra thành công!")
                
            } catch (e: Exception) {
                _attendanceState.value = AttendanceState.Error("Lỗi: ${e.message}")
            }
        }
    }

    fun resetState() {
        _attendanceState.value = AttendanceState.Idle
    }
}

sealed class AttendanceState {
    object Idle : AttendanceState()
    object Loading : AttendanceState()
    data class Success(val message: String) : AttendanceState()
    data class Error(val message: String) : AttendanceState()
} 