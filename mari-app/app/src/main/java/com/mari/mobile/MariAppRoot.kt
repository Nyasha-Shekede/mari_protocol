package com.Mari.mobile

import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.Mari.mobile.ui.auth.AuthScreen
import com.Mari.mobile.ui.main.MainScreen
import com.Mari.mobile.ui.send.SendFlowScreen
import com.Mari.mobile.ui.receive.ReceiveScreen
import com.Mari.mobile.ui.theme.MariTheme

@Composable
fun MariAppRoot() {
    val navController = rememberNavController()
    var isAuthenticated by remember { mutableStateOf(false) }
    
    MariTheme {
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
            }
        }
    }
}
