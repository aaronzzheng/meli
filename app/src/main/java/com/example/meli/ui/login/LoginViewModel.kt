package com.example.meli.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class LoginViewModel : ViewModel() {

    enum class LoginState {
        SIGN_IN, CREATE_ACCOUNT
    }

    private val _state = MutableLiveData(LoginState.SIGN_IN)
    val state: LiveData<LoginState> = _state

    private val _navigateToSignIn = MutableLiveData<Boolean>()
    val navigateToSignIn: LiveData<Boolean> = _navigateToSignIn

    private val _navigateToHome = MutableLiveData<Boolean>()
    val navigateToHome: LiveData<Boolean> = _navigateToHome

    fun toggleState() {
        _state.value = if (_state.value == LoginState.SIGN_IN) {
            LoginState.CREATE_ACCOUNT
        } else {
            LoginState.SIGN_IN
        }
    }

    fun onActionButtonClicked() {
        if (_state.value == LoginState.CREATE_ACCOUNT) {
            // Simulate account creation
            _state.value = LoginState.SIGN_IN
            _navigateToSignIn.value = true
        } else {
            // Handle Sign In logic - simulate success
            _navigateToHome.value = true
        }
    }

    fun onNavigationConsumed() {
        _navigateToSignIn.value = false
        _navigateToHome.value = false
    }
}