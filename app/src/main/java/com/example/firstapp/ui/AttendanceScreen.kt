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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.rememberImagePainter
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
import com.example.firstapp.ui.theme.FirstAPPTheme
import androidx.compose.ui.tooling.preview.Preview as Review

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
    var attendanceData by remember {
        mutableStateOf(
            mapOf(
                "checkIn" to null,
                "checkOut" to null
            )
        )
    }
    var showCameraModal by remember { mutableStateOf(false) }
    var clockType by remember { mutableStateOf<String?>(null) } // "checkIn"/"checkOut"/"update_check_in"/"update_check_out"
    var currentTime by remember { mutableStateOf(Date()) }
    var capturedImage by remember { mutableStateOf<String?>(null) }

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

    // Hiển thị CameraScreen khi showCameraModal = true
    if (showCameraModal) {
        CameraScreen(
            onImageCaptured = { path ->
                // Xử lý khi chụp xong ảnh
                // viewModel.updateImagePath(path) // Nếu cần lưu ảnh vào ViewModel
                showCameraModal = false
            },
            onCancel = {
                showCameraModal = false
            }
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

    // State theo dõi scroll
    val listState = rememberLazyListState()
    var lastScrollOffset by remember { mutableStateOf(0) }
    var showChamCongBar by remember { mutableStateOf(true) }
    LaunchedEffect(listState.firstVisibleItemScrollOffset) {
        val currentOffset = listState.firstVisibleItemScrollOffset
        showChamCongBar = currentOffset < lastScrollOffset || currentOffset == 0
        lastScrollOffset = currentOffset
    }

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
                        .fillMaxWidth().height(50.dp),
                    contentScale = ContentScale.Fit
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.2f))
                )
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(86.dp))
            AnimatedVisibility(
                visible = showChamCongBar,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Chấm công",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                    }
                }
            }
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    AttendanceCard(
                        title = "Giờ Vào",
                        time = formatTime(currentTime),
                        address = address,
                        status = if (attendanceData["checkIn"] != null) "Đã check in" else null,
                        statusColor = if (attendanceData["checkIn"] != null) Color.Green else null,
                        imagePath = (attendanceData["checkIn"] as? Map<*, *>)?.get("image") as? String,
                        buttonText = if (attendanceData["checkIn"] != null) "Cập nhật giờ vào" else "Chấm công vào",
                        buttonColor = Color(0xFFFFA726),
                        onButtonClick = {
                            clockType = if (attendanceData["checkIn"] != null) "update_check_in" else "checkIn"
                            showCameraModal = true
                        },
                        isValidLocation = isValidLocation,
                        nameLocal = nameLocal
                    )
                }
                item {
                    AttendanceCard(
                        title = "Giờ Ra",
                        time = formatTime(currentTime),
                        address = address,
                        status = if (attendanceData["checkOut"] != null) "Đã check out" else null,
                        statusColor = if (attendanceData["checkOut"] != null) Color.Green else null,
                        imagePath = (attendanceData["checkOut"] as? Map<*, *>)?.get("image") as? String,
                        buttonText = if (attendanceData["checkOut"] != null) "Cập nhật giờ ra" else "Chấm công ra",
                        buttonColor = Color(0xFF7C4DFF),
                        onButtonClick = {
                            clockType = if (attendanceData["checkOut"] != null) "update_check_out" else "checkOut"
                            showCameraModal = true
                        },
                        isValidLocation = isValidLocation,
                        nameLocal = nameLocal
                    )
                }
            }
        }
    }
    // TODO: Thêm CameraModal, fetchAttendanceData, toast, cập nhật dữ liệu sau khi chấm công
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
                Image(
                    painter = rememberImagePainter(imagePath),
                    contentDescription = null,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
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
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    val yellowPrimary = MaterialTheme.colorScheme.primary
    val yellowDark = MaterialTheme.colorScheme.onPrimary

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Chụp ảnh chấm công",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.fillMaxWidth(),
                        color = yellowDark
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Text("❌", color = yellowDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = yellowPrimary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            // Camera Preview
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        this.scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                },
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

            // Capture Button
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
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
                        .size(80.dp),
                    shape = RoundedCornerShape(40.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = yellowPrimary,
                        contentColor = yellowDark
                    )
                ) {
                    Text("📸", style = MaterialTheme.typography.titleLarge, color = yellowDark)
                }
            }
        }
    }
}

enum class AttendanceType {
    CHECK_IN,
    CHECK_OUT
}

@Review(apiLevel = 33, showBackground = true, name = "Attendance Screen Preview")
@Composable
fun PreviewAttendanceScreen() {
    FirstAPPTheme {
        AttendanceScreen()
    }
}