package com.malrang.pomodoro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malrang.pomodoro.networkRepo.SupabaseProvider
import com.malrang.pomodoro.networkRepo.SupabaseRepository
import com.malrang.pomodoro.networkRepo.User
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UserViewModel(
    private val supabase: SupabaseClient
) : ViewModel() {
    private val supabaseRepository: SupabaseRepository =
        SupabaseRepository(supabase.postgrest, supabase.storage)
    private val _userProfile = MutableStateFlow<User?>(null)
    val userProfile = _userProfile.asStateFlow()

    init {
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