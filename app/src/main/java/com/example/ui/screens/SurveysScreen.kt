package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.EconomyConfig
import com.example.data.model.RewardCatalog
import com.example.data.model.SurveyPartnerItem
import com.example.data.model.UserProfile
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldBright
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianCardElevated

@Composable
fun SurveysScreen(
    profile: UserProfile,
    isBusy: Boolean,
    nowElapsedMs: () -> Long,
    onCompleteSurvey: (SurveyPartnerItem, Long) -> Unit
) {
    val partners = remember { listOf("All", "PubScale", "GoodWalls", "CPX Research", "BitLabs") }
    var selectedPartner by remember { mutableStateOf("All") }
    var activeSurvey by remember { mutableStateOf<SurveyPartnerItem?>(null) }
    var activeSurveyStartMs by remember { mutableLongStateOf(0L) }

    val filteredSurveys = remember(selectedPartner) {
        if (selectedPartner == "All") RewardCatalog.surveyPartners
        else RewardCatalog.surveyPartners.filter { it.partnerName == selectedPartner }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Poll,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Partner Survey Wall",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "Earn verified survey rewards from PubScale, GoodWalls, CPX Research & BitLabs. 70% of partner payout goes directly to your wallet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(partners) { partner ->
                    FilterChip(
                        selected = selectedPartner == partner,
                        onClick = { selectedPartner = partner },
                        label = { Text(partner) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary.copy(alpha = 0.22f),
                            selectedLabelColor = EmeraldBright
                        ),
                        modifier = Modifier.testTag("survey_filter_${partner.lowercase().replace(" ", "_")}")
                    )
                }
            }
        }

        items(filteredSurveys, key = { it.id }) { survey ->
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, ObsidianCardElevated),
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
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                color = CyanAccent.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = survey.partnerName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyanAccent,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Surface(
                                color = EmeraldPrimary.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = survey.qualificationRate,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldBright,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Text(
                            text = "+%,d pts".format(survey.pointsReward),
                            style = MaterialTheme.typography.labelLarge,
                            color = GoldBright,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = survey.topic,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Timer,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "${survey.estimatedMinutes} min • ₹%.2f value".format(
                                    EconomyConfig.pointsToUserInr(survey.pointsReward)
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = {
                                activeSurveyStartMs = nowElapsedMs()
                                activeSurvey = survey
                            },
                            enabled = !isBusy && profile.accountStatus != "FROZEN",
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                contentColor = Color(0xFF022C22)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("start_survey_${survey.id}")
                        ) {
                            Text("Start Survey", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    activeSurvey?.let { survey ->
        SurveyRunnerDialog(
            survey = survey,
            onDismiss = { activeSurvey = null },
            onComplete = {
                val started = activeSurveyStartMs
                activeSurvey = null
                onCompleteSurvey(survey, started)
            }
        )
    }
}

@Composable
private fun SurveyRunnerDialog(
    survey: SurveyPartnerItem,
    onDismiss: () -> Unit,
    onComplete: () -> Unit
) {
    var questionIndex by remember(survey.id) { mutableIntStateOf(0) }
    var selectedOption by remember(survey.id, questionIndex) { mutableStateOf<String?>(null) }
    val currentQuestion = survey.questions[questionIndex]
    val isLast = questionIndex == survey.questions.lastIndex

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianCard,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "${survey.partnerName} • Question ${questionIndex + 1} of ${survey.questions.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyanAccent
                )
                Text(
                    text = survey.topic,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = currentQuestion.prompt,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                currentQuestion.options.forEach { option ->
                    Surface(
                        color = if (selectedOption == option) {
                            EmeraldPrimary.copy(alpha = 0.16f)
                        } else {
                            ObsidianCardElevated
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.dp,
                            if (selectedOption == option) EmeraldPrimary else Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedOption = option }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedOption == option,
                                onClick = { selectedOption = option }
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = option,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!isLast) {
                        questionIndex += 1
                    } else {
                        onComplete()
                    }
                },
                enabled = selectedOption != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    contentColor = Color(0xFF022C22)
                ),
                modifier = Modifier.testTag("survey_next_submit_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(if (isLast) "Submit (+${survey.pointsReward} pts)" else "Next")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Exit")
            }
        }
    )
}
