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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.EconomyConfig
import com.example.data.model.PayoutMethod
import com.example.data.model.UserProfile
import com.example.data.model.WithdrawalRequest
import com.example.data.security.FraudAssessment
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DangerRed
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAmber
import com.example.ui.theme.GoldBright
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianCardElevated
import com.example.ui.theme.WarningOrange

@Composable
fun WalletScreen(
    profile: UserProfile,
    withdrawals: List<WithdrawalRequest>,
    fraudAssessment: FraudAssessment,
    isBusy: Boolean,
    onRefreshSecurity: () -> Unit,
    onSubmitWithdrawal: (
        method: PayoutMethod,
        points: Int,
        upiId: String?,
        paytmNumber: String?,
        bankAccountNumber: String?,
        ifscCode: String?,
        accountHolderName: String?,
        redeemEmail: String?
    ) -> Unit,
    onCopiedNotice: (String) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var selectedMethod by remember { mutableStateOf(PayoutMethod.UPI) }
    var selectedPoints by remember { mutableIntStateOf(1000) }
    var upiId by remember { mutableStateOf("") }
    var paytmNumber by remember { mutableStateOf("") }
    var bankAccount by remember { mutableStateOf("") }
    var ifscCode by remember { mutableStateOf("") }
    var accountHolder by remember { mutableStateOf(profile.displayName) }
    var redeemEmail by remember { mutableStateOf(profile.email) }

    val amountPresets = remember { listOf(1000, 2500, 5000, 10000) }
    val inrBalance = EconomyConfig.pointsToUserInr(profile.pointsBalance.toInt())

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Wallet Balance & Anti-Fraud Shield
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Redeemable Wallet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "₹%.2f".format(inrBalance),
                                style = MaterialTheme.typography.displaySmall,
                                color = EmeraldBright
                            )
                            Text(
                                text = "%,d points • 100 pts = ₹1.00".format(profile.pointsBalance),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Withdrawn",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "₹%.2f".format(EconomyConfig.pointsToUserInr(profile.lifetimePointsWithdrawn.toInt())),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        }
                    }

                    // Fraud Shield Card
                    Surface(
                        color = ObsidianCardElevated,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Shield,
                                    contentDescription = "Device Integrity Shield",
                                    tint = if (fraudAssessment.riskScore < 35) EmeraldBright else WarningOrange,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column {
                                    Text(
                                        text = if (fraudAssessment.riskScore < 35) "Account Protected" else "Security Review Active",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color.White
                                    )
                                    Text(
                                        text = if (fraudAssessment.riskScore < 35) {
                                            "Verified device • Instant payouts enabled"
                                        } else {
                                            fraudAssessment.signals.first()
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = onRefreshSecurity) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = "Re-scan device integrity",
                                    tint = CyanAccent
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Fast Payout Rails Selector (UPI, Paytm, IMPS, Bank Transfer, Google Play Redeem)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.AccountBalance,
                                contentDescription = null,
                                tint = GoldAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Fast Withdrawal Rail",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                        }
                        Surface(
                            color = EmeraldPrimary.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = selectedMethod.slaLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldBright,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(PayoutMethod.entries) { method ->
                            FilterChip(
                                selected = selectedMethod == method,
                                onClick = { selectedMethod = method },
                                label = { Text(method.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary.copy(alpha = 0.22f),
                                    selectedLabelColor = EmeraldBright
                                ),
                                modifier = Modifier.testTag("payout_rail_${method.id.lowercase()}")
                            )
                        }
                    }

                    Text(
                        text = "Select Amount",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(amountPresets) { pts ->
                            val inr = EconomyConfig.pointsToUserInr(pts)
                            FilterChip(
                                selected = selectedPoints == pts,
                                onClick = { selectedPoints = pts },
                                label = { Text("₹%.0f (%d pts)".format(inr, pts)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GoldAmber.copy(alpha = 0.24f),
                                    selectedLabelColor = GoldBright
                                ),
                                modifier = Modifier.testTag("payout_amount_$pts")
                            )
                        }
                    }

                    // Dynamic destination input fields per selected payout rail
                    when (selectedMethod) {
                        PayoutMethod.UPI -> {
                            OutlinedTextField(
                                value = upiId,
                                onValueChange = { upiId = it },
                                label = { Text("UPI ID (VPA)") },
                                placeholder = { Text("e.g. rahul@ybl or 9876543210@upi") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("upi_input")
                            )
                        }
                        PayoutMethod.PAYTM -> {
                            OutlinedTextField(
                                value = paytmNumber,
                                onValueChange = { paytmNumber = it },
                                label = { Text("Paytm Mobile Number or @paytm UPI") },
                                placeholder = { Text("10-digit mobile or name@paytm") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("paytm_input")
                            )
                        }
                        PayoutMethod.IMPS, PayoutMethod.BANK_TRANSFER -> {
                            OutlinedTextField(
                                value = accountHolder,
                                onValueChange = { accountHolder = it },
                                label = { Text("Account Holder Name") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("bank_holder_input")
                            )
                            OutlinedTextField(
                                value = bankAccount,
                                onValueChange = { bankAccount = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Bank Account Number") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("bank_account_input")
                            )
                            OutlinedTextField(
                                value = ifscCode,
                                onValueChange = { ifscCode = it.uppercase() },
                                label = { Text("IFSC Code") },
                                placeholder = { Text("e.g. HDFC0001234") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("bank_ifsc_input")
                            )
                        }
                        PayoutMethod.GOOGLE_PLAY_REDEEM -> {
                            OutlinedTextField(
                                value = redeemEmail,
                                onValueChange = { redeemEmail = it },
                                label = { Text("Delivery Email for Google Play Code") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("gplay_email_input")
                            )
                        }
                    }

                    Button(
                        onClick = {
                            onSubmitWithdrawal(
                                selectedMethod,
                                selectedPoints,
                                upiId.takeIf { selectedMethod == PayoutMethod.UPI },
                                paytmNumber.takeIf { selectedMethod == PayoutMethod.PAYTM },
                                bankAccount.takeIf { selectedMethod == PayoutMethod.IMPS || selectedMethod == PayoutMethod.BANK_TRANSFER },
                                ifscCode.takeIf { selectedMethod == PayoutMethod.IMPS || selectedMethod == PayoutMethod.BANK_TRANSFER },
                                accountHolder.takeIf { selectedMethod == PayoutMethod.IMPS || selectedMethod == PayoutMethod.BANK_TRANSFER },
                                redeemEmail.takeIf { selectedMethod == PayoutMethod.GOOGLE_PLAY_REDEEM }
                            )
                        },
                        enabled = !isBusy &&
                            profile.pointsBalance >= selectedPoints &&
                            profile.accountStatus != "FROZEN",
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = Color(0xFF022C22)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("submit_withdrawal_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Bolt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        val label = if (profile.pointsBalance < selectedPoints) {
                            "Need ${selectedPoints - profile.pointsBalance} more pts"
                        } else {
                            "Withdraw ₹%.0f via ${selectedMethod.label}".format(
                                EconomyConfig.pointsToUserInr(selectedPoints)
                            )
                        }
                        Text(label, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 3. Payout History & Instant UTR / Google Play Redeem Code Viewer
        item {
            Text(
                text = "Withdrawal & Redeem History",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
        }

        if (withdrawals.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No payouts yet. Minimum withdrawal is 1,000 pts (₹10) via UPI, Paytm, IMPS, Bank, or Google Play Redeem.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(withdrawals, key = { it.id }) { wd ->
                val statusColor = when (wd.status) {
                    "COMPLETED" -> EmeraldBright
                    "PROCESSING_IMPS" -> CyanAccent
                    "FLAGGED", "REJECTED" -> DangerRed
                    else -> GoldBright
                }
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${wd.method.replace('_', ' ')} • ₹%.2f".format(wd.amountInr),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = wd.destinationMasked,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                color = statusColor.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = wd.status.replace('_', ' '),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = statusColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        if (!wd.utrOrRedeemCode.isNullOrBlank()) {
                            Surface(
                                color = ObsidianCardElevated,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = if (wd.method == PayoutMethod.GOOGLE_PLAY_REDEEM.id) "Google Play Code" else "UTR / Reference",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = EmeraldBright
                                        )
                                        Text(
                                            text = wd.utrOrRedeemCode,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.White
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            clipboard.setText(AnnotatedString(wd.utrOrRedeemCode))
                                            onCopiedNotice("Copied ${wd.utrOrRedeemCode}")
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.ContentCopy,
                                            contentDescription = "Copy reference code",
                                            tint = CyanAccent
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
}
