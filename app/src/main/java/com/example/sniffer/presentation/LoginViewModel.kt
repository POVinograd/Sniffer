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
}