package com.teamposhan.biomed.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.teamposhan.biomed.R
import com.teamposhan.biomed.data.model.UserProfile
import com.teamposhan.biomed.data.model.UserRole
import com.teamposhan.biomed.ui.components.Gap
import com.teamposhan.biomed.ui.components.InfoBanner
import com.teamposhan.biomed.ui.components.PoshanCard
import com.teamposhan.biomed.ui.components.PoshanDropdownField
import com.teamposhan.biomed.ui.components.PoshanPrimaryButton
import com.teamposhan.biomed.ui.components.PoshanSurface
import com.teamposhan.biomed.ui.components.PoshanTextField
import com.teamposhan.biomed.ui.components.PoshanTonalButton
import com.teamposhan.biomed.ui.components.SectionLabel
import com.teamposhan.biomed.ui.haptics.rememberAppHaptics
import com.teamposhan.biomed.ui.theme.accentColors
import com.teamposhan.biomed.ui.theme.riskColors
import kotlinx.coroutines.launch
import java.util.UUID

// ---------------------------------------------------------------------------
// Language
// ---------------------------------------------------------------------------

@Composable
fun LanguageScreen(current: String, onSelect: (String) -> Unit) {
    val haptics = rememberAppHaptics()
    PoshanSurface {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(28.dp))
            Image(
                painter = painterResource(R.drawable.illus_splash),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .aspectRatio(0.72f)
                    .clip(RoundedCornerShape(28.dp)),
            )
            Spacer(Modifier.height(20.dp))
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Gap(6)
            Text(
                stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))
            SectionLabel(stringResource(R.string.select_language), Modifier.fillMaxWidth())
            Gap(8)
            LanguageOption("English", "English", "A", current.startsWith("en")) { haptics.click(); onSelect("en") }
            Gap(10)
            LanguageOption("हिन्दी", "Hindi", "अ", current.startsWith("hi")) { haptics.click(); onSelect("hi") }
            Gap(10)
            LanguageOption("తెలుగు", "Telugu", "అ", current.startsWith("te")) { haptics.click(); onSelect("te") }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun LanguageOption(native: String, english: String, glyph: String, selected: Boolean, onClick: () -> Unit) {
    val c = riskColors
    PoshanCard(
        onClick = onClick,
        container = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(glyph, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(native, style = MaterialTheme.typography.titleLarge)
                Text(english, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (selected) Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = c.low)
        }
    }
}

// ---------------------------------------------------------------------------
// Guide
// ---------------------------------------------------------------------------

@Composable
fun GuideScreen(onContinue: () -> Unit) {
    PoshanSurface {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.app_guide), style = MaterialTheme.typography.headlineSmall)
            Gap(16)

            GuideArtCard(
                R.drawable.illus_onboard_language,
                stringResource(R.string.guide_categories_title),
                stringResource(R.string.guide_categories_body),
            )
            GuideArtCard(
                R.drawable.illus_onboard_offline,
                stringResource(R.string.guide_offline_title),
                stringResource(R.string.guide_offline_body),
            )
            GuideArtCard(
                R.drawable.illus_onboard_screening,
                stringResource(R.string.guide_screening_title),
                stringResource(R.string.guide_screening_body),
            )
            Spacer(Modifier.height(8.dp))
        }
        Box(Modifier.padding(20.dp)) {
            PoshanPrimaryButton(stringResource(R.string.continue_label), onClick = onContinue)
        }
    }
}

@Composable
private fun GuideArtCard(art: Int, title: String, body: String) {
    PoshanCard(modifier = Modifier.padding(bottom = 12.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
        Column {
            Image(
                painter = painterResource(art),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().aspectRatio(1.6f).clip(
                    RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                ),
            )
            Column(Modifier.padding(18.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Login
// ---------------------------------------------------------------------------

@Composable
fun LoginScreen(onPhoneLogin: (String) -> Unit, onGuest: () -> Unit) {
    var phone by remember { mutableStateOf("") }
    PoshanSurface {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.phone_login), style = MaterialTheme.typography.headlineSmall)
            Gap(8)
            Text(
                stringResource(R.string.login_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(28.dp))
            PoshanTextField(
                value = phone,
                onValueChange = { phone = it },
                label = stringResource(R.string.mobile_number),
                prefix = "+91 ",
                keyboardType = KeyboardType.Phone,
                filter = { input -> input.filter { it.isDigit() }.take(10) },
            )
            Spacer(Modifier.height(20.dp))
            PoshanPrimaryButton(
                stringResource(R.string.continue_label),
                enabled = phone.length == 10,
            ) { onPhoneLogin(phone) }
            Gap(10)
            PoshanTonalButton(stringResource(R.string.guest_login), onClick = onGuest)
            Spacer(Modifier.height(16.dp))
            InfoBanner(stringResource(R.string.guest_mode_note), icon = Icons.Filled.Shield)
        }
    }
}

// ---------------------------------------------------------------------------
// Consent
// ---------------------------------------------------------------------------

@Composable
fun ConsentScreen(onAgree: () -> Unit) {
    var checked by remember { mutableStateOf(false) }
    PoshanSurface {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp)) {
            Text(stringResource(R.string.consent_privacy), style = MaterialTheme.typography.headlineSmall)
            Gap(16)
            PoshanCard {
                Text(
                    stringResource(R.string.consent_body),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(Modifier.height(12.dp))
                BulletLine(stringResource(R.string.consent_bullet_device))
                BulletLine(stringResource(R.string.consent_bullet_no_upload))
                BulletLine(stringResource(R.string.consent_bullet_offline))
                BulletLine(stringResource(R.string.consent_bullet_permissions))
            }
            Gap(16)
            PoshanCard(onClick = { checked = !checked }, container = MaterialTheme.colorScheme.surfaceContainerLow) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = checked, onCheckedChange = { checked = it })
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.i_agree), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        Box(Modifier.padding(20.dp)) {
            PoshanPrimaryButton(stringResource(R.string.continue_label), enabled = checked, onClick = onAgree)
        }
    }
}

@Composable
private fun BulletLine(text: String) {
    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
        Text("•", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ---------------------------------------------------------------------------
// Permission primer — explains every permission visually and where it is used
// ---------------------------------------------------------------------------

private data class PermissionItem(
    val icon: ImageVector,
    val title: String,
    val why: String,
    val usage: String,
    val permission: String,
    val optional: Boolean = false,
)

@Composable
fun PermissionsScreen(onContinue: () -> Unit) {
    val context = LocalContext.current
    val haptics = rememberAppHaptics()

    val items = listOf(
        PermissionItem(
            Icons.Filled.CameraAlt,
            stringResource(R.string.permissions_camera),
            stringResource(R.string.permissions_camera_why),
            stringResource(R.string.permissions_camera_use),
            Manifest.permission.CAMERA,
        ),
        PermissionItem(
            Icons.Filled.Notifications,
            stringResource(R.string.permissions_notifications),
            stringResource(R.string.permissions_notifications_why),
            stringResource(R.string.permissions_notifications_use),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.POST_NOTIFICATIONS else Manifest.permission.INTERNET,
        ),
        PermissionItem(
            Icons.Filled.LocationOn,
            stringResource(R.string.permissions_location),
            stringResource(R.string.permissions_location_why),
            stringResource(R.string.permissions_location_use),
            Manifest.permission.ACCESS_COARSE_LOCATION,
            optional = true,
        ),
    )

    var granted by remember { mutableStateOf(items.associate { it.permission to isGranted(context, it.permission) }) }

    // Ask the system for the permissions, rather than sending the user to the app settings
    // page and hoping they find the right toggle. Dropping people into Settings is what the
    // previous version did, and it is why almost nobody arrived at the camera module with
    // camera access already granted.
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        granted = granted + result.mapValues { (permission, ok) ->
            // A denied request may still be granted from a previous session, so re-read
            // rather than trusting the dialog result alone.
            ok || isGranted(context, permission)
        }
    }

    /** Requests everything not yet granted, in one dialog sequence. */
    fun requestAll() {
        val missing = items
            .map { it.permission }
            .filter { !isGranted(context, it) }
            .toTypedArray()
        if (missing.isEmpty()) {
            granted = items.associate { it.permission to true }
        } else {
            launcher.launch(missing)
        }
    }

    // Ask as soon as the screen appears: this screen exists to collect permissions, and
    // making the caregiver tap each card first is friction with no benefit.
    LaunchedEffect(Unit) { requestAll() }

    val allRequired = items.filterNot { it.optional }.all { granted[it.permission] == true }

    PoshanSurface {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp)) {
            Text(stringResource(R.string.permissions_title), style = MaterialTheme.typography.headlineSmall)
            Gap(6)
            Text(
                stringResource(R.string.permissions_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(18.dp))

            items.forEach { item ->
                val ok = granted[item.permission] == true
                PermissionCard(
                    item = item,
                    granted = ok,
                    onRequest = {
                        haptics.click()
                        if (isGranted(context, item.permission)) {
                            granted = granted + (item.permission to true)
                        } else {
                            // A second refusal is permanent on Android, and the system
                            // dialog then never appears again — so fall back to Settings
                            // only once the system has stopped offering us the dialog.
                            launcher.launch(arrayOf(item.permission))
                        }
                    },
                )
                Gap(12)
            }

            InfoBanner(
                stringResource(R.string.permissions_settings_hint),
                icon = Icons.Filled.Shield,
            )
        }
        Box(Modifier.padding(20.dp)) {
            Column {
                if (!allRequired) {
                    // Required permissions still missing: offer the dialog again rather
                    // than letting the caregiver walk into a module that cannot work.
                    PoshanPrimaryButton(
                        stringResource(R.string.permissions_allow_all),
                        onClick = { haptics.click(); requestAll() },
                    )
                    Gap(10)
                    com.teamposhan.biomed.ui.components.PoshanTonalButton(
                        stringResource(R.string.continue_label),
                        onClick = onContinue,
                    )
                } else {
                    PoshanPrimaryButton(stringResource(R.string.continue_label), onClick = onContinue)
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(item: PermissionItem, granted: Boolean, onRequest: () -> Unit) {
    val c = riskColors
    val tint by animateColorAsState(if (granted) c.low else MaterialTheme.colorScheme.primary, label = "perm")
    PoshanCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(46.dp).background(tint.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(item.icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium)
                    if (item.optional) {
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.permissions_optional), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text(item.why, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (granted) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = c.low)
            } else {
                com.teamposhan.biomed.ui.components.SmallIconButton(
                    icon = Icons.Filled.Shield,
                    onClick = onRequest,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(item.usage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(Modifier.height(12.dp))
        PermissionDialogMock(item.title)
    }
}

/** Visual of the Android system dialog, so the user knows exactly what to tap. */
@Composable
private fun PermissionDialogMock(permissionName: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Column {
            Text(
                "Allow Poshan to use $permissionName?",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.permissions_dialog_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MockButton(stringResource(R.string.permissions_dont_allow), false)
                MockButton(stringResource(R.string.permissions_allow), true)
            }
        }
    }
}

@Composable
private fun MockButton(text: String, primary: Boolean) {
    Box(
        Modifier
            .background(
                if (primary) MaterialTheme.colorScheme.primary else Color.Transparent,
                RoundedCornerShape(20.dp),
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun isGranted(context: android.content.Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

// ---------------------------------------------------------------------------
// User type
// ---------------------------------------------------------------------------

@Composable
fun UserTypeScreen(onPick: (UserRole) -> Unit) {
    PoshanSurface {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
            Text(stringResource(R.string.user_type), style = MaterialTheme.typography.headlineSmall)
            Gap(18)
            RoleCard(Icons.Filled.Person, stringResource(R.string.user_type_parent), stringResource(R.string.user_type_parent_desc)) { onPick(UserRole.PARENT) }
            Gap(12)
            RoleCard(Icons.Filled.Home, stringResource(R.string.user_type_anganwadi), stringResource(R.string.user_type_anganwadi_desc)) { onPick(UserRole.ANGANWADI_WORKER) }
            Gap(12)
            RoleCard(Icons.Filled.LocalHospital, stringResource(R.string.user_type_healthcare), stringResource(R.string.user_type_healthcare_desc)) { onPick(UserRole.HEALTHCARE_FACILITY) }
        }
    }
}

@Composable
private fun RoleCard(icon: ImageVector, title: String, desc: String, onClick: () -> Unit) {
    val accents = accentColors
    PoshanCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(52.dp).background(accents.mint, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Profile setup
// ---------------------------------------------------------------------------

@Composable
fun ProfileSetupScreen(role: UserRole, onSave: (UserProfile) -> Unit) {
    val context = LocalContext.current
    val haptics = rememberAppHaptics()
    var name by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    // Declared alongside `location` rather than inside the GPS block: the profile save is
    // outside that block and needs these in scope.
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var centreName by remember { mutableStateOf("") }
    var centreCode by remember { mutableStateOf("") }
    var facilityName by remember { mutableStateOf("") }
    var hfrId by remember { mutableStateOf("") }
    var employeeId by remember { mutableStateOf("") }

    val title = when (role) {
        UserRole.PARENT -> stringResource(R.string.parent_profile)
        UserRole.ANGANWADI_WORKER -> stringResource(R.string.worker_profile)
        else -> stringResource(R.string.facility_profile)
    }

    PoshanSurface {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Gap(18)
            when (role) {
                UserRole.PARENT -> {
                    PoshanTextField(name, { name = it }, stringResource(R.string.parent_name))
                    Gap(14)
                    PoshanDropdownField(
                        value = relationship,
                        onSelect = { relationship = it },
                        label = stringResource(R.string.relationship_with_child),
                        options = listOf(
                            stringResource(R.string.relationship_mother),
                            stringResource(R.string.relationship_father),
                            stringResource(R.string.relationship_grandmother),
                            stringResource(R.string.relationship_grandfather),
                            stringResource(R.string.relationship_sibling),
                            stringResource(R.string.relationship_aunt),
                            stringResource(R.string.relationship_uncle),
                            stringResource(R.string.relationship_guardian),
                        ),
                        allowOther = true,
                    )
                    Gap(14)
                    PoshanTextField(phone, { phone = it }, stringResource(R.string.mobile_number), keyboardType = KeyboardType.Phone)
                }
                UserRole.ANGANWADI_WORKER -> {
                    PoshanTextField(name, { name = it }, stringResource(R.string.worker_name))
                    Gap(14)
                    PoshanTextField(centreName, { centreName = it }, stringResource(R.string.anganwadi_centre_name))
                    Gap(14)
                    PoshanTextField(
                        centreCode, { centreCode = it }, stringResource(R.string.anganwadi_centre_code),
                        keyboardType = KeyboardType.Number,
                        supportingText = "${centreCode.length}/11 digits",
                        filter = { input -> input.filter { c -> c.isDigit() }.take(11) },
                    )
                    Gap(14)
                    PoshanTextField(phone, { phone = it }, stringResource(R.string.mobile_number), keyboardType = KeyboardType.Phone)
                }
                else -> {
                    PoshanTextField(facilityName, { facilityName = it }, stringResource(R.string.facility_name))
                    Gap(14)
                    PoshanTextField(hfrId, { hfrId = it }, stringResource(R.string.hfr_id))
                    Gap(14)
                    PoshanTextField(employeeId, { employeeId = it }, stringResource(R.string.employee_id_and_name))
                    Gap(14)
                    PoshanTextField(phone, { phone = it }, stringResource(R.string.mobile_number), keyboardType = KeyboardType.Phone)
                }
            }
            Gap(14)
            PoshanTextField(location, { location = it }, stringResource(R.string.location))
            Gap(10)
            val scope = androidx.compose.runtime.rememberCoroutineScope()
            var locating by remember { mutableStateOf(false) }
            // A silent failure here used to look like a broken button; now the reason shows.
            var locationError by remember { mutableStateOf<String?>(null) }

            suspend fun capture() {
                locating = true
                locationError = null
                when (val outcome = com.teamposhan.biomed.domain.location.LocationHelper.locate(context)) {
                    is com.teamposhan.biomed.domain.location.LocationHelper.Outcome.Found -> {
                        location = outcome.result.label
                        // Keep the coordinates too: the vitamin D sun window needs a
                        // latitude, and a place name cannot supply one offline.
                        latitude = outcome.result.latitude
                        longitude = outcome.result.longitude
                    }
                    is com.teamposhan.biomed.domain.location.LocationHelper.Outcome.Failed -> {
                        locationError = when (outcome.reason) {
                            com.teamposhan.biomed.domain.location.LocationHelper.Failure.NO_PERMISSION ->
                                "Location permission is needed to fill this in."
                            com.teamposhan.biomed.domain.location.LocationHelper.Failure.LOCATION_OFF ->
                                "Turn on Location in your phone settings, then tap again."
                            com.teamposhan.biomed.domain.location.LocationHelper.Failure.NO_FIX ->
                                "No GPS fix yet. Step outside or near a window and tap again."
                        }
                    }
                }
                locating = false
            }

            val locationPermission = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { granted ->
                if (granted.values.any { it }) {
                    scope.launch { capture() }
                } else {
                    locationError = "Location permission is needed to fill this in."
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    // Clip before clickable so the ripple is rounded, not a hard rectangle.
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(enabled = !locating) {
                    haptics.click()
                    if (com.teamposhan.biomed.domain.location.LocationHelper.hasPermission(context)) {
                        scope.launch { capture() }
                    } else {
                        locationPermission.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                            )
                        )
                    }
                },
            ) {
                Icon(
                    Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (locating) "Getting location…" else stringResource(R.string.use_gps),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            locationError?.let {
                Spacer(Modifier.height(6.dp))
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(Modifier.height(16.dp))
        }
        Box(Modifier.padding(20.dp)) {
            PoshanPrimaryButton(stringResource(R.string.create_profile), enabled = name.isNotBlank()) {
                onSave(
                    UserProfile(
                        id = UUID.randomUUID().toString(),
                        role = role,
                        displayName = name,
                        phone = phone,
                        relationship = relationship,
                        location = location,
                        latitude = latitude,
                        longitude = longitude,
                        anganwadiName = centreName,
                        anganwadiCode = centreCode,
                        facilityName = facilityName,
                        hfrId = hfrId,
                        employeeId = employeeId,
                        isGuest = true,
                    ),
                )
            }
        }
    }
}

@Composable
internal fun Field(
    label: String,
    value: String,
    keyboard: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
        PoshanTextField(value, onChange, label, keyboardType = keyboard)
    }
}
