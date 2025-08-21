package com.example.firstapp.ui

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.rememberImagePainter
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.compose.ui.graphics.asImageBitmap
import android.util.Base64
import com.example.firstapp.R
import com.example.firstapp.viewmodel.AttendanceViewModel
import com.example.firstapp.viewmodel.AttendanceState
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.layout.ContentScale
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import com.example.firstapp.ui.theme.FirstAPPTheme
import androidx.compose.ui.tooling.preview.Preview as Review
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.util.Log

// Định nghĩa allowedLocations (có thể lấy từ server hoặc hardcode)
data class AllowedLocation(val name: String, val latitude: Double, val longitude: Double, val radius: Int)

val allowedLocations = listOf(
    AllowedLocation(
        name = "Văn phòng Vis Group Co., Ltd.",
        latitude = 16.0634623,
        longitude = 108.170192,
        radius = 300 // mét
    )
)

// Hàm tính khoảng cách giữa 2 điểm (mét)
fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R = 6371e3 // Bán kính trái đất (mét)
    val φ1 = Math.toRadians(lat1)
    val φ2 = Math.toRadians(lat2)
    val Δφ = Math.toRadians(lat2 - lat1)
    val Δλ = Math.toRadians(lon2 - lon1)
    val a = Math.sin(Δφ / 2) * Math.sin(Δφ / 2) +
            Math.cos(φ1) * Math.cos(φ2) *
            Math.sin(Δλ / 2) * Math.sin(Δλ / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return R * c
}

// Hàm lấy địa chỉ từ tọa độ (reverse geocode)
fun getAddressFromLocation(context: android.content.Context, location: Location?): String {
    if (location == null) return "Không xác định"
    return try {
        val geocoder = Geocoder(context, Locale.getDefault())
        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
        if (!addresses.isNullOrEmpty()) {
            val addr = addresses[0]
            listOfNotNull(
                addr.thoroughfare,
                addr.subLocality,
                addr.locality,
                addr.adminArea,
                addr.countryName
            ).joinToString(", ")
        } else {
            "Lat: %.5f, Lng: %.5f".format(location.latitude, location.longitude)
        }
    } catch (e: Exception) {
        "Lat: %.5f, Lng: %.5f".format(location.latitude, location.longitude)
    }
}

fun formatTime(date: Date?): String {
    if (date == null) return "--:--:--"
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(date)
}

fun parseServerTimeToDate(iso: String?): Date? {
    if (iso.isNullOrEmpty()) return null
    val patterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX"
    )
    for (p in patterns) {
        try {
            val sdf = SimpleDateFormat(p, Locale.getDefault())
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val d = sdf.parse(iso)
            android.util.Log.d("AttendanceScreen", "parse time ok: pattern=$p -> $d from $iso")
            return d
        } catch (_: Exception) { }
    }
    android.util.Log.w("AttendanceScreen", "parse time failed for: $iso")
    return null
}

// Chuyển chuỗi ảnh trả về từ server thành URL hiển thị được
fun toDisplayImage(src: String?): String? {
    if (src.isNullOrEmpty()) return null
    return if (src.startsWith("http") || src.startsWith("data:")) src
    else "data:image/jpeg;base64,$src"
}

fun normalizeType(type: String?): String? {
    if (type == null) return null
    val t = type.lowercase(Locale.getDefault())
    return when (t) {
        "check_in", "checkin", "check-in", "in" -> "check_in"
        "check_out", "checkout", "check-out", "out" -> "check_out"
        else -> t
    }
}

fun decodeDataUriToBitmap(dataUri: String, maxDim: Int = 1024): Bitmap? {
    return try {
        val base64Part = dataUri.substringAfter(",", missingDelimiterValue = "")
        if (base64Part.isEmpty()) return null
        val bytes = Base64.decode(base64Part, Base64.DEFAULT)
        // bounds first
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var inSampleSize = 1
        while ((bounds.outWidth / inSampleSize) > maxDim || (bounds.outHeight / inSampleSize) > maxDim) {
            inSampleSize *= 2
        }
        val opts = BitmapFactory.Options().apply { this.inSampleSize = inSampleSize }
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        // Attempt to detect EXIF-like rotation markers in data URI is unreliable.
        // If server preserved orientation, we may need to rotate based on width/height heuristic.
        val rotated = if (bmp.width > bmp.height && dataUri.contains("orientation=right", ignoreCase = true)) {
            rotateBitmap(bmp, 90)
        } else bmp
        rotated
    } catch (e: Exception) {
        android.util.Log.w("AttendanceScreen", "decodeDataUriToBitmap failed: ${e.message}")
        null
    }
}

fun rotateBitmap(src: Bitmap, degrees: Int): Bitmap {
    val m = Matrix()
    m.postRotate(degrees.toFloat())
    return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    viewModel: AttendanceViewModel = viewModel(),
    onShowCamera: () -> Unit = {},
    onHideCamera: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val attendanceState by viewModel.attendanceState.collectAsState()
    val imagePath by viewModel.imagePath.collectAsState()
    val recIn by viewModel.checkInRecord.collectAsState()
    val recOut by viewModel.checkOutRecord.collectAsState()

    // State quản lý quyền
    var hasCameraPermission by remember { mutableStateOf(false) }
    var hasLocationPermission by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var locationError by remember { mutableStateOf<String?>(null) }

    // State vị trí, hợp lệ, địa chỉ
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var isValidLocation by remember { mutableStateOf(false) }
    var nameLocal by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var distance by remember { mutableStateOf<Double?>(null) }

    // State dữ liệu chấm công
    val today by viewModel.today.collectAsState()
    var showCameraModal by remember { mutableStateOf(false) }
    var clockType by remember { mutableStateOf<String?>(null) } // "checkIn"/"checkOut"/"update_check_in"/"update_check_out"
    var currentTime by remember { mutableStateOf(Date()) }
    var capturedImage by remember { mutableStateOf<String?>(null) }

    // State quản lý upload
    var isUploading by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }
    var uploadSuccess by remember { mutableStateOf(false) }

    // Tự động cập nhật giờ mỗi giây
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Date()
            delay(1000)
        }
    }

    // Gọi callback khi showCameraModal đổi trạng thái
    var prevShowCameraModal by remember { mutableStateOf(false) }
    LaunchedEffect(showCameraModal) {
        if (showCameraModal && !prevShowCameraModal) {
            onShowCamera()
        }
        if (!showCameraModal && prevShowCameraModal) {
            onHideCamera()
        }
        prevShowCameraModal = showCameraModal
    }

    // Phản ứng theo trạng thái submit từ ViewModel để điều khiển UI
    LaunchedEffect(attendanceState) {
        when (val s = attendanceState) {
            is AttendanceState.Loading -> {
                isUploading = true
                uploadError = null
                uploadSuccess = false
            }
            is AttendanceState.Success -> {
                isUploading = false
                uploadError = null
                uploadSuccess = true
                showCameraModal = false
                viewModel.fetchToday()
            }
            is AttendanceState.Error -> {
                isUploading = false
                uploadSuccess = false
                uploadError = s.message
            }
            else -> {}
        }
    }

    // Hiển thị CameraScreen khi showCameraModal = true
    if (showCameraModal) {
        CameraScreen(
            onImageCaptured = { path ->
                // Reset state khi bắt đầu upload mới
                isUploading = true
                uploadError = null
                uploadSuccess = false
                // Lưu path vào VM rồi submit qua API JSON
                viewModel.updateImagePath(path)
                val submitType = when (clockType) {
                    "checkIn", "update_check_in" -> "check_in"
                    else -> "check_out"
                }
                viewModel.submit(submitType, address)
            },
            onCancel = {
                // Reset state khi hủy
                isUploading = false
                uploadError = null
                uploadSuccess = false
                showCameraModal = false
            },
            isUploading = isUploading,
            uploadError = uploadError
        )
        return
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCameraPermission = permissions[Manifest.permission.CAMERA] == true
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (!hasCameraPermission) cameraError = "Cần cấp quyền camera để chấm công"
        if (!hasLocationPermission) locationError = "Cần cấp quyền vị trí để chấm công"
    }

    // Xin quyền khi vào màn hình
    LaunchedEffect(Unit) {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        val pm = context.packageManager
        hasCameraPermission = permissions.take(1).all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        hasLocationPermission = permissions.drop(1).all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (!hasCameraPermission || !hasLocationPermission) {
            permissionLauncher.launch(permissions)
        }
    }

    // Lấy vị trí thực tế
    fun fetchLocation() {
        val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    currentLocation = location
                    address = getAddressFromLocation(context, location)
                    viewModel.updateLocation(location)
                    // Kiểm tra hợp lệ
                    val allowed = allowedLocations.firstOrNull {
                        calculateDistance(location.latitude, location.longitude, it.latitude, it.longitude) <= it.radius
                    }
                    if (allowed != null) {
                        isValidLocation = true
                        nameLocal = allowed.name
                        distance = calculateDistance(location.latitude, location.longitude, allowed.latitude, allowed.longitude)
                    } else {
                        isValidLocation = false
                        nameLocal = ""
                        distance = null
                    }
                } else {
                    locationError = "Không thể lấy vị trí hiện tại"
                }
            }.addOnFailureListener {
                locationError = "Không thể lấy vị trí: ${it.message}"
            }
        } else {
            locationError = "Chưa có quyền vị trí"
        }
    }

    // Lấy vị trí khi có quyền
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) fetchLocation()
    }

    // Giao diện xin quyền
    if (!hasCameraPermission || !hasLocationPermission) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!hasCameraPermission) {
                Text("Ứng dụng cần quyền camera để chấm công", color = Color.Red)
                Button(onClick = {
                    permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                }) { Text("Cấp quyền Camera") }
            }
            Spacer(Modifier.height(16.dp))
            if (!hasLocationPermission) {
                Text("Ứng dụng cần quyền vị trí để chấm công", color = Color.Red)
                Button(onClick = {
                    permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                }) { Text("Cấp quyền Vị trí") }
            }
            if (cameraError != null) Text(cameraError!!, color = Color.Red)
            if (locationError != null) Text(locationError!!, color = Color.Red)
        }
        return
    }

    // State theo dõi scroll (có ngưỡng để tránh nháy)
    val listState = rememberLazyListState()
    var lastScrollOffset by remember { mutableStateOf(0) }
    var showChamCongBar by remember { mutableStateOf(true) }
    val density = LocalDensity.current
    val thresholdPx = with(density) { 16.dp.toPx().toInt() }
    var accumulatedDelta by remember { mutableStateOf(0) }
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemScrollOffset }
            .collect { currentOffset ->
                val delta = currentOffset - lastScrollOffset
                lastScrollOffset = currentOffset
                accumulatedDelta += delta
                if (accumulatedDelta > thresholdPx && showChamCongBar) {
                    showChamCongBar = false
                    accumulatedDelta = 0
                } else if (accumulatedDelta < -thresholdPx && !showChamCongBar) {
                    showChamCongBar = true
                    accumulatedDelta = 0
                }
                if (currentOffset == 0 && !showChamCongBar) {
                    showChamCongBar = true
                    accumulatedDelta = 0
                }
            }
    }
    val topBarAlpha by animateFloatAsState(targetValue = if (showChamCongBar) 1f else 0f, label = "topbar-alpha")

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(86.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Image(
                    painter = painterResource(id = R.drawable.topbar),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth().height(86.dp),
                    contentScale = ContentScale.Fit
                )
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(86.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(topBarAlpha),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Chấm công",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                }
            }
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    android.util.Log.d("AttendanceScreen", "Bind Giờ Vào: ts=${recIn?.timestamp}, imgLen=${recIn?.image?.length ?: 0}")
                    val checkInImage = toDisplayImage(recIn?.image)
                    val checkInAddr = recIn?.location
                    val checkInTimeDate = parseServerTimeToDate(recIn?.timestamp)
                    AttendanceCard(
                        title = "Giờ Vào",
                        time = formatTime(checkInTimeDate ?: currentTime),
                        address = checkInAddr ?: address,
                        status = null,
                        statusColor = null,
                        imagePath = checkInImage,
                        buttonText = if (recIn?._id != null) "Cập nhật giờ vào" else "Chấm công vào",
                         buttonColor = Color(0xFF476f95),
                        onButtonClick = {
                            clockType = if (recIn?._id != null) "update_check_in" else "checkIn"
                            showCameraModal = true
                        },
                        isValidLocation = isValidLocation,
                        nameLocal = nameLocal
                    )
                }
                item {
                    android.util.Log.d("AttendanceScreen", "Bind Giờ Ra: ts=${recOut?.timestamp}, imgLen=${recOut?.image?.length ?: 0}")
                    val checkOutImage = toDisplayImage(recOut?.image)
                    val checkOutAddr = recOut?.location
                    val checkOutTimeDate = parseServerTimeToDate(recOut?.timestamp)
                    AttendanceCard(
                        title = "Giờ Ra",
                        time = formatTime(checkOutTimeDate ?: currentTime),
                        address = checkOutAddr ?: address,
                        status = null,
                        statusColor = null,
                        imagePath = checkOutImage,
                        buttonText = if (recOut?._id != null) "Cập nhật giờ ra" else "Chấm công ra",
                        buttonColor = Color(0xFF7C4DFF),
                        onButtonClick = {
                            clockType = if (recOut?._id != null) "update_check_out" else "checkOut"
                            showCameraModal = true
                        },
                        isValidLocation = isValidLocation,
                        nameLocal = nameLocal
                    )
                }
            }
        }
    }
    // Load dữ liệu hôm nay khi vào màn hình
    LaunchedEffect(Unit) { viewModel.fetchToday() }
}

@Composable
fun AttendanceCard(
    title: String,
    time: String,
    address: String,
    status: String?,
    statusColor: Color?,
    imagePath: String?,
    buttonText: String,
    buttonColor: Color,
    onButtonClick: () -> Unit,
    isValidLocation: Boolean,
    nameLocal: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(time, style = MaterialTheme.typography.titleLarge, color = Color.Black)
            Spacer(Modifier.height(8.dp))
            Text(address, style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray, textAlign = TextAlign.Center)
            if (status != null && statusColor != null) {
                Spacer(Modifier.height(8.dp))
                Text("Trạng thái: $status", color = statusColor, style = MaterialTheme.typography.bodyMedium)
            }
            if (imagePath != null) {
                Spacer(Modifier.height(8.dp))
                val isDataUri = imagePath.startsWith("data:")
                if (isDataUri) {
                    val bmp = remember(imagePath) { decodeDataUriToBitmap(imagePath, maxDim = 512) }
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    Image(
                        painter = rememberImagePainter(imagePath),
                        contentDescription = null,
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            if (!isValidLocation) {
                Text("Vui lòng di chuyển đến địa điểm công ty", color = Color.Red, style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(nameLocal, color = Color.Blue, style = MaterialTheme.typography.bodyMedium)
            }
            Button(
                onClick = onButtonClick,
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                modifier = Modifier.fillMaxWidth(),
                enabled = isValidLocation
            ) {
                Text(buttonText, color = Color.White)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    onImageCaptured: (String) -> Unit,
    onCancel: () -> Unit,
    isUploading: Boolean,
    uploadError: String?
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary

    Box(modifier = Modifier.fillMaxSize()) {
        // Fullscreen camera preview
        AndroidView(
            factory = { ctx -> PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } },
            modifier = Modifier.fillMaxSize(),
            update = { previewView ->
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build()
                    val imageCaptureBuilder = ImageCapture.Builder()
                    imageCapture = imageCaptureBuilder.build()
                    val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture
                        )
                        preview.setSurfaceProvider(previewView.surfaceProvider)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(context))
            }
        )

        // Slim custom top bar overlay (smaller than default TopAppBar)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(40.dp)
                .background(primary)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCancel) { Text("❌", color = onPrimary) }
                Spacer(Modifier.width(4.dp))
                Text("Chụp ảnh chấm công", color = onPrimary, style = MaterialTheme.typography.titleMedium)
            }
        }

        // Upload overlay
        if (isUploading) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(48.dp))
                    Text("Đang gửi ảnh...", color = Color.White, style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        // Error overlay
        if (uploadError != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("❌ Lỗi", style = MaterialTheme.typography.titleLarge, color = Color.Red)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(uploadError, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = Color.Black)
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Button(onClick = { onCancel() }, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) { Text("Hủy", color = Color.White) }
                            Button(onClick = { onCancel() }, colors = ButtonDefaults.buttonColors(containerColor = primary)) { Text("Thử lại", color = onPrimary) }
                        }
                    }
                }
            }
        }

        // Capture button bottom-center
        if (!isUploading && uploadError == null) {
            Button(
                onClick = {
                    imageCapture?.let { capture ->
                        val photoFile = File(
                            context.getExternalFilesDir(null),
                            "attendance_${System.currentTimeMillis()}.jpg"
                        )
                        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                        capture.takePicture(
                            outputOptions,
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                    onImageCaptured(photoFile.absolutePath)
                                }
                                override fun onError(exception: ImageCaptureException) {
                                    exception.printStackTrace()
                                }
                            }
                        )
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
                    .size(80.dp),
                shape = RoundedCornerShape(40.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primary, contentColor = onPrimary)
            ) { Text("📸", style = MaterialTheme.typography.titleLarge, color = onPrimary) }
        }
    }
}

enum class AttendanceType {
    CHECK_IN,
    CHECK_OUT
}
//@Review(apiLevel = 33, showBackground = true, name = "Attendance Screen Preview")
//@Composable
//fun PreviewAttendanceScreen() {
//    FirstAPPTheme {
//        // Preview với callback giả để test UI
//        AttendanceScreen(
//            onShowCamera = { /* Preview không cần thực sự ẩn bottomBar */ },
//            onHideCamera = { /* Preview không cần thực sự hiện bottomBar */ }
//        )
//    }
//}
