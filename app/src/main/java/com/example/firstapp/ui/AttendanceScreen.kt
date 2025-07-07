package com.example.firstapp.ui

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
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
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

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
        // Camera Screen
        CameraScreen(
            onImageCaptured = { path ->
                viewModel.updateImagePath(path)
                // Submit attendance after capturing image
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
        // Main Attendance Screen
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Chấm Công") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Location Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "📍 Vị trí hiện tại:",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        currentLocation?.let { location ->
                            Text("Latitude: ${location.latitude}")
                            Text("Longitude: ${location.longitude}")
                            Text("Độ chính xác: ${location.accuracy}m")
                        } ?: Text("Đang lấy vị trí...")

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "⏰ ${SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault()).format(Date())}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = {
                            currentAttendanceType = AttendanceType.CHECK_IN
                            showCamera = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = attendanceState !is AttendanceState.Loading && currentLocation != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "📷 Chấm Công Vào",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            currentAttendanceType = AttendanceType.CHECK_OUT
                            showCamera = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = attendanceState !is AttendanceState.Loading && currentLocation != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "📷 Chấm Công Ra",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Status
                when (val state = attendanceState) {
                    is AttendanceState.Loading -> {
                        CircularProgressIndicator()
                        Text("Đang xử lý...")
                    }
                    is AttendanceState.Success -> {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    is AttendanceState.Error -> {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chụp ảnh chấm công") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Text("❌")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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
                                        // Handle error - you can show a toast or log the error
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
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("📸", style = MaterialTheme.typography.titleLarge)
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