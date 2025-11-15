package com.Mari.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Mari.mobile.ui.models.Transaction as UITransaction
import com.Mari.mobileapp.domain.model.Transaction as DomainTransaction
import com.Mari.mobileapp.domain.repository.TransactionRepository
import com.Mari.mobileapp.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    
    init {
        loadData()
    }
    
    private fun loadData() {
        viewModelScope.launch {
            // Get current authenticated user
            val currentUser = userRepository.getCurrentUser()
            if (currentUser == null) {
                _uiState.update { it.copy(error = "User not authenticated", isLoading = false) }
                return@launch
            }
            
            val userBioHash = currentUser.bloodHash
            val userName = "User ${currentUser.bloodHash.take(8)}"
            val userPhone = currentUser.bloodHash.take(10)
            
            // Load balance and transactions
            combine(
                transactionRepository.getTransactionsForUser(userBioHash),
                flowOf(userBioHash)
            ) { transactions, bioHash ->
                val balance = calculateBalance(transactions)
                val uiTransactions = transactions.map { it.toUITransaction() }
                val gamificationState = calculateGamification(transactions)
                
                MainUiState(
                    balance = balance,
                    transactions = uiTransactions,
                    location = null, // Will be updated by location manager
                    gamificationState = gamificationState,
                    userName = userName,
                    userPhone = userPhone,
                    isLoading = false
                )
            }.catch { error ->
                _uiState.update { it.copy(error = error.message, isLoading = false) }
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }
    
    fun updateLocation(lat: Double, lng: Double) {
        _uiState.update {
            it.copy(location = LocationInfo(lat, lng))
        }
    }
    
    private fun calculateBalance(transactions: List<DomainTransaction>): Double {
        var balance = 0.0
        transactions.forEach { tx ->
            when (tx.type) {
                com.Mari.mobileapp.domain.model.TransactionType.RECEIVE -> {
                    if (tx.status == com.Mari.mobileapp.domain.model.TransactionStatus.COMPLETED) {
                        balance += tx.amount
                    }
                }
                com.Mari.mobileapp.domain.model.TransactionType.SEND -> {
                    if (tx.status == com.Mari.mobileapp.domain.model.TransactionStatus.COMPLETED) {
                        balance -= tx.amount
                    }
                }
            }
        }
        return balance
    }
    
    private fun calculateGamification(transactions: List<DomainTransaction>): GamificationState {
        // Get transactions from this week
        val weekStart = getWeekStartTimestamp()
        val weekTransactions = transactions.filter { it.timestamp >= weekStart }
        
        val txCount = weekTransactions.size
        val points = weekTransactions.sumOf { tx ->
            val basePoints = 1
            val bonus = if (tx.amount >= 5.0) 5 else 0
            basePoints + bonus
        }
        
        val rewards = if (txCount >= 50) listOf("100MB Data") else emptyList()
        
        return GamificationState(
            txCountWeek = txCount,
            pointsWeek = points,
            rewards = rewards
        )
    }
    
    private fun getWeekStartTimestamp(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
    
    private fun DomainTransaction.toUITransaction(): UITransaction {
        return UITransaction(
            id = this.id,
            type = when (this.type) {
                com.Mari.mobileapp.domain.model.TransactionType.SEND -> "sent"
                com.Mari.mobileapp.domain.model.TransactionType.RECEIVE -> "received"
            },
            amount = this.amount,
            peerId = this.receiverBioHash.take(10), // First 10 chars as ID
            peerName = getPeerName(this),
            timestamp = formatTimestamp(this.timestamp),
            hsmId = this.id.take(8)
        )
    }
    
    private fun getPeerName(tx: DomainTransaction): String {
        // In production, lookup name from UserRepository
        return "User ${tx.receiverBioHash.take(8)}"
    }
    
    private fun formatTimestamp(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < 60_000 -> "Just now"
            diff < 3600_000 -> "${diff / 60_000} minutes ago"
            diff < 86400_000 -> "${diff / 3600_000} hours ago"
            else -> "${diff / 86400_000} days ago"
        }
    }
}

data class MainUiState(
    val balance: Double = 0.0,
    val transactions: List<UITransaction> = emptyList(),
    val location: LocationInfo? = null,
    val gamificationState: GamificationState = GamificationState(),
    val userName: String = "",
    val userPhone: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
)

data class LocationInfo(
    val lat: Double,
    val lng: Double
)

data class GamificationState(
    val weekStartIso: String = "",
    val txCountWeek: Int = 0,
    val pointsWeek: Int = 0,
    val streakWeeks: Int = 0,
    val badges: List<String> = emptyList(),
    val rewards: List<String> = emptyList()
)
