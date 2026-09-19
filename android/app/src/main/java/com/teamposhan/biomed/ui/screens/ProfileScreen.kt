package com.teamposhan.biomed.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.teamposhan.biomed.data.model.UserProfile
import com.teamposhan.biomed.data.model.UserRole
import com.teamposhan.biomed.ui.components.Gap
import com.teamposhan.biomed.ui.components.InfoBanner
import com.teamposhan.biomed.ui.components.PoshanCard
import com.teamposhan.biomed.ui.components.PoshanPrimaryButton
import com.teamposhan.biomed.ui.components.PoshanTextField
import com.teamposhan.biomed.ui.components.SectionLabel

/**
 * The signed-in person's own details, and who they want reports sent to.
 *
 * Everything on this page was already collected at sign-in and then became invisible — there
 * was no screen that showed it back, so a typo in a phone number entered once during
 * onboarding could never be found, let alone corrected.
 */
@Composable
fun ProfileScreen(
    profile: UserProfile?,
    onSave: (UserProfile) -> Unit,
    onDone: () -> Unit,
) {
    if (profile == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Not signed in.", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    var displayName by remember { mutableStateOf(profile.displayName) }
    var phone by remember { mutableStateOf(profile.phone) }
    var location by remember { mutableStateOf(profile.location) }
    var doctorName by remember { mutableStateOf(profile.doctorName) }
    var doctorPhone by remember { mutableStateOf(profile.doctorPhone) }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(30.dp),
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        displayName.ifBlank { "Your details" },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        roleLabel(profile.role),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Gap(22)

            SectionLabel("Your details")
            Gap(8)
            PoshanTextField(displayName, { displayName = it }, "Your name")
            Gap(12)
            PoshanTextField(
                phone, { phone = it }, "Your mobile number",
                keyboardType = KeyboardType.Phone,
            )
            Gap(12)
            PoshanTextField(location, { location = it }, "Village or area")

            // Read-only facts from sign-up. Shown because they were previously invisible,
            // not editable because changing them would silently re-key synced records.
            if (profile.anganwadiName.isNotBlank() || profile.anganwadiCode.isNotBlank() ||
                profile.facilityName.isNotBlank() || profile.employeeId.isNotBlank()
            ) {
                Gap(18)
                SectionLabel("From sign-in")
                Gap(8)
                PoshanCard(contentPadding = PaddingValues(16.dp)) {
                    ReadOnlyRow("Anganwadi centre", profile.anganwadiName)
                    ReadOnlyRow("Centre code", profile.anganwadiCode)
                    ReadOnlyRow("Facility", profile.facilityName)
                    ReadOnlyRow("Facility ID", profile.hfrId)
                    ReadOnlyRow("Staff ID", profile.employeeId)
                    ReadOnlyRow(
                        "Location fix",
                        profile.latitude?.let { lat ->
                            profile.longitude?.let { lon -> "%.3f, %.3f".format(lat, lon) }
                        } ?: "not captured",
                    )
                }
            }

            Gap(22)
            SectionLabel("Send reports to")
            Gap(8)
            PoshanTextField(doctorName, { doctorName = it }, "Doctor or health worker name")
            Gap(12)
            PoshanTextField(
                doctorPhone, { doctorPhone = it }, "Their mobile number",
                keyboardType = KeyboardType.Phone,
                supportingText = "Used only when you choose to send a report.",
            )
            Gap(12)
            InfoBanner(
                "Saved on this phone. The app never messages this number on its own — a " +
                    "report is only ever sent when you press send.",
            )

            Gap(24)
        }
        Box(Modifier.padding(20.dp)) {
            PoshanPrimaryButton("Save") {
                onSave(
                    profile.copy(
                        displayName = displayName.trim(),
                        phone = phone.trim(),
                        location = location.trim(),
                        doctorName = doctorName.trim(),
                        doctorPhone = doctorPhone.trim(),
                    ),
                )
                onDone()
            }
        }
    }
}

@Composable
private fun ReadOnlyRow(label: String, value: String) {
    if (value.isBlank()) return
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(140.dp),
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun roleLabel(role: UserRole): String = when (role) {
    UserRole.PARENT -> "Parent or caregiver"
    UserRole.ANGANWADI_WORKER -> "Anganwadi worker"
    UserRole.HEALTHCARE_FACILITY -> "Health facility"
    UserRole.ADMIN -> "Administrator"
}
