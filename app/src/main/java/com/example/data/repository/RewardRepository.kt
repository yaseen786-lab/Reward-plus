package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.EarningLog
import com.example.data.model.EconomyConfig
import com.example.data.model.FraudEvent
import com.example.data.model.PayoutMethod
import com.example.data.model.ReferralRecord
import com.example.data.model.UserProfile
import com.example.data.model.WithdrawalRequest
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.example.data.security.FraudAssessment
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class RewardRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun todayKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun isVerifiedAdmin(): Boolean {
        val user = auth.currentUser ?: return false
        return user.email?.equals(EconomyConfig.BOOTSTRAP_ADMIN_EMAIL, ignoreCase = true) == true &&
            user.isEmailVerified
    }

    private fun generateReferralCode(uid: String): String {
        val clean = uid.replace(Regex("[^A-Za-z0-9]"), "").uppercase(Locale.US)
        val suffix = clean.take(6).ifEmpty { "USER01" }
        return "RP$suffix"
    }

    suspend fun ensureUserProfile(
        displayName: String,
        email: String,
        photoUrl: String?,
        assessment: FraudAssessment
    ): Result<Unit> {
        var docPath = "users/unknown"
        return try {
            val uid = requireUserId()
            val userRef = db.collection("users").document(uid)
            docPath = userRef.path
            val snapshot = userRef.get().await()
            val today = todayKey()
            val safeName = displayName.trim().ifBlank { "RewardPulse User" }.take(60)
            val safeEmail = email.trim().ifBlank { "user@rewardpulse.app" }.take(120)

            if (!snapshot.exists()) {
                val refCode = generateReferralCode(uid)
                val userPayload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "email" to safeEmail,
                    "photoUrl" to photoUrl?.take(500),
                    "pointsBalance" to 0L,
                    "lifetimePointsEarned" to 0L,
                    "lifetimePointsWithdrawn" to 0L,
                    "referralCode" to refCode,
                    "referralCount" to 0L,
                    "referralBonusPoints" to 0L,
                    "referralCommissionPoints" to 0L,
                    "dailyOfferDate" to today,
                    "dailyOfferCount" to 0L,
                    "dailyAdCount" to 0L,
                    "megaOfferTickets" to 0L,
                    "normalOffer1Tickets" to 0L,
                    "normalOffer2Tickets" to 0L,
                    "fraudRiskScore" to assessment.riskScore.toLong(),
                    "accountStatus" to "ACTIVE",
                    "deviceHash" to assessment.deviceHash,
                    "role" to "USER",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }

                val codeRef = db.collection("referral_codes").document(refCode)
                val codePayload = mapOf(
                    "ownerUid" to uid,
                    "referralCode" to refCode,
                    "ownerName" to safeName,
                    "createdAt" to FieldValue.serverTimestamp()
                )

                val batch = db.batch()
                batch.set(userRef, userPayload)
                batch.set(codeRef, codePayload)
                batch.commit().await()
            } else {
                val existingDate = snapshot.getString("dailyOfferDate") ?: ""
                val updates = mutableMapOf<String, Any>(
                    "displayName" to safeName,
                    "deviceHash" to assessment.deviceHash,
                    "fraudRiskScore" to assessment.riskScore.toLong(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!photoUrl.isNullOrBlank()) {
                    updates["photoUrl"] = photoUrl.take(500)
                }
                if (existingDate != today) {
                    updates["dailyOfferDate"] = today
                    updates["dailyOfferCount"] = 0L
                    updates["dailyAdCount"] = 0L
                }
                userRef.update(updates).await()

                // Ensure referral_codes lookup doc exists for legacy profiles
                val existingCode = snapshot.getString("referralCode")
                if (!existingCode.isNullOrBlank()) {
                    val codeRef = db.collection("referral_codes").document(existingCode)
                    val codeSnap = codeRef.get().await()
                    if (!codeSnap.exists()) {
                        codeRef.set(
                            mapOf(
                                "ownerUid" to uid,
                                "referralCode" to existingCode,
                                "ownerName" to safeName,
                                "createdAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snap -> snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error -> logAndRethrowFlowError(error, OperationType.GET, path, userId) }
        )
    }

    fun observeEarningLogs(userId: String): Flow<List<EarningLog>> = flow {
        val path = "earning_logs"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(EarningLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error -> logAndRethrowFlowError(error, OperationType.LIST, path, userId) }
        )
    }

    fun observeWithdrawals(userId: String): Flow<List<WithdrawalRequest>> = flow {
        val path = "withdrawals"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(WithdrawalRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error -> logAndRethrowFlowError(error, OperationType.LIST, path, userId) }
        )
    }

    fun observeReferrals(userId: String): Flow<List<ReferralRecord>> = flow {
        val path = "referrals"
        emitAll(
            db.collection(path)
                .whereEqualTo("inviterUid", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ReferralRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error -> logAndRethrowFlowError(error, OperationType.LIST, path, userId) }
        )
    }

    fun observeUserFraudEvents(userId: String): Flow<List<FraudEvent>> = flow {
        val path = "fraud_events"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(FraudEvent::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error -> logAndRethrowFlowError(error, OperationType.LIST, path, userId) }
        )
    }

    // --- ADMIN OBSERVATION STREAMS ---

    fun observeAllUsers(adminUid: String): Flow<List<UserProfile>> = flow {
        val path = "users"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snap ->
                    snap.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.lifetimePointsEarned }
                }
                .catch { error -> logAndRethrowFlowError(error, OperationType.LIST, path, adminUid) }
        )
    }

    fun observeAllWithdrawals(adminUid: String): Flow<List<WithdrawalRequest>> = flow {
        val path = "withdrawals"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snap ->
                    snap.toObjects(WithdrawalRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error -> logAndRethrowFlowError(error, OperationType.LIST, path, adminUid) }
        )
    }

    fun observeAllEarningLogs(adminUid: String): Flow<List<EarningLog>> = flow {
        val path = "earning_logs"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snap ->
                    snap.toObjects(EarningLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error -> logAndRethrowFlowError(error, OperationType.LIST, path, adminUid) }
        )
    }

    fun observeAllFraudEvents(adminUid: String): Flow<List<FraudEvent>> = flow {
        val path = "fraud_events"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snap ->
                    snap.toObjects(FraudEvent::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error -> logAndRethrowFlowError(error, OperationType.LIST, path, adminUid) }
        )
    }

    // --- QUIZ OFFER TICKETS (1 AD = 1 TICKET; 30 TICKETS = 1 OFFER COMPLETED) ---

    suspend fun awardOfferTicket(
        profile: UserProfile,
        offer: com.example.data.model.DailyOfferItem,
        adDurationSeconds: Int,
        assessment: FraudAssessment
    ): Result<String> {
        var docPath = "users/${profile.userId}"
        return try {
            val uid = requireUserId()
            check(uid == profile.userId) { "Signed-in user mismatch" }
            if (profile.accountStatus == "FROZEN") {
                return Result.failure(IllegalStateException("Account is frozen by anti-fraud security."))
            }
            if (assessment.blockActivity) {
                logFraudEventInternal(
                    uid = uid,
                    userName = profile.displayName,
                    eventType = if (assessment.isRooted) "ROOT_DETECTED" else if (assessment.isVpnActive) "VPN_ACTIVE" else "RAPID_TAP_VELOCITY",
                    severity = assessment.riskLevel,
                    riskPoints = assessment.riskScore,
                    details = assessment.signals.joinToString("; ").take(240),
                    deviceHash = assessment.deviceHash
                )
                return Result.failure(
                    IllegalStateException("Security Shield blocked ticket: ${assessment.signals.firstOrNull() ?: "Suspicious activity"}")
                )
            }

            val today = todayKey()
            val currentDailyOffers = if (profile.dailyOfferDate == today) profile.dailyOfferCount else 0L
            val currentDailyAds = if (profile.dailyOfferDate == today) profile.dailyAdCount else 0L
            if (currentDailyOffers >= EconomyConfig.DAILY_OFFER_LIMIT) {
                return Result.failure(
                    IllegalStateException("Daily limit of ${EconomyConfig.DAILY_OFFER_LIMIT} offers reached! Come back tomorrow.")
                )
            }

            val currentTickets = profile.ticketsForOffer(offer.id)
            if (currentTickets >= EconomyConfig.TICKETS_REQUIRED_PER_OFFER) {
                return Result.success("30/30 tickets ready! Tap Claim ${offer.pointsReward} pts!")
            }

            val newTickets = (currentTickets + 1).coerceAtMost(EconomyConfig.TICKETS_REQUIRED_PER_OFFER)
            val userRef = db.collection("users").document(uid)
            docPath = userRef.path

            userRef.update(
                mapOf(
                    offer.ticketFieldName to newTickets.toLong(),
                    "dailyOfferDate" to today,
                    "dailyAdCount" to currentDailyAds + 1L,
                    "fraudRiskScore" to assessment.riskScore.toLong(),
                    "deviceHash" to assessment.deviceHash,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val msg = if (newTickets >= EconomyConfig.TICKETS_REQUIRED_PER_OFFER) {
                "Ticket $newTickets/30 earned! Tap Claim +${offer.pointsReward} pts!"
            } else {
                "+1 Ticket earned • $newTickets / ${EconomyConfig.TICKETS_REQUIRED_PER_OFFER} Tickets"
            }
            Result.success(msg)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun claimCompletedTicketOffer(
        profile: UserProfile,
        offer: com.example.data.model.DailyOfferItem,
        assessment: FraudAssessment
    ): Result<String> {
        var docPath = "users/${profile.userId}"
        return try {
            val uid = requireUserId()
            check(uid == profile.userId) { "Signed-in user mismatch" }
            if (profile.accountStatus == "FROZEN") {
                return Result.failure(IllegalStateException("Account is frozen by anti-fraud security."))
            }
            val currentTickets = profile.ticketsForOffer(offer.id)
            if (currentTickets < EconomyConfig.TICKETS_REQUIRED_PER_OFFER) {
                return Result.failure(
                    IllegalStateException("Need ${EconomyConfig.TICKETS_REQUIRED_PER_OFFER - currentTickets} more tickets to claim this offer.")
                )
            }
            val today = todayKey()
            val currentDailyOffers = if (profile.dailyOfferDate == today) profile.dailyOfferCount else 0L
            if (currentDailyOffers >= EconomyConfig.DAILY_OFFER_LIMIT) {
                return Result.failure(
                    IllegalStateException("Daily limit of ${EconomyConfig.DAILY_OFFER_LIMIT} offers reached!")
                )
            }

            val pointsAwarded = offer.pointsReward
            val (grossInr, userInr, adminInr) = EconomyConfig.splitRevenueInr(pointsAwarded)
            val sourceType = if (offer.tier == com.example.data.model.OfferTier.MEGA) "MEGA_OFFER" else "NORMAL_OFFER"

            val userRef = db.collection("users").document(uid)
            docPath = userRef.path
            val logId = "log_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            val logRef = db.collection("earning_logs").document(logId)

            val batch = db.batch()
            batch.update(
                userRef,
                mapOf(
                    offer.ticketFieldName to 0L,
                    "pointsBalance" to profile.pointsBalance + pointsAwarded.toLong(),
                    "lifetimePointsEarned" to profile.lifetimePointsEarned + pointsAwarded.toLong(),
                    "dailyOfferDate" to today,
                    "dailyOfferCount" to currentDailyOffers + 1L,
                    "fraudRiskScore" to assessment.riskScore.toLong(),
                    "deviceHash" to assessment.deviceHash,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                logRef,
                mapOf(
                    "userId" to uid,
                    "userName" to profile.displayName.ifBlank { "User" }.take(60),
                    "sourceType" to sourceType,
                    "title" to "${offer.title} (30/30 Tickets)",
                    "pointsAwarded" to pointsAwarded.toLong(),
                    "grossValueInr" to grossInr,
                    "userShareInr" to userInr,
                    "adminShareInr" to adminInr,
                    "fraudRiskScore" to assessment.riskScore.toLong(),
                    "verificationStatus" to if (assessment.riskScore >= 40) "FLAGGED" else "VERIFIED",
                    "dateKey" to today,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()

            Result.success("Offer completed! +$pointsAwarded pts credited (₹$userInr • 70% share)")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    // --- EARNING ACTIONS (OFFERS, REWARDED ADS, SURVEYS) ---

    suspend fun completeRewardAction(
        profile: UserProfile,
        sourceType: String,
        title: String,
        pointsAwarded: Int,
        countsTowardDailyOfferLimit: Boolean,
        isAdWatch: Boolean,
        assessment: FraudAssessment
    ): Result<String> {
        var docPath = "users/${profile.userId}"
        return try {
            val uid = requireUserId()
            check(uid == profile.userId) { "Signed-in user mismatch" }
            if (profile.accountStatus == "FROZEN") {
                return Result.failure(IllegalStateException("Account is frozen by anti-fraud security. Contact Admin."))
            }

            if (assessment.blockActivity) {
                logFraudEventInternal(
                    uid = uid,
                    userName = profile.displayName,
                    eventType = if (assessment.isRooted) "ROOT_DETECTED" else if (assessment.isVpnActive) "VPN_ACTIVE" else "RAPID_TAP_VELOCITY",
                    severity = assessment.riskLevel,
                    riskPoints = assessment.riskScore,
                    details = assessment.signals.joinToString("; ").take(240),
                    deviceHash = assessment.deviceHash
                )
                return Result.failure(
                    IllegalStateException("Security Shield blocked claim: ${assessment.signals.firstOrNull() ?: "Suspicious activity"}")
                )
            }

            val today = todayKey()
            val currentDailyOffers = if (profile.dailyOfferDate == today) profile.dailyOfferCount else 0L
            val currentDailyAds = if (profile.dailyOfferDate == today) profile.dailyAdCount else 0L

            if (countsTowardDailyOfferLimit && currentDailyOffers >= EconomyConfig.DAILY_OFFER_LIMIT) {
                logFraudEventInternal(
                    uid = uid,
                    userName = profile.displayName,
                    eventType = "DAILY_LIMIT_REACHED",
                    severity = "LOW",
                    riskPoints = 15,
                    details = "Daily offer limit of ${EconomyConfig.DAILY_OFFER_LIMIT} reached for $today",
                    deviceHash = assessment.deviceHash
                )
                return Result.failure(
                    IllegalStateException("Daily limit of ${EconomyConfig.DAILY_OFFER_LIMIT} offers reached! Come back tomorrow or try Surveys & Ads.")
                )
            }

            require(pointsAwarded in 1..2500) { "Points reward out of valid range" }
            val (grossInr, userInr, adminInr) = EconomyConfig.splitRevenueInr(pointsAwarded)

            val userRef = db.collection("users").document(uid)
            docPath = userRef.path
            val logId = "log_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            val logRef = db.collection("earning_logs").document(logId)

            val newBalance = profile.pointsBalance + pointsAwarded.toLong()
            val newLifetime = profile.lifetimePointsEarned + pointsAwarded.toLong()
            val newOfferCount = if (countsTowardDailyOfferLimit) currentDailyOffers + 1L else currentDailyOffers
            val newAdCount = if (isAdWatch) currentDailyAds + 1L else currentDailyAds

            val userUpdates = mapOf(
                "pointsBalance" to newBalance,
                "lifetimePointsEarned" to newLifetime,
                "dailyOfferDate" to today,
                "dailyOfferCount" to newOfferCount,
                "dailyAdCount" to newAdCount,
                "fraudRiskScore" to assessment.riskScore.toLong(),
                "deviceHash" to assessment.deviceHash,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val verificationStatus = if (assessment.riskScore >= 40) "FLAGGED" else "VERIFIED"
            val logPayload = mapOf(
                "userId" to uid,
                "userName" to profile.displayName.ifBlank { "User" }.take(60),
                "sourceType" to sourceType,
                "title" to title.take(120),
                "pointsAwarded" to pointsAwarded.toLong(),
                "grossValueInr" to grossInr,
                "userShareInr" to userInr,
                "adminShareInr" to adminInr,
                "fraudRiskScore" to assessment.riskScore.toLong(),
                "verificationStatus" to verificationStatus,
                "dateKey" to today,
                "createdAt" to FieldValue.serverTimestamp()
            )

            val batch = db.batch()
            batch.update(userRef, userUpdates)
            batch.set(logRef, logPayload)
            batch.commit().await()

            if (assessment.riskScore >= 30) {
                logFraudEventInternal(
                    uid = uid,
                    userName = profile.displayName,
                    eventType = if (assessment.isVpnActive) "VPN_ACTIVE" else "ELEVATED_RISK",
                    severity = assessment.riskLevel,
                    riskPoints = assessment.riskScore,
                    details = assessment.signals.joinToString("; ").take(240),
                    deviceHash = assessment.deviceHash
                )
            }

            Result.success("+$pointsAwarded pts credited (70% user share • ₹$userInr)")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    // --- REFERRAL SYSTEM (500 PTS BASE + 40% COMMISSION BONUS) ---

    suspend fun applyReferralCode(
        profile: UserProfile,
        rawCode: String,
        assessment: FraudAssessment
    ): Result<String> {
        val code = rawCode.trim().uppercase(Locale.US)
        var docPath = "referral_codes/$code"
        return try {
            val uid = requireUserId()
            if (code.length < 4 || code.length > 24 || !code.matches(Regex("^[A-Za-z0-9_\\-]+$"))) {
                return Result.failure(IllegalArgumentException("Enter a valid referral code (4–24 alphanumeric chars)."))
            }
            if (!profile.referredByCode.isNullOrBlank()) {
                return Result.failure(IllegalStateException("Referral code already linked to your account."))
            }
            if (code == profile.referralCode) {
                logFraudEventInternal(
                    uid = uid,
                    userName = profile.displayName,
                    eventType = "SELF_REFERRAL_ATTEMPT",
                    severity = "HIGH",
                    riskPoints = 55,
                    details = "User attempted to apply own referral code $code",
                    deviceHash = assessment.deviceHash
                )
                return Result.failure(IllegalStateException("Self-referral is blocked by Anti-Fraud Guard."))
            }

            val codeSnap = db.collection("referral_codes").document(code).get().await()
            if (!codeSnap.exists()) {
                return Result.failure(IllegalArgumentException("Referral code '$code' not found."))
            }
            val inviterUid = codeSnap.getString("ownerUid") ?: ""
            if (inviterUid.isBlank() || inviterUid == uid) {
                return Result.failure(IllegalStateException("Invalid referral owner."))
            }

            val referralRef = db.collection("referrals").document(uid)
            docPath = referralRef.path
            if (referralRef.get().await().exists()) {
                return Result.failure(IllegalStateException("Referral reward already attributed for this account."))
            }

            val userRef = db.collection("users").document(uid)
            val welcomeBonus = 250L // Invitee welcome starter bonus
            val (grossInr, userInr, adminInr) = EconomyConfig.splitRevenueInr(welcomeBonus.toInt())
            val logId = "log_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            val logRef = db.collection("earning_logs").document(logId)

            val referralPayload = mapOf(
                "inviterUid" to inviterUid,
                "inviteeUid" to uid,
                "inviteeName" to profile.displayName.ifBlank { "Friend" }.take(60),
                "referralCode" to code,
                "baseBonusPoints" to EconomyConfig.REFERRAL_BASE_POINTS.toLong(),
                "commissionRatePercent" to EconomyConfig.REFERRAL_COMMISSION_PERCENT.toLong(),
                "commissionPoints" to EconomyConfig.REFERRAL_COMMISSION_POINTS.toLong(),
                "claimedByInviter" to false,
                "status" to "PENDING_CLAIM",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val batch = db.batch()
            batch.set(referralRef, referralPayload)
            batch.update(
                userRef,
                mapOf(
                    "referredByCode" to code,
                    "referredByUid" to inviterUid,
                    "pointsBalance" to profile.pointsBalance + welcomeBonus,
                    "lifetimePointsEarned" to profile.lifetimePointsEarned + welcomeBonus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                logRef,
                mapOf(
                    "userId" to uid,
                    "userName" to profile.displayName.ifBlank { "User" }.take(60),
                    "sourceType" to "REFERRAL_BONUS",
                    "title" to "Invite Code Linked ($code) Welcome Bonus",
                    "pointsAwarded" to welcomeBonus,
                    "grossValueInr" to grossInr,
                    "userShareInr" to userInr,
                    "adminShareInr" to adminInr,
                    "fraudRiskScore" to assessment.riskScore.toLong(),
                    "verificationStatus" to "VERIFIED",
                    "dateKey" to todayKey(),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()

            Result.success("Referral linked! Inviter earns 500 pts + 40% bonus, and you got +$welcomeBonus pts!")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun claimPendingReferralRewards(profile: UserProfile): Result<Int> {
        val uid = requireUserId()
        var docPath = "referrals"
        return try {
            val pendingSnap = db.collection("referrals")
                .whereEqualTo("inviterUid", uid)
                .get()
                .await()

            val unclaimed = pendingSnap.toObjects(ReferralRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .filter { !it.claimedByInviter }

            if (unclaimed.isEmpty()) return Result.success(0)

            var runningBalance = profile.pointsBalance
            var runningLifetime = profile.lifetimePointsEarned
            var runningRefCount = profile.referralCount
            var runningBaseBonus = profile.referralBonusPoints
            var runningCommBonus = profile.referralCommissionPoints
            var totalClaimedPts = 0

            val userRef = db.collection("users").document(uid)
            val today = todayKey()

            for (record in unclaimed) {
                val basePts = record.baseBonusPoints.toInt() // 500
                val commPts = record.commissionPoints.toInt() // 200 (40% of 500)
                val totalForRecord = basePts + commPts // 700 (<= 2500 rule cap)
                val (grossBase, userBase, adminBase) = EconomyConfig.splitRevenueInr(basePts)
                val (grossComm, userComm, adminComm) = EconomyConfig.splitRevenueInr(commPts)

                runningBalance += totalForRecord
                runningLifetime += totalForRecord
                runningRefCount += 1L
                runningBaseBonus += basePts
                runningCommBonus += commPts
                totalClaimedPts += totalForRecord

                val refDoc = db.collection("referrals").document(record.id)
                docPath = refDoc.path
                val baseLogRef = db.collection("earning_logs").document("refb_${UUID.randomUUID().toString().take(12)}")
                val commLogRef = db.collection("earning_logs").document("refc_${UUID.randomUUID().toString().take(12)}")

                val batch = db.batch()
                batch.update(
                    refDoc,
                    mapOf(
                        "claimedByInviter" to true,
                        "status" to "CREDITED",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(
                    userRef,
                    mapOf(
                        "pointsBalance" to runningBalance,
                        "lifetimePointsEarned" to runningLifetime,
                        "referralCount" to runningRefCount,
                        "referralBonusPoints" to runningBaseBonus,
                        "referralCommissionPoints" to runningCommBonus,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.set(
                    baseLogRef,
                    mapOf(
                        "userId" to uid,
                        "userName" to profile.displayName.ifBlank { "User" }.take(60),
                        "sourceType" to "REFERRAL_BONUS",
                        "title" to "Referral Base Reward (${record.inviteeName})",
                        "pointsAwarded" to basePts.toLong(),
                        "grossValueInr" to grossBase,
                        "userShareInr" to userBase,
                        "adminShareInr" to adminBase,
                        "fraudRiskScore" to profile.fraudRiskScore,
                        "verificationStatus" to "VERIFIED",
                        "dateKey" to today,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.set(
                    commLogRef,
                    mapOf(
                        "userId" to uid,
                        "userName" to profile.displayName.ifBlank { "User" }.take(60),
                        "sourceType" to "REFERRAL_COMMISSION",
                        "title" to "40% Referral Commission (${record.inviteeName})",
                        "pointsAwarded" to commPts.toLong(),
                        "grossValueInr" to grossComm,
                        "userShareInr" to userComm,
                        "adminShareInr" to adminComm,
                        "fraudRiskScore" to profile.fraudRiskScore,
                        "verificationStatus" to "VERIFIED",
                        "dateKey" to today,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.commit().await()
            }
            Result.success(totalClaimedPts)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    // --- WITHDRAWAL SUBMISSION (UPI, PAYTM, IMPS, BANK TRANSFER, GOOGLE PLAY REDEEM) ---

    suspend fun submitWithdrawal(
        profile: UserProfile,
        method: PayoutMethod,
        pointsToRedeem: Int,
        upiId: String?,
        paytmNumber: String?,
        bankAccountNumber: String?,
        ifscCode: String?,
        accountHolderName: String?,
        redeemEmail: String?,
        assessment: FraudAssessment
    ): Result<String> {
        var docPath = "withdrawals"
        return try {
            val uid = requireUserId()
            if (profile.accountStatus == "FROZEN") {
                return Result.failure(IllegalStateException("Account is frozen due to security review."))
            }
            if (pointsToRedeem < EconomyConfig.MIN_WITHDRAWAL_POINTS) {
                return Result.failure(IllegalArgumentException("Minimum withdrawal is ${EconomyConfig.MIN_WITHDRAWAL_POINTS} pts (₹10)."))
            }
            if (pointsToRedeem > profile.pointsBalance) {
                return Result.failure(IllegalArgumentException("Insufficient points balance."))
            }
            if (assessment.blockActivity) {
                return Result.failure(IllegalStateException("Withdrawal blocked by Anti-Fraud Shield: ${assessment.signals.firstOrNull()}"))
            }

            val destinationMasked = when (method) {
                PayoutMethod.UPI -> {
                    val vpa = upiId?.trim().orEmpty()
                    if (!vpa.matches(Regex("^[a-zA-Z0-9.\\-_]{2,40}@[a-zA-Z]{2,20}$"))) {
                        return Result.failure(IllegalArgumentException("Enter a valid UPI ID (e.g. name@upi or mobile@ybl)."))
                    }
                    vpa
                }
                PayoutMethod.PAYTM -> {
                    val num = paytmNumber?.trim().orEmpty()
                    if (!num.matches(Regex("^[6-9][0-9]{9}$")) && !num.contains("@paytm")) {
                        return Result.failure(IllegalArgumentException("Enter a valid 10-digit Paytm number or @paytm UPI."))
                    }
                    if (num.length == 10) "Paytm ••••••${num.takeLast(4)}" else num
                }
                PayoutMethod.IMPS, PayoutMethod.BANK_TRANSFER -> {
                    val holder = accountHolderName?.trim().orEmpty()
                    val acct = bankAccountNumber?.trim().orEmpty()
                    val ifsc = ifscCode?.trim()?.uppercase(Locale.US).orEmpty()
                    if (holder.length < 3) {
                        return Result.failure(IllegalArgumentException("Enter account holder name."))
                    }
                    if (!acct.matches(Regex("^[0-9]{9,18}$"))) {
                        return Result.failure(IllegalArgumentException("Enter a valid 9–18 digit bank account number."))
                    }
                    if (!ifsc.matches(Regex("^[A-Z]{4}0[A-Z0-9]{6}$"))) {
                        return Result.failure(IllegalArgumentException("Enter a valid 11-character IFSC code (e.g. HDFC0001234)."))
                    }
                    "${ifsc.take(4)} •••• ${acct.takeLast(4)}"
                }
                PayoutMethod.GOOGLE_PLAY_REDEEM -> {
                    val mail = redeemEmail?.trim().orEmpty().ifBlank { profile.email }
                    if (!mail.contains("@") || mail.length < 5) {
                        return Result.failure(IllegalArgumentException("Enter a valid email for Google Play Redeem delivery."))
                    }
                    "Play Code -> $mail"
                }
            }

            val amountInr = EconomyConfig.pointsToUserInr(pointsToRedeem)
            val fastTrack = assessment.riskScore < 35 &&
                (method == PayoutMethod.IMPS || method == PayoutMethod.UPI || method == PayoutMethod.PAYTM || method == PayoutMethod.GOOGLE_PLAY_REDEEM)

            val initialStatus = when {
                assessment.riskScore >= 45 -> "FLAGGED"
                method == PayoutMethod.IMPS || method == PayoutMethod.UPI || method == PayoutMethod.PAYTM -> "PROCESSING_IMPS"
                else -> "PENDING"
            }

            val withdrawalId = "wd_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            val wdRef = db.collection("withdrawals").document(withdrawalId)
            docPath = wdRef.path
            val userRef = db.collection("users").document(uid)

            val wdPayload = mapOf(
                "userId" to uid,
                "userName" to profile.displayName.ifBlank { "User" }.take(60),
                "userEmail" to profile.email.take(120),
                "method" to method.id,
                "pointsRedeemed" to pointsToRedeem.toLong(),
                "amountInr" to amountInr,
                "destinationMasked" to destinationMasked.take(120),
                "upiId" to upiId?.trim()?.takeIf { it.isNotBlank() }?.take(100),
                "paytmNumber" to paytmNumber?.trim()?.takeIf { it.isNotBlank() }?.take(20),
                "bankAccountNumber" to bankAccountNumber?.trim()?.takeIf { it.isNotBlank() }?.take(30),
                "ifscCode" to ifscCode?.trim()?.uppercase(Locale.US)?.takeIf { it.isNotBlank() }?.take(15),
                "accountHolderName" to accountHolderName?.trim()?.takeIf { it.isNotBlank() }?.take(80),
                "redeemEmail" to redeemEmail?.trim()?.takeIf { it.isNotBlank() }?.take(120),
                "status" to initialStatus,
                "fastTrackEligible" to fastTrack,
                "fraudRiskScore" to assessment.riskScore.toLong(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            val batch = db.batch()
            batch.set(wdRef, wdPayload)
            batch.update(
                userRef,
                mapOf(
                    "pointsBalance" to (profile.pointsBalance - pointsToRedeem.toLong()).coerceAtLeast(0L),
                    "lifetimePointsWithdrawn" to profile.lifetimePointsWithdrawn + pointsToRedeem.toLong(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()

            val msg = if (initialStatus == "PROCESSING_IMPS") {
                "Fast ${method.label} payout of ₹$amountInr queued in IMPS Fast-Track!"
            } else {
                "${method.label} withdrawal of ₹$amountInr submitted for fulfillment."
            }
            Result.success(msg)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    // --- ADMIN ACTIONS ---

    suspend fun approveAndSettleWithdrawal(withdrawal: WithdrawalRequest): Result<String> {
        val docPath = "withdrawals/${withdrawal.id}"
        return try {
            require(isVerifiedAdmin()) { "Admin privileges required." }
            val generatedRef = when (withdrawal.method) {
                PayoutMethod.GOOGLE_PLAY_REDEEM.id -> {
                    val seg = { UUID.randomUUID().toString().replace("-", "").uppercase(Locale.US).take(4) }
                    "GP-${seg()}-${seg()}-${seg()}-${seg()}"
                }
                PayoutMethod.IMPS.id, PayoutMethod.BANK_TRANSFER.id ->
                    "IMPS${System.currentTimeMillis().toString().takeLast(12)}"
                PayoutMethod.UPI.id, PayoutMethod.PAYTM.id ->
                    "UPI${System.currentTimeMillis().toString().takeLast(12)}"
                else -> "RP${System.currentTimeMillis().toString().takeLast(10)}"
            }

            val note = if (withdrawal.method == PayoutMethod.GOOGLE_PLAY_REDEEM.id) {
                "Google Play Redeem Code issued"
            } else {
                "Instant ${withdrawal.method} payout settled"
            }

            db.collection("withdrawals").document(withdrawal.id)
                .update(
                    mapOf(
                        "status" to "COMPLETED",
                        "utrOrRedeemCode" to generatedRef,
                        "adminNote" to note,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success("Settled with reference $generatedRef")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun rejectWithdrawal(withdrawal: WithdrawalRequest, reason: String): Result<Unit> {
        val docPath = "withdrawals/${withdrawal.id}"
        return try {
            require(isVerifiedAdmin()) { "Admin privileges required." }
            db.collection("withdrawals").document(withdrawal.id)
                .update(
                    mapOf(
                        "status" to "REJECTED",
                        "adminNote" to reason.ifBlank { "Rejected during security review" }.take(200),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateUserAccountStatus(targetUserId: String, newStatus: String): Result<Unit> {
        val docPath = "users/$targetUserId"
        return try {
            require(isVerifiedAdmin()) { "Admin privileges required." }
            require(newStatus in listOf("ACTIVE", "FLAGGED", "FROZEN"))
            db.collection("users").document(targetUserId)
                .update(
                    mapOf(
                        "accountStatus" to newStatus,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun resolveFraudEvent(eventId: String): Result<Unit> {
        val docPath = "fraud_events/$eventId"
        return try {
            require(isVerifiedAdmin()) { "Admin privileges required." }
            db.collection("fraud_events").document(eventId)
                .update(mapOf("resolved" to true))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    private suspend fun logFraudEventInternal(
        uid: String,
        userName: String,
        eventType: String,
        severity: String,
        riskPoints: Int,
        details: String,
        deviceHash: String
    ) {
        val eventId = "fr_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val path = "fraud_events/$eventId"
        try {
            db.collection("fraud_events").document(eventId)
                .set(
                    mapOf(
                        "userId" to uid,
                        "userName" to userName.ifBlank { "User" }.take(60),
                        "eventType" to eventType.take(48),
                        "severity" to severity,
                        "riskPoints" to riskPoints.toLong(),
                        "details" to details.take(250),
                        "deviceHash" to deviceHash,
                        "resolved" to false,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
        }
    }

    private suspend fun logAndRethrowFlowError(
        error: Throwable,
        operationType: OperationType,
        path: String,
        expectedUid: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
        throw error
    }
}
