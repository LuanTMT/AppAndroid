package com.example.firstapp.viewmodel

import android.content.Context
import android.location.Location
import android.util.Base64
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firstapp.data.AttendanceRecord
import com.example.firstapp.data.TodayAttendance
import com.example.firstapp.data.AttendanceRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.example.firstapp.network.ApiClient
import java.io.File
import java.io.ByteArrayOutputStream
import retrofit2.HttpException
import androidx.exifinterface.media.ExifInterface

class AttendanceViewModel : ViewModel() {
    private val _attendanceState = MutableStateFlow<AttendanceState>(AttendanceState.Idle)
    val attendanceState: StateFlow<AttendanceState> = _attendanceState

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation

    private val _imagePath = MutableStateFlow<String?>(null)
    val imagePath: StateFlow<String?> = _imagePath

    private val _today = MutableStateFlow<AttendanceRecord?>(null)
    val today: StateFlow<AttendanceRecord?> = _today

    private val _checkInRecord = MutableStateFlow<AttendanceRecord?>(null)
    val checkInRecord: StateFlow<AttendanceRecord?> = _checkInRecord

    private val _checkOutRecord = MutableStateFlow<AttendanceRecord?>(null)
    val checkOutRecord: StateFlow<AttendanceRecord?> = _checkOutRecord

    fun updateLocation(location: Location) {
        _currentLocation.value = location
    }

    fun updateImagePath(path: String) {
        _imagePath.value = path
    }

    fun fetchToday() {
        viewModelScope.launch {
            try {
                val res = ApiClient.api.getTodayAttendance()
                if (res.success) {
                    // API: data { email, checkIn{ _id, image } }, timestamp ngoài
                    val payload: TodayAttendance? = res.data
                    val recIn = payload?.checkIn?.id?.let {
                        AttendanceRecord(
                            _id = it,
                            email = payload.email,
                            name = null,
                            image = ensureDataUri(payload.checkIn?.image),
                            location = payload.checkIn?.location,
                            timestamp = payload.checkIn?.timestamp,
                            type = "check_in",
                            status = null
                        )
                    }
                    val recOut = payload?.checkOut?.id?.let {
                        AttendanceRecord(
                            _id = it,
                            email = payload.email,
                            name = null,
                            image = ensureDataUri(payload.checkOut?.image),
                            location = payload.checkOut?.location,
                            timestamp = payload.checkOut?.timestamp,
                            type = "check_out",
                            status = null
                        )
                    }
                    _checkInRecord.value = recIn
                    _checkOutRecord.value = recOut
                    // keep today as last non-null, for backward compat
                    _today.value = recOut ?: recIn
                    Log.d(
                        "AttendanceViewModel",
                        "fetchToday success(mapped): in=${recIn?._id != null}, out=${recOut?._id != null}, tsIn=${recIn?.timestamp}, tsOut=${recOut?.timestamp}"
                    )
                    _attendanceState.value = AttendanceState.Idle
                } else {
                    Log.w("AttendanceViewModel", "fetchToday failure: ${res.message}")
                    _attendanceState.value = AttendanceState.Error(res.message ?: "Lỗi tải dữ liệu")
                }
            } catch (e: Exception) {
                val message = if (e is HttpException) {
                    val code = e.code()
                    val body = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
                    "HTTP $code: ${body ?: e.message()}"
                } else e.message
                Log.e("AttendanceViewModel", "fetchToday exception: $message", e)
                _attendanceState.value = AttendanceState.Error(message ?: "Lỗi mạng")
            }
        }
    }

    fun checkIn(address: String?, context: Context) {
        submit("check_in", address)
    }

    fun checkOut(address: String?, context: Context) {
        submit("check_out", address)
    }

    fun submit(type: String, address: String?) {
        _attendanceState.value = AttendanceState.Loading
        viewModelScope.launch {
            try {
                val location = _currentLocation.value
                val imagePathLocal = _imagePath.value
                if (location == null) {
                    _attendanceState.value = AttendanceState.Error("Không thể lấy vị trí")
                    return@launch
                }
                if (imagePathLocal.isNullOrEmpty()) {
                    _attendanceState.value = AttendanceState.Error("Chưa có ảnh")
                    return@launch
                }
                val base64 = encodeImageFileToBase64(imagePathLocal)
                val dataUrl = ensureDataUri(base64)
                val body = AttendanceRequest(
                    image = dataUrl,
                    location = address ?: "Lat: ${location.latitude}, Lng: ${location.longitude}",
                    type = type
                )
                val res = ApiClient.api.submitAttendance(body)
                if (res.success) {
                    _attendanceState.value = AttendanceState.Success("Chấm công thành công")
                    _today.value = res.data
                    val t = _today.value
                    Log.d("AttendanceViewModel", "submit success: type=${t?.type}, timestamp=${t?.timestamp}, imageLen=${t?.image?.length ?: 0}, location=${t?.location}")
                } else {
                    Log.w("AttendanceViewModel", "submit failure: ${res.message}")
                    _attendanceState.value = AttendanceState.Error(res.message ?: "Thất bại")
                }
            } catch (e: Exception) {
                val message = if (e is HttpException) {
                    val code = e.code()
                    val body = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
                    "HTTP $code: ${body ?: e.message()}"
                } else e.message
                Log.e("AttendanceViewModel", "submit exception: $message", e)
                _attendanceState.value = AttendanceState.Error(message ?: "Lỗi mạng")
            }
        }
    }

    private fun ensureDataUri(imageBaseOrUrl: String?): String {
        if (imageBaseOrUrl.isNullOrEmpty()) return ""
        val s = imageBaseOrUrl.trim()
        return if (s.startsWith("http") || s.startsWith("data:")) s else "data:image/jpeg;base64,$s"
    }

    private fun encodeImageFileToBase64(path: String): String {
        // Decode with sampling to avoid OOM
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(path, options)
        val maxDim = 1280
        var inSampleSize = 1
        var width = options.outWidth
        var height = options.outHeight
        while (width / inSampleSize > maxDim || height / inSampleSize > maxDim) {
            inSampleSize *= 2
        }
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = inSampleSize
        }
        val bitmap = BitmapFactory.decodeFile(path, decodeOptions)
        val rotation = getRotationDegreesFromExif(path)
        val scaled = scaleBitmapIfNeeded(bitmap, maxDim, maxDim)
        val corrected = if (rotation != 0) rotateBitmap(scaled, rotation) else scaled
        val baos = ByteArrayOutputStream()
        corrected.compress(Bitmap.CompressFormat.JPEG, 75, baos)
        val bytes = baos.toByteArray()
        if (scaled !== bitmap) bitmap.recycle()
        corrected.recycle()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun scaleBitmapIfNeeded(src: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = src.width
        val height = src.height
        if (width <= maxWidth && height <= maxHeight) return src
        val ratio = minOf(maxWidth.toFloat() / width, maxHeight.toFloat() / height)
        val newW = (width * ratio).toInt()
        val newH = (height * ratio).toInt()
        return Bitmap.createScaledBitmap(src, newW, newH, true)
    }

    private fun rotateBitmap(src: Bitmap, degrees: Int): Bitmap {
        val m = Matrix()
        m.postRotate(degrees.toFloat())
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
    }

    private fun getRotationDegreesFromExif(path: String): Int {
        return try {
            val exif = ExifInterface(path)
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } catch (e: Exception) {
            0
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
