package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DailyOfferItem
import com.example.data.model.EarningLog
import com.example.data.model.EconomyConfig
import com.example.data.model.OfferTier
import com.example.data.model.QuizQuestion
import com.example.data.model.RewardCatalog
import com.example.data.model.RewardedAdSlot
import com.example.data.model.UserProfile
import com.example.data.security.FraudAssessment
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DangerRed
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAmber
import com.example.ui.theme.GoldBright
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianCardElevated
import kotlinx.coroutines.delay

@Composable
fun EarnScreen(
    profile: UserProfile,
    todayKey: String,
    logs: List<EarningLog>,
    fraudAssessment: FraudAssessment,
    isBusy: Boolean,
    nowElapsedMs: () -> Long,
    onAwardOfferTicket: (DailyOfferItem, Int, Long) -> Unit,
    onClaimTicketOffer: (DailyOfferItem) -> Unit,
    onCompleteAd: (RewardedAdSlot, Long) -> Unit,
    onNavigateToWallet: () -> Unit
) {
    var activeQuizOffer by remember { mutableStateOf<DailyOfferItem?>(null) }
    var activePostQuizAd by remember { mutableStateOf<Pair<DailyOfferItem, Int>?>(null) }
    var activePostQuizAdStartMs by remember { mutableLongStateOf(0L) }

    var activeAdSlot by remember { mutableStateOf<RewardedAdSlot?>(null) }
    var activeAdStartMs by remember { mutableLongStateOf(0L) }

    val dailyOffersUsed = if (profile.dailyOfferDate == todayKey) profile.dailyOfferCount.toInt() else 0
    val quotaProgress = (dailyOffersUsed.toFloat() / EconomyConfig.DAILY_OFFER_LIMIT).coerceIn(0f, 1f)
    val inrValue = EconomyConfig.pointsToUserInr(profile.pointsBalance.toInt())

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Clean Balance & Daily Limit Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, EmeraldPrimary.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_rewards),
                    contentDescription = "Wallet balance banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    ObsidianBg.copy(alpha = 0.84f),
                                    ObsidianBg.copy(alpha = 0.94f)
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Your Points",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = "%,d pts".format(profile.pointsBalance),
                                        style = MaterialTheme.typography.displaySmall,
                                        color = Color.White
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = "₹%.2f".format(inrValue),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = EmeraldBright,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }
                            }
                            FilledTonalButton(
                                onClick = onNavigateToWallet,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = EmeraldPrimary,
                                    contentColor = Color(0xFF022C22)
                                ),
                                modifier = Modifier.testTag("hero_withdraw_button")
                            ) {
                                Text(
                                    text = "Withdraw",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Daily Offers Completed",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$dailyOffersUsed / ${EconomyConfig.DAILY_OFFER_LIMIT}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (dailyOffersUsed >= EconomyConfig.DAILY_OFFER_LIMIT) DangerRed else EmeraldBright
                                )
                            }
                            LinearProgressIndicator(
                                progress = { quotaProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(50)),
                                color = if (dailyOffersUsed >= EconomyConfig.DAILY_OFFER_LIMIT) DangerRed else EmeraldPrimary,
                                trackColor = ObsidianCardElevated
                            )
                        }
                    }
                }
            }
        }

        // 2. Daily Quiz Offers (1 Mega Offer + 2 Normal Offers, 30 Tickets each, built-in ad durations)
        item {
            Text(
                text = "Daily Quiz Offers",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        items(RewardCatalog.dailyOffers, key = { it.id }) { offer ->
            val isMega = offer.tier == OfferTier.MEGA
            val borderColor = if (isMega) GoldAmber.copy(alpha = 0.7f) else EmeraldPrimary.copy(alpha = 0.35f)
            val quotaReached = dailyOffersUsed >= EconomyConfig.DAILY_OFFER_LIMIT
            val tickets = profile.ticketsForOffer(offer.id)
            val ticketProgress = (tickets.toFloat() / EconomyConfig.TICKETS_REQUIRED_PER_OFFER).coerceIn(0f, 1f)
            val readyToClaim = tickets >= EconomyConfig.TICKETS_REQUIRED_PER_OFFER

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isMega) Color(0xFF1B1914) else ObsidianCard
                ),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(if (isMega) 1.5.dp else 1.dp, borderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = if (isMega) GoldAmber.copy(alpha = 0.2f) else EmeraldPrimary.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isMega) Icons.Filled.Diamond else Icons.Filled.Stars,
                                    contentDescription = null,
                                    tint = if (isMega) GoldBright else EmeraldBright,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = if (isMega) "MEGA OFFER" else "NORMAL OFFER",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isMega) GoldBright else EmeraldBright
                                )
                            }
                        }
                        Text(
                            text = "+%,d pts".format(offer.pointsReward),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isMega) GoldBright else EmeraldBright,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = offer.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = offer.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Simple Ticket Progress Bar
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.ConfirmationNumber,
                                    contentDescription = null,
                                    tint = if (isMega) GoldBright else EmeraldBright,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "$tickets / ${EconomyConfig.TICKETS_REQUIRED_PER_OFFER} Tickets",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = "1 Ad = 1 Ticket",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        LinearProgressIndicator(
                            progress = { ticketProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(50)),
                            color = if (readyToClaim) EmeraldBright else if (isMega) GoldAmber else EmeraldPrimary,
                            trackColor = ObsidianCardElevated
                        )
                    }

                    if (readyToClaim) {
                        Button(
                            onClick = { onClaimTicketOffer(offer) },
                            enabled = !isBusy && !quotaReached && profile.accountStatus != "FROZEN",
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldBright,
                                contentColor = Color(0xFF022C22)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("claim_offer_${offer.id}")
                        ) {
                            Icon(Icons.Filled.Bolt, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Claim +${offer.pointsReward} pts", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = { activeQuizOffer = offer },
                            enabled = !isBusy && !quotaReached && profile.accountStatus != "FROZEN",
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isMega) GoldAmber else EmeraldPrimary,
                                contentColor = Color(0xFF0A1322)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("play_quiz_${offer.id}")
                        ) {
                            Icon(Icons.Filled.Quiz, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (quotaReached) "Daily Limit Reached" else "Play Quiz & Earn Ticket",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 3. Watch Ads & Earn Station (without showing second counts)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Watch Ads & Earn",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(RewardCatalog.rewardedAdSlots, key = { it.id }) { adSlot ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.28f)),
                            modifier = Modifier.width(200.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = adSlot.networkName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CyanAccent
                                    )
                                    Text(
                                        text = "+${adSlot.pointsReward} pts",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = GoldBright,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = adSlot.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Button(
                                    onClick = {
                                        activeAdStartMs = nowElapsedMs()
                                        activeAdSlot = adSlot
                                    },
                                    enabled = !isBusy && profile.accountStatus != "FROZEN",
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CyanAccent,
                                        contentColor = Color(0xFF04263B)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("watch_ad_${adSlot.id}")
                                ) {
                                    Icon(Icons.Filled.PlayCircleFilled, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Watch Ad", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Recent Earnings Feed
        item {
            Text(
                text = "Recent Credits",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (logs.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Play a quiz or watch an ad above to earn your first points!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(logs.take(5), key = { it.id }) { log ->
                EarningLogRow(log)
            }
        }
    }

    // Step 1: Quiz Mini-Game Dialog -> Built-in ad launches right after completion
    activeQuizOffer?.let { offer ->
        val ticketIndex = profile.ticketsForOffer(offer.id)
        val builtInAdSeconds = offer.adDurationsSeconds[ticketIndex % offer.adDurationsSeconds.size]
        QuizGameDialog(
            offer = offer,
            roundNumber = ticketIndex + 1,
            onDismiss = { activeQuizOffer = null },
            onQuizFinishedLaunchAd = {
                activeQuizOffer = null
                activePostQuizAdStartMs = nowElapsedMs()
                activePostQuizAd = offer to builtInAdSeconds
            }
        )
    }

    // Step 2: Built-In Sponsored Ad Player (no duration numbers displayed!) -> +1 Ticket!
    activePostQuizAd?.let { (offer, adDurationSec) ->
        BuiltInAdPlayerDialog(
            title = "Sponsored Reward Ad",
            badgeText = "EARN +1 TICKET",
            subtitle = "Keep this screen open until the ad finishes to collect your ticket.",
            durationSeconds = adDurationSec,
            accentColor = if (offer.tier == OfferTier.MEGA) GoldAmber else EmeraldPrimary,
            confirmLabel = "Collect +1 Ticket",
            onDismiss = { activePostQuizAd = null },
            onClaimVerified = {
                val started = activePostQuizAdStartMs
                activePostQuizAd = null
                onAwardOfferTicket(offer, adDurationSec, started)
            }
        )
    }

    // Direct Rewarded Ad Player (also hides explicit seconds)
    activeAdSlot?.let { adSlot ->
        BuiltInAdPlayerDialog(
            title = adSlot.title,
            badgeText = "${adSlot.networkName.uppercase()} • +${adSlot.pointsReward} PTS",
            subtitle = "Sponsored video is playing. Your reward unlocks automatically when complete.",
            durationSeconds = adSlot.watchSeconds,
            accentColor = CyanAccent,
            confirmLabel = "Claim +${adSlot.pointsReward} pts",
            onDismiss = { activeAdSlot = null },
            onClaimVerified = {
                val started = activeAdStartMs
                activeAdSlot = null
                onCompleteAd(adSlot, started)
            }
        )
    }
}

@Composable
private fun QuizGameDialog(
    offer: DailyOfferItem,
    roundNumber: Int,
    onDismiss: () -> Unit,
    onQuizFinishedLaunchAd: () -> Unit
) {
    val roundQuestions: List<QuizQuestion> = remember(offer.id, roundNumber) {
        val pool = offer.quizPool
        val q1 = pool[(roundNumber - 1) % pool.size]
        val q2 = pool[(roundNumber + 2) % pool.size]
        listOf(q1, q2)
    }
    var qIndex by remember(offer.id, roundNumber) { mutableIntStateOf(0) }
    var selectedOption by remember(offer.id, roundNumber, qIndex) { mutableStateOf<Int?>(null) }
    val currentQ = roundQuestions[qIndex]
    val isLast = qIndex == roundQuestions.lastIndex
    val isMega = offer.tier == OfferTier.MEGA
    val accent = if (isMega) GoldAmber else EmeraldPrimary

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianCard,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "ROUND #$roundNumber OF 30 • QUESTION ${qIndex + 1}/${roundQuestions.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = accent
                )
                Text(
                    text = offer.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = currentQ.question,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                currentQ.options.forEachIndexed { idx, opt ->
                    val isSelected = selectedOption == idx
                    val isCorrect = idx == currentQ.correctIndex
                    val borderCol = when {
                        selectedOption == null -> Color.Transparent
                        isSelected && isCorrect -> EmeraldBright
                        isSelected && !isCorrect -> DangerRed
                        else -> Color.Transparent
                    }
                    Surface(
                        color = if (isSelected) accent.copy(alpha = 0.16f) else ObsidianCardElevated,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, borderCol),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedOption = idx }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedOption = idx }
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = opt,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                        }
                    }
                }
                if (selectedOption != null) {
                    val correct = selectedOption == currentQ.correctIndex
                    Text(
                        text = if (correct) "Nice! Correct answer." else "Answer: ${currentQ.options[currentQ.correctIndex]}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (correct) EmeraldBright else GoldBright
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!isLast) {
                        qIndex += 1
                    } else {
                        onQuizFinishedLaunchAd()
                    }
                },
                enabled = selectedOption != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = Color(0xFF0A1322)
                ),
                modifier = Modifier.testTag("quiz_next_ad_button")
            ) {
                Text(if (isLast) "Continue (+1 Ticket)" else "Next")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EarningLogRow(log: EarningLog) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(EmeraldPrimary.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Verified,
                        contentDescription = null,
                        tint = EmeraldBright,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = log.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White
                    )
                    Text(
                        text = log.sourceType.replace('_', ' '),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = "+%,d pts".format(log.pointsAwarded),
                style = MaterialTheme.typography.labelLarge,
                color = EmeraldBright,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun BuiltInAdPlayerDialog(
    title: String,
    badgeText: String,
    subtitle: String,
    durationSeconds: Int,
    accentColor: Color,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onClaimVerified: () -> Unit
) {
    var secondsRemaining by remember(title, durationSeconds) { mutableIntStateOf(durationSeconds) }
    LaunchedEffect(title, durationSeconds) {
        while (secondsRemaining > 0) {
            delay(1000L)
            secondsRemaining -= 1
        }
    }
    val adFinished = secondsRemaining == 0
    val progress = 1f - (secondsRemaining.toFloat() / durationSeconds.coerceAtLeast(1))
    val btnColor by animateColorAsState(
        targetValue = if (adFinished) accentColor else ObsidianCardElevated,
        label = "ad_claim_btn_color"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianCard,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50)),
                    color = accentColor,
                    trackColor = ObsidianCardElevated
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (adFinished) Icons.Filled.CheckCircle else Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = if (adFinished) EmeraldBright else accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (adFinished) "Ad complete! Tap below to collect." else subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (adFinished) EmeraldBright else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onClaimVerified,
                enabled = adFinished,
                colors = ButtonDefaults.buttonColors(
                    containerColor = btnColor,
                    contentColor = Color(0xFF070B14)
                ),
                modifier = Modifier.testTag("confirm_verified_claim_button")
            ) {
                Text(if (adFinished) confirmLabel else "Playing Ad...")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
