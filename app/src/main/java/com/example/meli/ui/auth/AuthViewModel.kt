package com.example.meli.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.meli.data.repository.AuthRepository
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val repo = AuthRepository()
    
    private val _status = MutableLiveData<Result<Unit>?>()
    val status: LiveData<Result<Unit>?> = _status

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    val currentUser get() = repo.getCurrentUser()

    fun login(email: String, pass: String) {
        if (_isLoading.value == true) return
        _isLoading.value = true
        viewModelScope.launch { 
            _status.value = repo.login(email, pass)
            _isLoading.value = false
        }
    }

    fun register(email: String, pass: String) {
        if (_isLoading.value == true) return
        _isLoading.value = true
        viewModelScope.launch { 
            _status.value = repo.register(email, pass)
            _isLoading.value = false
        }
    }

    fun logout() = repo.logout()
    
    fun clearStatus() { 
        _status.value = null 
    }
}