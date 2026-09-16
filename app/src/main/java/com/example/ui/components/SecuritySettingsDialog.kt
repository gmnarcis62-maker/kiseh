package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.security.BiometricAuthManager
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val isAppLockEnabled by viewModel.isAppLockEnabled.collectAsState()
    val isLockOnLaunchEnabled by viewModel.isLockOnLaunchEnabled.collectAsState()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    val pinLength by viewModel.pinLength.collectAsState()
    val hasPinSet by viewModel.hasPinSet.collectAsState()

    val biometricStatus = remember { viewModel.checkBiometricAvailability() }
    val isBiometricSupported = biometricStatus == BiometricAuthManager.BiometricStatus.AVAILABLE

    // Sub-dialog states
    var showSetupPinDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showDisableConfirmDialog by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, GoldAccent.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                    .testTag("security_settings_dialog"),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "امنیت و قفل برنامه",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "محافظت از اطلاعات مالی و تراکنش‌ها",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_security_dialog_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "بستن",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Status Banner
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isAppLockEnabled) EmeraldDark.copy(alpha = 0.08f) else Color(0xFFFFF4E5)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isAppLockEnabled) EmeraldPrimary.copy(alpha = 0.3f) else Color(0xFFFFB74D)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isAppLockEnabled) EmeraldPrimary else Color(0xFFF57C00)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isAppLockEnabled) Icons.Default.Shield else Icons.Default.LockOpen,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isAppLockEnabled) "قفل برنامه فعال است" else "قفل برنامه غیرفعال است",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAppLockEnabled) EmeraldPrimary else Color(0xFFD84315)
                                )
                                Text(
                                    text = if (isAppLockEnabled)
                                        "اطلاعات حساب‌ها و تراکنش‌های شما رمزگذاری و محافظت می‌شود."
                                    else
                                        "برای محافظت از حریم خصوصی، قفل با رمز عبور را فعال کنید.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Option 1: Master Lock Switch
                    SecurityOptionRow(
                        title = "فعال‌سازی قفل برنامه",
                        subtitle = if (isAppLockEnabled) "رمز عبور $pinLength رقمی تنظیم شده است" else "تعیین رمز عبور و محافظت از ورود به برنامه",
                        icon = Icons.Default.Lock,
                        trailingContent = {
                            Switch(
                                checked = isAppLockEnabled,
                                onCheckedChange = { enable ->
                                    if (enable) {
                                        showSetupPinDialog = true
                                    } else {
                                        showDisableConfirmDialog = true
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = EmeraldPrimary
                                ),
                                modifier = Modifier.testTag("app_lock_switch")
                            )
                        }
                    )

                    AnimatedVisibility(visible = isAppLockEnabled) {
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))

                            // Option 2: Lock on Launch
                            SecurityOptionRow(
                                title = "قفل هنگام باز شدن برنامه",
                                subtitle = "در هر بار اجرای مجدد، صفحه ورود قفل نمایش داده شود",
                                icon = Icons.Default.PowerSettingsNew,
                                trailingContent = {
                                    Switch(
                                        checked = isLockOnLaunchEnabled,
                                        onCheckedChange = { viewModel.setLockOnLaunchEnabled(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = EmeraldPrimary
                                        ),
                                        modifier = Modifier.testTag("lock_on_launch_switch")
                                    )
                                }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Option 3: Biometric Fingerprint
                            val biometricSubtitle = when (biometricStatus) {
                                BiometricAuthManager.BiometricStatus.AVAILABLE ->
                                    "ورود سریع و امن با اثر انگشت یا حسگر چهره"
                                BiometricAuthManager.BiometricStatus.NONE_ENROLLED ->
                                    "اثر انگشتی در تنظیمات دستگاه ثبت نشده است"
                                BiometricAuthManager.BiometricStatus.NO_HARDWARE ->
                                    "دستگاه شما فاقد حسگر اثر انگشت است"
                                BiometricAuthManager.BiometricStatus.HW_UNAVAILABLE ->
                                    "حسگر بیومتریک موقتاً در دسترس نیست"
                                else -> "احراز هویت بیومتریک پشتیبانی نمی‌شود"
                            }

                            SecurityOptionRow(
                                title = "ورود با اثر انگشت (بیومتریک)",
                                subtitle = biometricSubtitle,
                                icon = Icons.Default.Fingerprint,
                                enabled = isBiometricSupported,
                                trailingContent = {
                                    Switch(
                                        checked = isBiometricEnabled && isBiometricSupported,
                                        enabled = isBiometricSupported,
                                        onCheckedChange = { checked ->
                                            if (isBiometricSupported) {
                                                viewModel.setBiometricEnabled(checked)
                                            } else {
                                                Toast.makeText(context, biometricSubtitle, Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = EmeraldPrimary
                                        ),
                                        modifier = Modifier.testTag("biometric_switch")
                                    )
                                }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Change PIN button
                            OutlinedButton(
                                onClick = { showChangePinDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("change_pin_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = EmeraldPrimary
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LockReset,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("تغییر رمز عبور", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Close Button
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dismiss_security_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("بستن و ذخیره تنظیمات", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Sub-Dialog 1: Setup PIN Dialog
        if (showSetupPinDialog) {
            SetupPinDialog(
                viewModel = viewModel,
                isBiometricSupported = isBiometricSupported,
                onDismiss = { showSetupPinDialog = false },
                onSuccess = {
                    showSetupPinDialog = false
                    Toast.makeText(context, "رمز عبور با موفقیت تنظیم و فعال شد 🔒", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Sub-Dialog 2: Change PIN Dialog
        if (showChangePinDialog) {
            ChangePinDialog(
                viewModel = viewModel,
                onDismiss = { showChangePinDialog = false },
                onSuccess = {
                    showChangePinDialog = false
                    Toast.makeText(context, "رمز عبور با موفقیت تغییر کرد 🔑", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Sub-Dialog 3: Disable App Lock Confirm Dialog
        if (showDisableConfirmDialog) {
            DisableAppLockDialog(
                viewModel = viewModel,
                onDismiss = { showDisableConfirmDialog = false },
                onSuccess = {
                    showDisableConfirmDialog = false
                    Toast.makeText(context, "قفل برنامه غیرفعال شد", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
fun SecurityOptionRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true,
    trailingContent: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (enabled) EmeraldPrimary.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (enabled) EmeraldPrimary else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        lineHeight = 15.sp
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            trailingContent()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupPinDialog(
    viewModel: MainViewModel,
    isBiometricSupported: Boolean,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var selectedLength by remember { mutableIntStateOf(4) }
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var enableBiometric by remember { mutableStateOf(isBiometricSupported) }
    var pinVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Key, contentDescription = null, tint = EmeraldPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("تنظیم رمز عبور جدید", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "طول رمز عبور را انتخاب کنید:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedLength == 4,
                        onClick = {
                            selectedLength = 4
                            pin = ""
                            confirmPin = ""
                            errorMessage = null
                        },
                        label = { Text("رمز ۴ رقمی") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = selectedLength == 6,
                        onClick = {
                            selectedLength = 6
                            pin = ""
                            confirmPin = ""
                            errorMessage = null
                        },
                        label = { Text("رمز ۶ رقمی") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= selectedLength && it.all { char -> char.isDigit() }) {
                            pin = it
                            errorMessage = null
                        }
                    },
                    label = { Text("رمز عبور ($selectedLength رقم)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = if (pinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { pinVisible = !pinVisible }) {
                            Icon(
                                imageVector = if (pinVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("setup_pin_field"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = {
                        if (it.length <= selectedLength && it.all { char -> char.isDigit() }) {
                            confirmPin = it
                            errorMessage = null
                        }
                    },
                    label = { Text("تکرار رمز عبور") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = if (pinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().testTag("confirm_pin_field"),
                    shape = RoundedCornerShape(12.dp)
                )

                if (isBiometricSupported) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("فعال‌سازی اثر انگشت", fontSize = 12.sp)
                        }
                        Switch(
                            checked = enableBiometric,
                            onCheckedChange = { enableBiometric = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = EmeraldPrimary
                            )
                        )
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pin.length != selectedLength) {
                        errorMessage = "رمز باید دقیقاً $selectedLength رقم باشد"
                        return@Button
                    }
                    if (pin != confirmPin) {
                        errorMessage = "رمز عبور با تکرار آن یکسان نیست"
                        return@Button
                    }
                    viewModel.setupNewPin(
                        pin = pin,
                        enableBiometric = enableBiometric && isBiometricSupported,
                        onSuccess = onSuccess,
                        onError = { errorMessage = it }
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("save_new_pin_button")
            ) {
                Text("ذخیره و فعال‌سازی", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}

@Composable
fun ChangePinDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val currentLength by viewModel.pinLength.collectAsState()
    var selectedLength by remember { mutableIntStateOf(currentLength) }

    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmNewPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LockReset, contentDescription = null, tint = EmeraldPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("تغییر رمز عبور", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = currentPin,
                    onValueChange = {
                        if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                            currentPin = it
                            errorMessage = null
                        }
                    },
                    label = { Text("رمز عبور فعلی") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().testTag("current_pin_field"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedLength == 4,
                        onClick = {
                            selectedLength = 4
                            newPin = ""
                            confirmNewPin = ""
                        },
                        label = { Text("۴ رقمی") }
                    )
                    FilterChip(
                        selected = selectedLength == 6,
                        onClick = {
                            selectedLength = 6
                            newPin = ""
                            confirmNewPin = ""
                        },
                        label = { Text("۶ رقمی") }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = newPin,
                    onValueChange = {
                        if (it.length <= selectedLength && it.all { char -> char.isDigit() }) {
                            newPin = it
                            errorMessage = null
                        }
                    },
                    label = { Text("رمز جدید ($selectedLength رقم)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().testTag("new_pin_field"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = confirmNewPin,
                    onValueChange = {
                        if (it.length <= selectedLength && it.all { char -> char.isDigit() }) {
                            confirmNewPin = it
                            errorMessage = null
                        }
                    },
                    label = { Text("تکرار رمز جدید") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().testTag("confirm_new_pin_field"),
                    shape = RoundedCornerShape(12.dp)
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (currentPin.isEmpty()) {
                        errorMessage = "لطفاً رمز فعلی را وارد کنید"
                        return@Button
                    }
                    if (newPin.length != selectedLength) {
                        errorMessage = "رمز جدید باید $selectedLength رقم باشد"
                        return@Button
                    }
                    if (newPin != confirmNewPin) {
                        errorMessage = "رمز جدید با تکرار آن یکسان نیست"
                        return@Button
                    }

                    viewModel.changePin(
                        currentPin = currentPin,
                        newPin = newPin,
                        onSuccess = onSuccess,
                        onError = { errorMessage = it }
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("confirm_change_pin_button")
            ) {
                Text("تغییر رمز", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}

@Composable
fun DisableAppLockDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LockOpen, contentDescription = null, tint = Color(0xFFE53935))
                Spacer(modifier = Modifier.width(8.dp))
                Text("غیرفعال‌سازی قفل برنامه", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "جهت تایید غیرفعال‌سازی قفل و حذف رمز عبور، لطفاً رمز فعلی را وارد کنید:",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                            pin = it
                            errorMessage = null
                        }
                    },
                    label = { Text("رمز عبور فعلی") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().testTag("disable_lock_pin_field"),
                    shape = RoundedCornerShape(12.dp)
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pin.isEmpty()) {
                        errorMessage = "لطفاً رمز عبور را وارد کنید"
                        return@Button
                    }
                    viewModel.disableAppLock(
                        currentPin = pin,
                        onSuccess = onSuccess,
                        onError = { errorMessage = it }
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("confirm_disable_lock_button")
            ) {
                Text("حذف رمز و غیرفعال‌سازی", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}
