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

    val currentUser get() = repo.getCurrentUser()

    fun login(email: String, pass: String) {
        viewModelScope.launch { _status.value = repo.login(email, pass) }
    }

    fun register(email: String, pass: String) {
        viewModelScope.launch { _status.value = repo.register(email, pass) }
    }

    fun logout() = repo.logout()
    fun clearStatus() { _status.value = null }
}