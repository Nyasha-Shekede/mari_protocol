package com.Mari.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.Mari.mobile.ui.auth.AuthScreen
import com.Mari.mobile.ui.main.MainScreen
import com.Mari.mobile.ui.send.SendFlowScreen
import com.Mari.mobile.ui.receive.ReceiveScreen
import com.Mari.mobile.ui.theme.MariTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MariTheme {
                MariAppRoot()
            }
        }
    }
}

@Composable
fun MariAppRoot() {
    val navController = rememberNavController()
    var isAuthenticated by remember { mutableStateOf(false) }
    
    if (!isAuthenticated) {
        AuthScreen(
            onAuthSuccess = { isAuthenticated = true }
        )
    } else {
        NavHost(
            navController = navController,
            startDestination = "main"
        ) {
            composable("main") {
                MainScreen(
                    onSendClick = { navController.navigate("send") },
                    onReceiveClick = { navController.navigate("receive") },
                    onLogout = { isAuthenticated = false }
                )
            }
            composable("send") {
                SendFlowScreen(
                    onBack = { navController.popBackStack() },
                    onComplete = { navController.navigate("main") {
                        popUpTo("main") { inclusive = false }
                    }}
                )
            }
            composable("receive") {
                ReceiveScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
