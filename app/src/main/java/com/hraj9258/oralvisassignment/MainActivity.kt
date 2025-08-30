package com.hraj9258.oralvisassignment

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.hraj9258.oralvisassignment.navigation.AppNavigation
import com.hraj9258.oralvisassignment.ui.theme.OralVisAssignmentTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OralVisAssignmentTheme {
                val navController = rememberNavController()

                Scaffold {paddingValues ->
                    AppNavigation(
                        navController = navController,
                        modifier = Modifier
                            .padding(paddingValues)
                    )
                }
            }
        }
    }
}