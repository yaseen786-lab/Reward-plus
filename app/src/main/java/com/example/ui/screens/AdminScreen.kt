package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.EarningLog
import com.example.data.model.EconomyConfig
import com.example.data.model.FraudEvent
import com.example.data.model.UserProfile
import com.example.data.model.WithdrawalRequest
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DangerRed
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAmber
import com.example.ui.theme.GoldBright
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianCardElevated
import com.example.ui.theme.WarningOrange

private enum class AdminSection(val label: String) {
    PAYOUTS("Payout Queue"),
    USERS("Users & Risk"),
    FRAUD("Fraud Alerts"),
    EARNINGS("70/30 Ledger")
}

@Composable
fun AdminScreen(
    users: List<UserProfile>,
    withdrawals: List<WithdrawalRequest>,
    earningLogs: List<EarningLog>,
    fraudEvents: List<FraudEvent>,
    isBusy: Boolean,
    onApproveWithdrawal: (WithdrawalRequest) -> Unit,
    onRejectWithdrawal: (WithdrawalRequest, String) -> Unit,
    onSetUserStatus: (String, String) -> Unit,
    onResolveFraudEvent: (String) -> Unit
) {
    var activeSection by remember { mutableStateOf(AdminSection.PAYOUTS) }

    val grossRevenueInr = remember(earningLogs) { earningLogs.sumOf { it.grossValueInr } }
    val user70ShareInr = remember(earningLogs) { earningLogs.sumOf { it.userShareInr } }
    val admin30ShareInr = remember(earningLogs) { earningLogs.sumOf { it.adminShareInr } }
    val pendingPayoutCount = remember(withdrawals) {
        withdrawals.count { it.status == "PENDING" || it.status == "PROCESSING_IMPS" || it.status == "FLAGGED" }
    }
    val openFraudCount = remember(fraudEvents) { fraudEvents.count { !it.resolved } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Admin Economics Summary Card (70% Audience / 30% Platform)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, GoldAmber.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.AdminPanelSettings,
                            contentDescription = null,
                            tint = GoldBright,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Admin Revenue & Trust Console",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AdminMetricBox(
                            title = "Gross Revenue",
                            value = "₹%.2f".format(grossRevenueInr),
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        AdminMetricBox(
                            title = "70% Audience",
                            value = "₹%.2f".format(user70ShareInr),
                            color = CyanAccent,
                            modifier = Modifier.weight(1f)
                        )
                        AdminMetricBox(
                            title = "30% Your Share",
                            value = "₹%.2f".format(admin30ShareInr),
                            color = EmeraldBright,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AdminMetricBox(
                            title = "Total Users",
                            value = "${users.size}",
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        AdminMetricBox(
                            title = "Pending Payouts",
                            value = "$pendingPayoutCount",
                            color = GoldBright,
                            modifier = Modifier.weight(1f)
                        )
                        AdminMetricBox(
                            title = "Open Fraud Flags",
                            value = "$openFraudCount",
                            color = if (openFraudCount > 0) DangerRed else EmeraldBright,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 2. Section Switcher
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(AdminSection.entries) { section ->
                    FilterChip(
                        selected = activeSection == section,
                        onClick = { activeSection = section },
                        label = { Text(section.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldAmber.copy(alpha = 0.22f),
                            selectedLabelColor = GoldBright
                        ),
                        modifier = Modifier.testTag("admin_tab_${section.name.lowercase()}")
                    )
                }
            }
        }

        // 3. Section Content
        when (activeSection) {
            AdminSection.PAYOUTS -> {
                if (withdrawals.isEmpty()) {
                    item { AdminEmptyCard("No withdrawal requests yet.") }
                } else {
                    items(withdrawals, key = { it.id }) { wd ->
                        val isOpen = wd.status == "PENDING" || wd.status == "PROCESSING_IMPS" || wd.status == "FLAGGED"
                        Card(
                            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${wd.userName} • ${wd.method.replace('_', ' ')}",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "${wd.destinationMasked} • Risk ${wd.fraudRiskScore}/100",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "₹%.2f".format(wd.amountInr),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = EmeraldBright,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = wd.status,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (wd.status == "COMPLETED") EmeraldBright else GoldBright
                                        )
                                    }
                                }

                                if (isOpen) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { onApproveWithdrawal(wd) },
                                            enabled = !isBusy,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = EmeraldPrimary,
                                                contentColor = Color(0xFF022C22)
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                                .testTag("admin_approve_${wd.id}")
                                        ) {
                                            Icon(Icons.Filled.Check, null, Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Settle / Issue")
                                        }
                                        OutlinedButton(
                                            onClick = { onRejectWithdrawal(wd, "Declined by Admin security review") },
                                            enabled = !isBusy,
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                                .testTag("admin_reject_${wd.id}")
                                        ) {
                                            Icon(Icons.Filled.Close, null, Modifier.size(16.dp), tint = DangerRed)
                                            Spacer(Modifier.width(4.dp))
                                            Text("Reject", color = DangerRed)
                                        }
                                    }
                                } else if (!wd.utrOrRedeemCode.isNullOrBlank()) {
                                    Text(
                                        text = "Ref/Code: ${wd.utrOrRedeemCode}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CyanAccent
                                    )
                                }
                            }
                        }
                    }
                }
            }

            AdminSection.USERS -> {
                if (users.isEmpty()) {
                    item { AdminEmptyCard("No registered users found.") }
                } else {
                    items(users, key = { it.userId }) { user ->
                        val isFrozen = user.accountStatus == "FROZEN"
                        Card(
                            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = user.displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color.White
                                    )
                                    Text(
                                        text = user.email,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Bal: %,d pts • Offers: %d/%d • Risk: %d • %s".format(
                                            user.pointsBalance,
                                            user.dailyOfferCount,
                                            EconomyConfig.DAILY_OFFER_LIMIT,
                                            user.fraudRiskScore,
                                            user.accountStatus
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isFrozen) DangerRed else EmeraldBright
                                    )
                                }
                                OutlinedButton(
                                    onClick = {
                                        onSetUserStatus(user.userId, if (isFrozen) "ACTIVE" else "FROZEN")
                                    },
                                    enabled = !isBusy,
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isFrozen) Icons.Filled.LockOpen else Icons.Filled.Lock,
                                        contentDescription = null,
                                        tint = if (isFrozen) EmeraldBright else DangerRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = if (isFrozen) "Activate" else "Freeze",
                                        color = if (isFrozen) EmeraldBright else DangerRed
                                    )
                                }
                            }
                        }
                    }
                }
            }

            AdminSection.FRAUD -> {
                if (fraudEvents.isEmpty()) {
                    item { AdminEmptyCard("Zero fraud alerts detected.") }
                } else {
                    items(fraudEvents, key = { it.id }) { event ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                            shape = RoundedCornerShape(16.dp),
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
                                    Icon(
                                        imageVector = Icons.Filled.Warning,
                                        contentDescription = null,
                                        tint = if (event.resolved) EmeraldBright else WarningOrange
                                    )
                                    Column {
                                        Text(
                                            text = "${event.eventType} (${event.severity})",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "${event.userName}: ${event.details}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (!event.resolved) {
                                    OutlinedButton(
                                        onClick = { onResolveFraudEvent(event.id) },
                                        modifier = Modifier.height(48.dp)
                                    ) {
                                        Text("Resolve")
                                    }
                                } else {
                                    Text(
                                        text = "Resolved",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldBright
                                    )
                                }
                            }
                        }
                    }
                }
            }

            AdminSection.EARNINGS -> {
                if (earningLogs.isEmpty()) {
                    item { AdminEmptyCard("No global earning logs recorded yet.") }
                } else {
                    items(earningLogs.take(40), key = { it.id }) { log ->
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${log.userName} • ${log.title}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "${log.sourceType} • Gross ₹%.2f".format(log.grossValueInr),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "User 70%: ₹%.2f".format(log.userShareInr),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CyanAccent
                                    )
                                    Text(
                                        text = "Admin 30%: ₹%.2f".format(log.adminShareInr),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldBright,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminMetricBox(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = ObsidianCardElevated,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = color,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AdminEmptyCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )
    }
}
