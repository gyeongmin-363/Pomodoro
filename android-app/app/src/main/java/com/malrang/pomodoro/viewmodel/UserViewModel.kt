package com.malrang.pomodoro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malrang.pomodoro.networkRepo.SupabaseProvider
import com.malrang.pomodoro.networkRepo.SupabaseRepository
import com.malrang.pomodoro.networkRepo.User
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UserViewModel() : ViewModel() {
    private val supabaseRepository: SupabaseRepository
    private val _userProfile = MutableStateFlow<User?>(null)
    val userProfile = _userProfile.asStateFlow()

    init {
        val supabase = SupabaseProvider.client
        supabaseRepository = SupabaseRepository(supabase.postgrest, supabase.storage)
        fetchUserProfile()
    }

    fun fetchUserProfile() {
        viewModelScope.launch {
            val userId = SupabaseProvider.client.auth.currentUserOrNull()?.id
            if (userId != null) {
                val profile = supabaseRepository.getUserProfile(userId)
                _userProfile.value = profile
            }
        }
    }
}