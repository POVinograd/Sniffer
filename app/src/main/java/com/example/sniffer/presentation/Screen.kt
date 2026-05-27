package com.example.sniffer.presentation

sealed class Screen (val route: String){
    object MainScreen : Screen(route = "main_screen")
    object LoginScreen : Screen(route = "login_screen")

    fun withArgs(vararg args: String): String{
        return buildString{
            append(route)
            args.forEach{ arg->
                append("/$arg")
            }
        }
    }
}