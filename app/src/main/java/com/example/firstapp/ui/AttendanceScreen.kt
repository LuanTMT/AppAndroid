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
import java.text.SimpleDateFormat
import java.util.*
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

    var showCamera by remember { mutableStateOf(false) }
    var currentAttendanceType by remember { mutableStateOf<AttendanceType?>(null) }

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
                        .height(screenHeight / 7) // hoặc 56.dp, hoặc 1/6 màn hình
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.topbar),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Overlay mờ nếu muốn
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.Black.copy(alpha = 0.2f))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
//                        Text(
//                            "Chấm Công",
//                            style = MaterialTheme.typography.titleLarge,
//                            color = Color.White
//                        )
                    }
                }
            },
            containerColor = Color.Transparent // Để ảnh nền hiển thị trọn vẹn
        )  { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize(),
//                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Location Info Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
//                    colors = CardDefaults.cardColors(containerColor = yellowPrimary)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "📍 Vị trí hiện tại:",
                            style = MaterialTheme.typography.titleMedium,
//                            color = yellowDark
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        currentLocation?.let { location ->
                            Text("Latitude: ${location.latitude}", color = Color.DarkGray)
                            Text("Longitude: ${location.longitude}", color = Color.DarkGray)
                            Text("Độ chính xác: ${location.accuracy}m", color = Color.DarkGray)
                        } ?: Text("Đang lấy vị trí...", color = yellowDark)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "⏰ ${SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault()).format(Date())}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = yellowDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Action Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box (
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Button(
                            onClick = {
                                currentAttendanceType = AttendanceType.CHECK_IN
                                showCamera = true
                            },
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(56.dp),
                            enabled = attendanceState !is AttendanceState.Loading && currentLocation != null,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFF9C4),
                                contentColor = Color(0xFFFFF9C4)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = "📷 CheckIn",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color(0xFFFFF9C4)
                            )

                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            currentAttendanceType = AttendanceType.CHECK_OUT
                            showCamera = true
                        },
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(56.dp),
                        enabled = attendanceState !is AttendanceState.Loading && currentLocation != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = yellowPrimary,
                            contentColor = yellowDark
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Box (
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ){
                            Text(
                                text = "📷 CheckOut",
                                style = MaterialTheme.typography.titleLarge,
                                color = yellowDark
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Status
                when (val state = attendanceState) {
                    is AttendanceState.Loading -> {
                        CircularProgressIndicator(color = yellowPrimary)
                        Text(
                            "Đang xử lý...",
                            color = yellowDark,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    is AttendanceState.Success -> {
                        Text(
                            text = state.message,
                            color = yellowDark,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    is AttendanceState.Error -> {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    else -> {}
                }
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