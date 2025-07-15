package com.example.firstapp.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.firstapp.viewmodel.AttendanceViewModel
import com.example.firstapp.viewmodel.AttendanceState
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.io.File
import java.util.*
import java.text.SimpleDateFormat
import androidx.compose.ui.tooling.preview.Preview as Review
import com.example.firstapp.ui.theme.FirstAPPTheme
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.example.firstapp.R
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.background
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import coil.compose.rememberImagePainter
import androidx.compose.ui.draw.clip
import android.location.Location
import java.util.Locale
import kotlinx.coroutines.delay
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut

// Định dạng giờ từ Date
fun formatTime(date: Date?): String {
    if (date == null) return "--:--:--"
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(date)
}

// Lấy địa chỉ từ Location (nếu muốn dùng Geocoder thì bổ sung sau)
fun getAddressFromLocation(context: android.content.Context, location: Location?): String {
    if (location == null) return "Không xác định"
    return "Lat: %.5f, Lng: %.5f".format(location.latitude, location.longitude)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    viewModel: AttendanceViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val attendanceState by viewModel.attendanceState.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()
    val imagePath by viewModel.imagePath.collectAsState()

    // Nếu ViewModel chỉ có 1 imagePath, tạm thời dùng chung cho cả checkin/checkout
    val checkInImagePath = imagePath // TODO: Nếu tách riêng, sửa lại cho đúng
    val checkOutImagePath = imagePath // TODO: Nếu tách riêng, sửa lại cho đúng

    var showCamera by remember { mutableStateOf(false) }
    var currentAttendanceType by remember { mutableStateOf<AttendanceType?>(null) }
    var currentTime by remember { mutableStateOf(Date()) }
    var hasCheckedIn by remember { mutableStateOf(false) }
    var hasCheckedOut by remember { mutableStateOf(false) }
    var checkInTime by remember { mutableStateOf<Date?>(null) }
    var checkOutTime by remember { mutableStateOf<Date?>(null) }

    // Tự động cập nhật thời gian mỗi giây
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Date()
            delay(1000) // Cập nhật mỗi 1 giây
        }
    }

    val yellowPrimary = MaterialTheme.colorScheme.primary
    val yellowLight = MaterialTheme.colorScheme.background
    val yellowDark = MaterialTheme.colorScheme.onPrimary

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            startLocationUpdates(context, viewModel)
        }
    }

    // Check permissions on first launch
    LaunchedEffect(Unit) {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val hasPermissions = permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

        if (!hasPermissions) {
            permissionLauncher.launch(permissions)
        } else {
            startLocationUpdates(context, viewModel)
        }
    }

    // Handle state changes
    LaunchedEffect(attendanceState) {
        when (val state = attendanceState) {
            is AttendanceState.Success -> {
                showCamera = false
                currentAttendanceType = null
                // Cập nhật trạng thái check in/out thành công
                when (currentAttendanceType) {
                    AttendanceType.CHECK_IN -> {
                        hasCheckedIn = true
                        checkInTime = Date()
                    }
                    AttendanceType.CHECK_OUT -> {
                        hasCheckedOut = true
                        checkOutTime = Date()
                    }
                    else -> {}
                }
                viewModel.resetState()
            }
            is AttendanceState.Error -> {
                showCamera = false
                currentAttendanceType = null
                viewModel.resetState()
            }
            else -> {}
        }
    }

    if (showCamera) {
        CameraScreen(
            onImageCaptured = { path ->
                viewModel.updateImagePath(path)
                when (currentAttendanceType) {
                    AttendanceType.CHECK_IN -> viewModel.checkIn(context)
                    AttendanceType.CHECK_OUT -> viewModel.checkOut(context)
                    else -> {}
                }
            },
            onCancel = {
                showCamera = false
                currentAttendanceType = null
            }
        )
    } else {
        // Nội dung chính
        Scaffold(
            topBar = {
                val screenHeight = LocalConfiguration.current.screenHeightDp.dp
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
                            .fillMaxWidth()
                            .height(50.dp), // Chỉ chiếm 1 phần phía dưới
                        contentScale = ContentScale.Fit
                    )
                    // Overlay mờ nếu muốn
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.Black.copy(alpha = 0.2f))
                    )
                }
            },
            containerColor = Color.Transparent // Để ảnh nền hiển thị trọn vẹn
        )  { padding ->
            // State theo dõi scroll
            val listState = rememberLazyListState()
            var lastScrollOffset by remember { mutableStateOf(0) }
            var showChamCongBar by remember { mutableStateOf(true) }

            // Theo dõi hướng scroll
            LaunchedEffect(listState.firstVisibleItemScrollOffset) {
                val currentOffset = listState.firstVisibleItemScrollOffset
                showChamCongBar = currentOffset < lastScrollOffset || currentOffset == 0
                lastScrollOffset = currentOffset
            }

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
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.05f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "CHẤM CÔNG",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.DarkGray
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
                            time = if (hasCheckedIn) formatTime(checkInTime) else formatTime(currentTime),
                            address = getAddressFromLocation(context, currentLocation),
                            status = if (hasCheckedIn) "Đã check in" else null,
                            statusColor = if (hasCheckedIn) Color.Green else null,
                            imagePath = checkInImagePath,
                            buttonText = if (hasCheckedIn) "Cập nhật giờ vào" else "Check In",
                            buttonColor = Color(0xFFFFA726),
                            onButtonClick = {
                                currentAttendanceType = AttendanceType.CHECK_IN
                                showCamera = true
                            }
                        )
                    }
                    item {
                        AttendanceCard(
                            title = "Giờ Ra",
                            time = if (hasCheckedOut) formatTime(checkOutTime) else formatTime(currentTime),
                            address = getAddressFromLocation(context, currentLocation),
                            status = if (hasCheckedOut) "Đã check out" else null,
                            statusColor = if (hasCheckedOut) Color.Green else null,
                            imagePath = checkOutImagePath,
                            buttonText = if (hasCheckedOut) "Cập nhật giờ ra" else "Check Out",
                            buttonColor = Color(0xFF7C4DFF),
                            onButtonClick = {
                                currentAttendanceType = AttendanceType.CHECK_OUT
                                showCamera = true
                            }
                        )
                    }
                }
            }
        }
    }
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
    onButtonClick: () -> Unit
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
                    painter = rememberImagePainter(imagePath), // Nếu dùng Coil, hoặc painterResource nếu là resource
                    contentDescription = null,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onButtonClick,
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                modifier = Modifier.fillMaxWidth()
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

private fun startLocationUpdates(context: android.content.Context, viewModel: AttendanceViewModel) {
    val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    if (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                viewModel.updateLocation(it)
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