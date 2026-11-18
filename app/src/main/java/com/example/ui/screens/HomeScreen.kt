package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.EconomyConfig
import com.example.data.model.FraudEvent
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
import com.example.ui.theme.WarningOrange

@Composable
fun HomeScreen(
    profile: UserProfile,
    todayKey: String,
    isAdmin: Boolean,
    fraudAssessment: FraudAssessment,
    fraudEvents: List<FraudEvent>,
    onRefreshSecurity: () -> Unit,
    onNavigateToEarn: () -> Unit,
    onNavigateToSurveys: () -> Unit,
    onNavigateToWallet: () -> Unit,
    onNavigateToRefer: () -> Unit,
    onNavigateToWebsite: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onCopiedNotice: (String) -> Unit,
    onSignOut: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val dailyOffersUsed = if (profile.dailyOfferDate == todayKey) profile.dailyOfferCount.toInt() else 0
    val inrBalance = EconomyConfig.pointsToUserInr(profile.pointsBalance.toInt())
    val initials = profile.displayName
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
        .ifEmpty { "RP" }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Clean Profile & Balance Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                listOf(ObsidianCardElevated.copy(alpha = 0.65f), ObsidianCard)
                            )
                        )
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(EmeraldPrimary, CyanAccent))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color(0xFF041C16),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = profile.displayName,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(
                                    imageVector = Icons.Filled.VerifiedUser,
                                    contentDescription = "Verified account",
                                    tint = EmeraldBright,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = profile.email,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        StatusPill(
                            text = if (profile.accountStatus == "ACTIVE") "Active" else profile.accountStatus,
                            color = if (profile.accountStatus == "ACTIVE") EmeraldBright else DangerRed
                        )
                    }

                    // Simple Balance & Quick Play Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProfileStatTile(
                            label = "Your Balance",
                            value = "%,d pts".format(profile.pointsBalance),
                            sub = "₹%.2f".format(inrBalance),
                            accent = EmeraldBright,
                            modifier = Modifier.weight(1f)
                        )
                        ProfileStatTile(
                            label = "Total Earned",
                            value = "%,d pts".format(profile.lifetimePointsEarned),
                            sub = "Today: $dailyOffersUsed / ${EconomyConfig.DAILY_OFFER_LIMIT}",
                            accent = GoldBright,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Copyable Invite Code
                    Surface(
                        color = ObsidianBg.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Your Invite Code (Earn 500 pts + 40%)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = profile.referralCode,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = EmeraldBright,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(profile.referralCode))
                                    onCopiedNotice("Referral code ${profile.referralCode} copied!")
                                },
                                modifier = Modifier.testTag("home_copy_code_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = "Copy invite code",
                                    tint = CyanAccent
                                )
                            }
                        }
                    }

                    Button(
                        onClick = onNavigateToEarn,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = Color(0xFF022C22)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("home_play_quiz_button")
                    ) {
                        Icon(Icons.Filled.Bolt, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Start Earning Now", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 2. Admin Console Entry Card (for verified Admin only)
        if (isAdmin) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1914)),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, GoldAmber.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToAdmin() }
                        .testTag("home_admin_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AdminPanelSettings,
                                contentDescription = null,
                                tint = GoldBright,
                                modifier = Modifier.size(26.dp)
                            )
                            Column {
                                Text(
                                    text = "Admin Dashboard",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = GoldBright,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Manage earnings, payouts & users",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Button(
                            onClick = onNavigateToAdmin,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldAmber,
                                contentColor = Color(0xFF0A1322)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Open", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 3. Quiz Ticket Progress Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                shape = RoundedCornerShape(20.dp),
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.ConfirmationNumber,
                                contentDescription = null,
                                tint = GoldBright,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Your Quiz Tickets",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "30 = Reward",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanAccent
                        )
                    }

                    TicketProgressBarRow("Mega Quiz (1,500 pts)", profile.megaOfferTickets.toInt(), GoldAmber)
                    TicketProgressBarRow("Normal Quiz 1 (800 pts)", profile.normalOffer1Tickets.toInt(), EmeraldPrimary)
                    TicketProgressBarRow("Normal Quiz 2 (800 pts)", profile.normalOffer2Tickets.toInt(), CyanAccent)
                }
            }
        }

        // 4. Quick Access Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickShortcutCard(
                    icon = Icons.Filled.Poll,
                    title = "Surveys",
                    subtitle = "PubScale & GoodWalls",
                    accent = CyanAccent,
                    onClick = onNavigateToSurveys,
                    modifier = Modifier.weight(1f)
                )
                QuickShortcutCard(
                    icon = Icons.Filled.AccountBalanceWallet,
                    title = "Withdraw",
                    subtitle = "UPI, IMPS & Play",
                    accent = EmeraldBright,
                    onClick = onNavigateToWallet,
                    modifier = Modifier.weight(1f)
                )
                QuickShortcutCard(
                    icon = Icons.Filled.GroupAdd,
                    title = "Refer",
                    subtitle = "500 + 40%",
                    accent = GoldBright,
                    onClick = onNavigateToRefer,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 5. Compact Security Shield Pill
        item {
            Surface(
                color = ObsidianCard,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onRefreshSecurity)
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
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = "Account protection",
                            tint = if (fraudAssessment.riskScore < 35) EmeraldBright else WarningOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = if (fraudAssessment.riskScore < 35) "Account Protected" else "Security Check Needed",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White
                            )
                            Text(
                                text = if (fraudEvents.any { !it.resolved }) {
                                    "Tap to refresh device check"
                                } else {
                                    "Verified device • Ready for instant payout"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = "Scan",
                        style = MaterialTheme.typography.labelMedium,
                        color = CyanAccent
                    )
                }
            }
        }

        // 5b. Official Website & Play Store Info Card
        item {
            Surface(
                color = ObsidianCard,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.28f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToWebsite)
                    .testTag("home_website_card")
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
                        Icon(
                            imageVector = Icons.Filled.Language,
                            contentDescription = "Official Website",
                            tint = CyanAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Official App Website",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White
                            )
                            Text(
                                text = "View earnings calculator, FAQ, Privacy Policy & Terms",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = "Open",
                        style = MaterialTheme.typography.labelMedium,
                        color = CyanAccent
                    )
                }
            }
        }

        // 6. Sign Out Action
        item {
            OutlinedButton(
                onClick = onSignOut,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("home_sign_out_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = DangerRed,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Sign Out", color = DangerRed, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun StatusPill(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.16f),
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ProfileStatTile(
    label: String,
    value: String,
    sub: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = ObsidianCardElevated,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = accent,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = sub,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TicketProgressBarRow(title: String, tickets: Int, accent: Color) {
    val progress = (tickets.toFloat() / EconomyConfig.TICKETS_REQUIRED_PER_OFFER).coerceIn(0f, 1f)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.bodySmall, color = Color.White)
            Text(
                text = "$tickets / ${EconomyConfig.TICKETS_REQUIRED_PER_OFFER}",
                style = MaterialTheme.typography.labelSmall,
                color = accent
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50)),
            color = accent,
            trackColor = ObsidianCardElevated
        )
    }
}

@Composable
private fun QuickShortcutCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ObsidianCardElevated),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accent,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
