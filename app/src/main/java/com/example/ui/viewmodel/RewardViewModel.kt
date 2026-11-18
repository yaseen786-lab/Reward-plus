package com.example.ui.viewmodel

import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.DailyOfferItem
import com.example.data.model.EarningLog
import com.example.data.model.FraudEvent
import com.example.data.model.OfferTier
import com.example.data.model.PayoutMethod
import com.example.data.model.ReferralRecord
import com.example.data.model.RewardedAdSlot
import com.example.data.model.SurveyPartnerItem
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.model.WithdrawalRequest
import com.example.data.repository.RewardRepository
import com.example.data.security.FraudAssessment
import com.example.data.security.FraudGuard
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RewardViewModel(
    private val repository: RewardRepository,
    private val fraudGuard: FraudGuard,
    private val currentUserId: String,
    private val userDisplayName: String,
    private val userEmail: String,
    private val userPhotoUrl: String?
) : ViewModel() {

    val isAdmin: Boolean = repository.isVerifiedAdmin()

    private val _fraudAssessment = MutableStateFlow(fraudGuard.assessEnvironment())
    val fraudAssessment: StateFlow<FraudAssessment> = _fraudAssessment.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _bannerMessage = MutableStateFlow<Pair<Boolean, String>?>(null) // isError to message
    val bannerMessage: StateFlow<Pair<Boolean, String>?> = _bannerMessage.asStateFlow()

    val userProfileState: StateFlow<UiState<UserProfile?>> = repository.observeUserProfile(currentUserId)
        .asUiState("Failed to load wallet profile")

    val earningLogsState: StateFlow<UiState<List<EarningLog>>> = repository.observeEarningLogs(currentUserId)
        .asUiState("Failed to load earning logs")

    val withdrawalsState: StateFlow<UiState<List<WithdrawalRequest>>> = repository.observeWithdrawals(currentUserId)
        .asUiState("Failed to load withdrawals")

    val referralsState: StateFlow<UiState<List<ReferralRecord>>> = repository.observeReferrals(currentUserId)
        .asUiState("Failed to load referrals")

    val fraudEventsState: StateFlow<UiState<List<FraudEvent>>> = repository.observeUserFraudEvents(currentUserId)
        .asUiState("Failed to load security logs")

    // Admin Streams (only instantiated/collected when isAdmin == true)
    val adminUsersState: StateFlow<UiState<List<UserProfile>>> = if (isAdmin) {
        repository.observeAllUsers(currentUserId).asUiState("Failed to load users")
    } else {
        MutableStateFlow(UiState.Success(emptyList()))
    }

    val adminWithdrawalsState: StateFlow<UiState<List<WithdrawalRequest>>> = if (isAdmin) {
        repository.observeAllWithdrawals(currentUserId).asUiState("Failed to load payout queue")
    } else {
        MutableStateFlow(UiState.Success(emptyList()))
    }

    val adminEarningLogsState: StateFlow<UiState<List<EarningLog>>> = if (isAdmin) {
        repository.observeAllEarningLogs(currentUserId).asUiState("Failed to load global earnings")
    } else {
        MutableStateFlow(UiState.Success(emptyList()))
    }

    val adminFraudEventsState: StateFlow<UiState<List<FraudEvent>>> = if (isAdmin) {
        repository.observeAllFraudEvents(currentUserId).asUiState("Failed to load fraud events")
    } else {
        MutableStateFlow(UiState.Success(emptyList()))
    }

    init {
        initializeSession()
    }

    private fun initializeSession() {
        viewModelScope.launch {
            val assessment = fraudGuard.assessEnvironment()
            _fraudAssessment.value = assessment
            val res = repository.ensureUserProfile(
                displayName = userDisplayName,
                email = userEmail,
                photoUrl = userPhotoUrl,
                assessment = assessment
            )
            res.onFailure { err ->
                _bannerMessage.value = true to (err.localizedMessage ?: "Profile sync failed")
            }
        }
    }

    fun refreshSecurityScan() {
        _fraudAssessment.value = fraudGuard.assessEnvironment()
    }

    fun syncPendingReferrals(profile: UserProfile) {
        viewModelScope.launch {
            val res = repository.claimPendingReferralRewards(profile)
            res.onSuccess { claimed ->
                if (claimed > 0) {
                    _bannerMessage.value = false to "Credited +$claimed pts from new referral invites (500 base + 40% bonus)!"
                }
            }
        }
    }

    fun clearBanner() {
        _bannerMessage.value = null
    }

    fun showError(message: String) {
        _bannerMessage.value = true to message
    }

    fun showInfo(message: String) {
        _bannerMessage.value = false to message
    }

    fun currentDateKey(): String = repository.todayKey()

    fun awardOfferTicket(
        profile: UserProfile,
        offer: DailyOfferItem,
        adDurationSeconds: Int,
        adStartedElapsedMs: Long
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val requiredDwell = (adDurationSeconds * 1000L) - 500L
            val assessment = fraudGuard.assessEnvironment(adStartedElapsedMs, requiredDwell)
            _fraudAssessment.value = assessment
            val result = repository.awardOfferTicket(
                profile = profile,
                offer = offer,
                adDurationSeconds = adDurationSeconds,
                assessment = assessment
            )
            fraudGuard.recordActionTimestamp()
            result.onSuccess { msg -> _bannerMessage.value = false to msg }
                .onFailure { err -> _bannerMessage.value = true to (err.localizedMessage ?: "Ticket failed") }
            _isBusy.value = false
        }
    }

    fun claimOfferWithTickets(profile: UserProfile, offer: DailyOfferItem) {
        viewModelScope.launch {
            _isBusy.value = true
            val assessment = fraudGuard.assessEnvironment()
            _fraudAssessment.value = assessment
            val result = repository.claimCompletedTicketOffer(
                profile = profile,
                offer = offer,
                assessment = assessment
            )
            result.onSuccess { msg -> _bannerMessage.value = false to msg }
                .onFailure { err -> _bannerMessage.value = true to (err.localizedMessage ?: "Claim failed") }
            _isBusy.value = false
        }
    }

    fun completeAdWatch(profile: UserProfile, adSlot: RewardedAdSlot, startedElapsedMs: Long) {
        viewModelScope.launch {
            _isBusy.value = true
            val requiredDwell = (adSlot.watchSeconds * 1000L) - 500L
            val assessment = fraudGuard.assessEnvironment(startedElapsedMs, requiredDwell)
            _fraudAssessment.value = assessment
            val result = repository.completeRewardAction(
                profile = profile,
                sourceType = "REWARDED_AD",
                title = "${adSlot.networkName}: ${adSlot.title}",
                pointsAwarded = adSlot.pointsReward,
                countsTowardDailyOfferLimit = false,
                isAdWatch = true,
                assessment = assessment
            )
            fraudGuard.recordActionTimestamp()
            result.onSuccess { msg -> _bannerMessage.value = false to msg }
                .onFailure { err -> _bannerMessage.value = true to (err.localizedMessage ?: "Ad reward failed") }
            _isBusy.value = false
        }
    }

    fun completeSurvey(profile: UserProfile, survey: SurveyPartnerItem, startedElapsedMs: Long) {
        viewModelScope.launch {
            _isBusy.value = true
            val assessment = fraudGuard.assessEnvironment(startedElapsedMs, 3500L)
            _fraudAssessment.value = assessment
            val result = repository.completeRewardAction(
                profile = profile,
                sourceType = survey.sourceType,
                title = "${survey.partnerName}: ${survey.topic}",
                pointsAwarded = survey.pointsReward,
                countsTowardDailyOfferLimit = false,
                isAdWatch = false,
                assessment = assessment
            )
            fraudGuard.recordActionTimestamp()
            result.onSuccess { msg -> _bannerMessage.value = false to msg }
                .onFailure { err -> _bannerMessage.value = true to (err.localizedMessage ?: "Survey failed") }
            _isBusy.value = false
        }
    }

    fun applyReferralCode(profile: UserProfile, code: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val assessment = fraudGuard.assessEnvironment()
            _fraudAssessment.value = assessment
            val result = repository.applyReferralCode(profile, code, assessment)
            result.onSuccess { msg -> _bannerMessage.value = false to msg }
                .onFailure { err -> _bannerMessage.value = true to (err.localizedMessage ?: "Invalid referral code") }
            _isBusy.value = false
        }
    }

    fun submitWithdrawal(
        profile: UserProfile,
        method: PayoutMethod,
        pointsToRedeem: Int,
        upiId: String?,
        paytmNumber: String?,
        bankAccountNumber: String?,
        ifscCode: String?,
        accountHolderName: String?,
        redeemEmail: String?
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val assessment = fraudGuard.assessEnvironment()
            _fraudAssessment.value = assessment
            val result = repository.submitWithdrawal(
                profile = profile,
                method = method,
                pointsToRedeem = pointsToRedeem,
                upiId = upiId,
                paytmNumber = paytmNumber,
                bankAccountNumber = bankAccountNumber,
                ifscCode = ifscCode,
                accountHolderName = accountHolderName,
                redeemEmail = redeemEmail,
                assessment = assessment
            )
            result.onSuccess { msg -> _bannerMessage.value = false to msg }
                .onFailure { err -> _bannerMessage.value = true to (err.localizedMessage ?: "Withdrawal failed") }
            _isBusy.value = false
        }
    }

    // --- ADMIN OPERATIONS ---

    fun approveWithdrawal(withdrawal: WithdrawalRequest) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.approveAndSettleWithdrawal(withdrawal)
                .onSuccess { msg -> _bannerMessage.value = false to msg }
                .onFailure { err -> _bannerMessage.value = true to (err.localizedMessage ?: "Failed to approve") }
            _isBusy.value = false
        }
    }

    fun rejectWithdrawal(withdrawal: WithdrawalRequest, reason: String) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.rejectWithdrawal(withdrawal, reason)
                .onSuccess { _bannerMessage.value = false to "Withdrawal rejected" }
                .onFailure { err -> _bannerMessage.value = true to (err.localizedMessage ?: "Failed to reject") }
            _isBusy.value = false
        }
    }

    fun setUserAccountStatus(targetUid: String, newStatus: String) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.updateUserAccountStatus(targetUid, newStatus)
                .onSuccess { _bannerMessage.value = false to "User status updated to $newStatus" }
                .onFailure { err -> _bannerMessage.value = true to (err.localizedMessage ?: "Failed to update user") }
            _isBusy.value = false
        }
    }

    fun resolveFraudEvent(eventId: String) {
        viewModelScope.launch {
            repository.resolveFraudEvent(eventId)
                .onSuccess { _bannerMessage.value = false to "Fraud alert marked resolved" }
                .onFailure { err -> _bannerMessage.value = true to (err.localizedMessage ?: "Failed to resolve alert") }
        }
    }

    fun nowElapsedMs(): Long = SystemClock.elapsedRealtime()

    private fun <T> Flow<T>.asUiState(fallbackMessage: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, fallbackMessage, rootError)
                emit(UiState.Error(rootError.message ?: fallbackMessage))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    private companion object {
        const val TAG = "RewardViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
