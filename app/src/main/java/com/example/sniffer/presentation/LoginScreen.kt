package com.example.sniffer.presentation

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.sniffer.presentation.Screen
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.flow.collectLatest

@Composable
fun LoginScreen(navController: NavController, viewModel: LoginViewModel = viewModel()) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val userName = currentUser.email ?: "Пользователь"
            navController.navigate(Screen.MainScreen.withArgs(userName)) {
                popUpTo(Screen.LoginScreen.route) { inclusive = true }
            }
            return@LaunchedEffect
        }
        viewModel.loginResult.collectLatest { result ->
            if (result.isSuccess) {
                val targetName = viewModel.email.ifBlank { "Пользователь" }
                navController.navigate(Screen.MainScreen.withArgs(targetName))
            } else {
                val errorMessage = getLocalizedErrorMessage(result.exceptionOrNull())
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(32.dp)
    ) {
        Text(
            text = "Sniffer",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 24.dp)
        )
        TextField(
            value = viewModel.email,
            onValueChange = { viewModel.email = it },
            label = { Text("Почта") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        TextField(
            value = viewModel.password,
            onValueChange = { viewModel.password = it },
            label = { Text("Пароль") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (viewModel.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = { viewModel.onRegisterClick() }) {
                    Text(text = "Регистрация")
                }
                Button(
                    onClick = { viewModel.onLoginClick() }
                ) {
                    Text(text = "Вход")
                }
            }
        }
    }
}

fun getLocalizedErrorMessage(exception: Throwable?): String {
    return when (exception) {
        // 1. Ошибка отсутствия интернета на самом смартфоне
        is FirebaseNetworkException -> {
            "Отсутствует подключение к интернету. Проверьте сеть."
        }

        // 2. Ошибка: Неверный пароль или malformed email (Firebase бросает этот класс)
        is FirebaseAuthInvalidCredentialsException -> {
            "Неверный пароль или формат почты. Попробуйте еще раз."
        }

        // 3. Ошибка: Пользователь не найден в базе данных или заблокирован
        is FirebaseAuthInvalidUserException -> {
            "Пользователь с такой почтой не найден или заблокирован."
        }

        // 4. Ошибка при РЕГИСТРАЦИИ: Пользователь с таким email уже существует
        is FirebaseAuthUserCollisionException -> {
            "Эта почта уже зарегистрирована другим пользователем."
        }

        // 5. Ошибка при РЕГИСТРАЦИИ: Слишком простой или короткий пароль
        is FirebaseAuthWeakPasswordException -> {
            "Пароль слишком слабый. Он должен быть не менее 6 символов."
        }

        // 6. Запасной вариант для остальных ошибок Firebase (если errorCode все-таки прилетит)
        is FirebaseAuthException -> {
            when (exception.errorCode) {
                "ERROR_INVALID_EMAIL" -> "Неверный формат почты."
                "ERROR_WRONG_PASSWORD" -> "Неверный пароль. Попробуйте еще раз."
                "ERROR_USER_NOT_FOUND" -> "Пользователь с такой почтой не найден."
                "ERROR_EMAIL_ALREADY_IN_USE" -> "Эта почта уже зарегистрирована."
                "ERROR_WEAK_PASSWORD" -> "Пароль должен быть не менее 6 символов."
                "ERROR_NETWORK_REQUEST_FAILED" -> "Сбой сети при связи с сервером."
                else -> "Ошибка авторизации: ${exception.localizedMessage}"
            }
        }

        // 7. Любая другая непредвиденная системная ошибка
        else -> exception?.localizedMessage ?: "Произошла неизвестная ошибка."
    }
}
