package com.example.meli.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.meli.data.repository.AuthRepository
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    private val authRepository = AuthRepository()

    enum class LoginState {
        SIGN_IN, CREATE_ACCOUNT
    }

    private val _state = MutableLiveData(LoginState.SIGN_IN)
    val state: LiveData<LoginState> = _state

    private val _navigateToHome = MutableLiveData<Boolean>()
    val navigateToHome: LiveData<Boolean> = _navigateToHome

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    fun toggleState() {
        _state.value = if (_state.value == LoginState.SIGN_IN) {
            LoginState.CREATE_ACCOUNT
        } else {
            LoginState.SIGN_IN
        }
    }

    fun onActionButtonClicked(email: String, pass: String, name: String = "", username: String = "") {
        if (email.isBlank() || pass.isBlank()) {
            _errorMessage.value = "Email and password cannot be empty"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            val result = if (_state.value == LoginState.CREATE_ACCOUNT) {
                if (name.isBlank()) {
                    _errorMessage.value = "Name cannot be empty"
                    _isLoading.value = false
                    return@launch
                }
                if (username.isBlank()) {
                    _errorMessage.value = "Username cannot be empty"
                    _isLoading.value = false
                    return@launch
                }
                authRepository.register(email, pass, name, username)
            } else {
                authRepository.login(email, pass)
            }

            _isLoading.value = false
            result.onSuccess {
                _navigateToHome.value = true
            }.onFailure {
                _errorMessage.value = it.message ?: "Authentication failed"
            }
        }
    }

    fun onNavigationConsumed() {
        _navigateToHome.value = false
    }
}
