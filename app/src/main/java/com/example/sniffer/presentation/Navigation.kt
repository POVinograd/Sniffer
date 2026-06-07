package com.example.sniffer.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navArgument
import androidx.navigation.compose.rememberNavController
import com.example.sniffer.presentation.Screen

@Composable
fun Navigation(onLogout: () -> Unit) {
    val navController = rememberNavController()
    NavHost(navController, startDestination = Screen.LoginScreen.route){
        composable(route = Screen.LoginScreen.route){
            LoginScreen(navController = navController)
        }
        composable(
            route = Screen.MainScreen.route + "/{name}",
            arguments = listOf(
                navArgument("name") {
                    type = NavType.StringType
                    defaultValue = "Noname"
                    nullable = true
                }
            )
        ){entry ->
            val context = LocalContext.current
            LaunchedEffect(Unit) {
                SensorMonitorService.start(context)
            }
            MainScreen(
                name = entry.arguments?.getString("name"),
                onLogout = {
                    onLogout()
                    navController.navigate(Screen.LoginScreen.route){
                        popUpTo(0) { inclusive = true }
                    }
                })
        }
    }
}


