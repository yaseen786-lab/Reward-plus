package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlin.math.roundToLong

object EconomyConfig {
    const val POINTS_PER_INR = 100
    const val USER_SHARE_PERCENT = 70
    const val ADMIN_SHARE_PERCENT = 30

    const val MEGA_OFFER_POINTS = 1500
    const val NORMAL_OFFER_POINTS = 800
    const val REWARDED_AD_POINTS = 250
    const val DAILY_OFFER_LIMIT = 30
    const val TICKETS_REQUIRED_PER_OFFER = 30

    const val REFERRAL_BASE_POINTS = 500
    const val REFERRAL_COMMISSION_PERCENT = 40
    const val REFERRAL_COMMISSION_POINTS = 200 // 40% of 500

    const val MIN_WITHDRAWAL_POINTS = 1000 // Rs 10.00 minimum payout

    const val BOOTSTRAP_ADMIN_EMAIL = "nazimkhan44549763337@gmail.com"

    fun pointsToUserInr(points: Int): Double =
        (points.toDouble() / POINTS_PER_INR).let { ((it * 100.0).roundToLong()) / 100.0 }

    fun splitRevenueInr(userPoints: Int): Triple<Double, Double, Double> {
        val userInr = pointsToUserInr(userPoints)
        val grossInr = ((userInr / (USER_SHARE_PERCENT / 100.0)) * 100.0).roundToLong() / 100.0
        val adminInr = ((grossInr - userInr) * 100.0).roundToLong() / 100.0
        return Triple(grossInr, userInr, adminInr)
    }
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val pointsBalance: Long = 0L,
    val lifetimePointsEarned: Long = 0L,
    val lifetimePointsWithdrawn: Long = 0L,
    val referralCode: String = "",
    val referredByCode: String? = null,
    val referredByUid: String? = null,
    val referralCount: Long = 0L,
    val referralBonusPoints: Long = 0L,
    val referralCommissionPoints: Long = 0L,
    val dailyOfferDate: String = "",
    val dailyOfferCount: Long = 0L,
    val dailyAdCount: Long = 0L,
    val megaOfferTickets: Long = 0L,
    val normalOffer1Tickets: Long = 0L,
    val normalOffer2Tickets: Long = 0L,
    val fraudRiskScore: Long = 0L,
    val accountStatus: String = "ACTIVE",
    val deviceHash: String = "",
    val role: String = "USER",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun ticketsForOffer(offerId: String): Int = when (offerId) {
        "mega_offer_prime_wallet" -> megaOfferTickets.toInt()
        "normal_offer_cloud_drive" -> normalOffer1Tickets.toInt()
        "normal_offer_shop_pulse" -> normalOffer2Tickets.toInt()
        else -> 0
    }
}

@Keep
@IgnoreExtraProperties
data class EarningLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val sourceType: String = "NORMAL_OFFER",
    val title: String = "",
    val pointsAwarded: Long = 0L,
    val grossValueInr: Double = 0.0,
    val userShareInr: Double = 0.0,
    val adminShareInr: Double = 0.0,
    val fraudRiskScore: Long = 0L,
    val verificationStatus: String = "VERIFIED",
    val dateKey: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class WithdrawalRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val method: String = "UPI",
    val pointsRedeemed: Long = 0L,
    val amountInr: Double = 0.0,
    val destinationMasked: String = "",
    val upiId: String? = null,
    val paytmNumber: String? = null,
    val bankAccountNumber: String? = null,
    val ifscCode: String? = null,
    val accountHolderName: String? = null,
    val redeemEmail: String? = null,
    val status: String = "PENDING",
    val fastTrackEligible: Boolean = true,
    val fraudRiskScore: Long = 0L,
    val utrOrRedeemCode: String? = null,
    val adminNote: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FraudEvent(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val eventType: String = "",
    val severity: String = "LOW",
    val riskPoints: Long = 0L,
    val details: String = "",
    val deviceHash: String = "",
    val resolved: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ReferralRecord(
    @DocumentId val id: String = "",
    val inviterUid: String = "",
    val inviteeUid: String = "",
    val inviteeName: String = "",
    val referralCode: String = "",
    val baseBonusPoints: Long = 500L,
    val commissionRatePercent: Long = 40L,
    val commissionPoints: Long = 200L,
    val claimedByInviter: Boolean = false,
    val status: String = "PENDING_CLAIM",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class OfferTier {
    MEGA,
    NORMAL
}

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int
)

data class DailyOfferItem(
    val id: String,
    val ticketFieldName: String,
    val title: String,
    val subtitle: String,
    val sponsorName: String,
    val tier: OfferTier,
    val pointsReward: Int,
    val adDurationsSeconds: List<Int>,
    val quizPool: List<QuizQuestion>
)

data class RewardedAdSlot(
    val id: String,
    val networkName: String,
    val title: String,
    val pointsReward: Int,
    val watchSeconds: Int
)

data class SurveyPartnerItem(
    val id: String,
    val partnerName: String,
    val sourceType: String,
    val topic: String,
    val estimatedMinutes: Int,
    val pointsReward: Int,
    val qualificationRate: String,
    val questions: List<SurveyQuestion>
)

data class SurveyQuestion(
    val prompt: String,
    val options: List<String>
)

enum class PayoutMethod(val id: String, val label: String, val slaLabel: String) {
    UPI("UPI", "UPI Instant", "Instant • < 2 mins"),
    PAYTM("PAYTM", "Paytm Wallet / UPI", "Instant • < 2 mins"),
    IMPS("IMPS", "IMPS Fast Bank", "IMPS 24x7 • Instant"),
    BANK_TRANSFER("BANK_TRANSFER", "Bank Transfer (NEFT)", "Bank Settlement • 2–4 hrs"),
    GOOGLE_PLAY_REDEEM("GOOGLE_PLAY_REDEEM", "Google Play Redeem Code", "Digital Voucher • Instant")
}
