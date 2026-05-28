package com.example.sniffer.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sniffer.data.FirebaseUserRepositoryImpl
import com.example.sniffer.domain.UserRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {
    private val repository: UserRepository = FirebaseUserRepositoryImpl()
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var isLoading by mutableStateOf(false)

    private val _loginResult = MutableSharedFlow<Result<Unit>>()
    val loginResult = _loginResult.asSharedFlow()

    fun onLoginClick() {
        viewModelScope.launch {
            isLoading = true
            val result = repository.login(email, password)
            _loginResult.emit(result)
            isLoading = false
        }
    }

    fun onRegisterClick(){ //метод для регистрации
        viewModelScope.launch{
            if (email.isEmpty() || password.isEmpty()) {
                _loginResult.emit(Result.failure(Exception("Поля не должны быть пустыми")))
                return@launch
            }
            if (password.length < 6) {
                _loginResult.emit(Result.failure(Exception("Пароль должен быть от 6 символов")))
                return@launch
            }
            isLoading = true

            com.google.firebase.auth.FirebaseAuth.getInstance()
                .createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener{
                    task->viewModelScope.launch {
                        isLoading = false
                        if (task.isSuccessful){
                            _loginResult.emit(Result.success(Unit))
                        }
                        else{
                            val excep = task.exception ?: Exception("Ошибка регистрации")
                            _loginResult.emit(Result.failure(excep))
                        }
                }
                }


        }

    }
}