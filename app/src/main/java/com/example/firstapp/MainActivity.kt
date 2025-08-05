package com.example.firstapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.firstapp.ui.theme.FirstAPPTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.firstapp.viewmodel.UserViewModel
import com.example.firstapp.ui.AttendanceScreen
import androidx.compose.ui.tooling.preview.Preview as Review

import androidx.core.view.WindowCompat // vẽ full màn hình
import com.example.firstapp.data.UserPreferences
import com.example.firstapp.ui.LoginScreen
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigator() {
    val context = LocalContext.current
    var isLoggedIn by remember { mutableStateOf<Boolean?>(null) }
    val scope = rememberCoroutineScope()

    // Kiểm tra token khi mở app
    LaunchedEffect(Unit) {
        val token = UserPreferences.getToken(context)
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
                            icon = { Text("Danh sách") },
                            label = { Text("Users") },
                            selected = currentRoute == "userList",
                            onClick = {
                                navController.navigate("userList") {
                                    popUpTo(navController.graph.startDestinationId)
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                }
            }
        )  { padding ->
            NavHost(
                navController = navController,
                startDestination = "attendance",
            ) {
                composable("attendance") {
                    AttendanceScreen(
                        onShowCamera = { showBottomBar = false },
                        onHideCamera = { showBottomBar = true }
                    )
                }
                composable("userList") {
                    UserListScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserListScreen(viewModel: UserViewModel = viewModel()) {
    val users by viewModel.users.collectAsState()

    Scaffold() { padding ->
        LazyColumn() {
            items(users) { user ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(30.dp)
                    ) {
                        Text(
                            text = user.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = user.email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
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
                    // Ảnh nền phủ toàn màn hình
                    Image(
                        painter = painterResource(id = R.drawable.bg),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Overlay mờ nếu muốn chữ nổi bật
                    Surface(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        AppNavigator()
                    }
                }
            }
        }
    }
}

@Review(showBackground = true, name = "Attendance Screen Preview")
@Composable
fun PreviewAttendanceScreen() {
    FirstAPPTheme {
        AppNavigator()
    }
}
