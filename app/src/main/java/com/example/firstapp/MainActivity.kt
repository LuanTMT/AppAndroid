// MainActivity.kt
package com.example.firstapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.example.firstapp.data.UserPreferences
import com.example.firstapp.ui.AttendanceScreen
import com.example.firstapp.ui.LoginScreen
import com.example.firstapp.ui.ProfileScreen
import com.example.firstapp.ui.theme.FirstAPPTheme
import com.example.firstapp.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch
import com.example.firstapp.network.TokenProvider
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigator() {
    val context = LocalContext.current
    var isLoggedIn by remember { mutableStateOf<Boolean?>(null) }
    val scope = rememberCoroutineScope()

    // Kiểm tra token khi mở app
    LaunchedEffect(Unit) {
        val token = UserPreferences.getToken(context)
        TokenProvider.token = token
        Log.d("MainActivity", "Loaded token: ${!token.isNullOrEmpty()} (hidden)")
        isLoggedIn = !token.isNullOrEmpty()
    }

    if (isLoggedIn == null) {
        // Đang kiểm tra token, có thể show splash/loading
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (isLoggedIn == false) {
        // Chưa đăng nhập
        LoginScreen(
            onLoginSuccess = { isLoggedIn = true }
        )
    } else {
        // Đã đăng nhập, show app như cũ
        val navController = rememberNavController()
        val currentBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = currentBackStackEntry?.destination?.route
        var showBottomBar by remember { mutableStateOf(true) }

        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar {
                        NavigationBarItem(
                            icon = { Text("Chấm công") },
                            label = { Text("Attendance") },
                            selected = currentRoute == "attendance",
                            onClick = {
                                navController.navigate("attendance") {
                                    popUpTo(navController.graph.startDestinationId)
                                    launchSingleTop = true
                                }
                            }
                        )
                        NavigationBarItem(
                            icon = { Text("Tài khoản") },
                            label = { Text("Account") },
                            selected = currentRoute == "account",
                            onClick = {
                                navController.navigate("account") {
                                    popUpTo(navController.graph.startDestinationId)
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = "attendance",
                modifier = Modifier.padding(padding)
            ) {
                composable("attendance") {
                    AttendanceScreen(
                        onShowCamera = { showBottomBar = false },
                        onHideCamera = { showBottomBar = true }
                    )
                }
                composable("account") {
                    ProfileScreen() // <-- không truyền viewModel nữa
                }
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            FirstAPPTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.bg),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Surface(modifier = Modifier.fillMaxSize()) {
                        AppNavigator()
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Attendance Screen Preview")
@Composable
fun PreviewAttendanceScreen() {
    FirstAPPTheme {
        AppNavigator()
    }
}
