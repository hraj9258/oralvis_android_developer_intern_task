package com.hraj9258.oralvisassignment.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hraj9258.oralvisassignment.ui.screen.CameraScreen
import com.hraj9258.oralvisassignment.ui.screen.HomeScreen
import com.hraj9258.oralvisassignment.ui.screen.SearchScreen
import com.hraj9258.oralvisassignment.ui.screen.SessionDetailsScreen

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = modifier
            .fillMaxSize()
    ) {
        composable("home") {
            HomeScreen(navController = navController)
        }

        composable("camera") {
            CameraScreen(navController = navController)
        }

        composable("search") {
            SearchScreen(navController = navController)
        }

        composable("session_details/{sessionId}") { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            SessionDetailsScreen(
                sessionId = sessionId,
                navController = navController
            )
        }
    }
}