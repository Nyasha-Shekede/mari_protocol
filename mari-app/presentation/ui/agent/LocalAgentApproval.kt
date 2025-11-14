package com.Mari.mobile.presentation.ui.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.Mari.mobileapp.core.agent.PaymentConstraints

/**
 * OFFICIAL DIRECTIVE: Create overlay UI for local agent payments
 * APPEARS over requesting app with consistent Mari design
 * NOTE: This is a scaffold; wire it to app state when LocalAgent flows are completed.
 */
@Composable
fun LocalAgentApprovalOverlay(
    agentDisplayName: String,
    trustLevel: String,
    constraints: PaymentConstraints,
    onApproved: () -> Unit,
    onRejected: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Dim background - tapping outside rejects
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable { onRejected() }
        )

        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .width(340.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(text = agentDisplayName, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                Text(text = "Trust: $trustLevel", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(16.dp))
                ConstraintSummaryCard(constraints)
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    androidx.compose.material3.Button(onClick = onApproved) { Text("Approve") }
                    androidx.compose.material3.OutlinedButton(onClick = onRejected) { Text("Reject") }
                }
            }
        }
    }
}

@Composable
private fun ConstraintSummaryCard(constraints: PaymentConstraints) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Payment Constraints", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            InfoRow("Max Amount", formatAmount(constraints.maxAmount))
            InfoRow("Allowed Stores", if (constraints.merchantWhitelist.isEmpty()) "(none)" else constraints.merchantWhitelist.joinToString())
            InfoRow("Category", constraints.category ?: "(any)")
            if (!constraints.description.isNullOrBlank()) InfoRow("Description", constraints.description)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.width(12.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun formatAmount(minor: Long): String {
    // Display as major units with 2 decimals (USD-like)
    val major = minor / 100.0
    return "$" + String.format("%.2f", major)
}
