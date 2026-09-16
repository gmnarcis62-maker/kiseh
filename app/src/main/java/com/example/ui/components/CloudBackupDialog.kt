package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.backup.CloudBackupState
import com.example.backup.GoogleAccountState
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.util.PersianUtils
import com.example.viewmodel.MainViewModel

@Composable
fun CloudBackupDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val accountState by viewModel.googleAccountState.collectAsState()
    val backupState by viewModel.cloudBackupState.collectAsState()
    val lastMetadata by viewModel.lastBackupMetadata.collectAsState()
    val availableBackups by viewModel.availableBackups.collectAsState()

    var showConfirmRestoreDialog by remember { mutableStateOf(false) }
    var showBackupSelectionDialog by remember { mutableStateOf(false) }
    var selectedBackupForRestore by remember { mutableStateOf<com.example.backup.BackupMetadata?>(null) }
    var showGoogleConnectDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshBackupMetadata()
        viewModel.resetCloudBackupState()
    }

    LaunchedEffect(backupState) {
        if (backupState is CloudBackupState.BackupsListReady) {
            showBackupSelectionDialog = true
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Dialog(onDismissRequest = {
            viewModel.resetCloudBackupState()
            onDismiss()
        }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .testTag("cloud_backup_dialog"),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // هدر دیالوگ با پس‌زمینه گرادیان زمردی
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(EmeraldPrimary, EmeraldDark)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudQueue,
                            contentDescription = "بکاپ ابری",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "پشتیبان‌گیری و بازیابی ابری",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "ذخیره و همگام‌سازی امن اطلاعات مالی با رمزنگاری پیشرفته در فضای ابری گوگل",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // کارت وضعیت حساب کاربری گوگل
                    AccountConnectionCard(
                        accountState = accountState,
                        onConnectClick = { showGoogleConnectDialog = true },
                        onDisconnectClick = { viewModel.disconnectGoogleAccount() }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // کارت اطلاعات آخرین نسخه بکاپ
                    LastBackupInfoCard(
                        metadata = lastMetadata
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // بخش نمایش وضعیت عملیات فعلی (Progress / Success / Error)
                    AnimatedVisibility(visible = backupState !is CloudBackupState.Idle) {
                        OperationStatusBox(
                            state = backupState,
                            onDismiss = { viewModel.resetCloudBackupState() }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // دکمه‌های عملیاتی: ایجاد بکاپ / بازیابی
                    val context = androidx.compose.ui.platform.LocalContext.current
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.createCloudBackup()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("create_cloud_backup_button"),
                            enabled = backupState !is CloudBackupState.InProgress,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ایجاد بکاپ اکنون",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                if (accountState !is GoogleAccountState.Connected) {
                                    showGoogleConnectDialog = true
                                } else {
                                    viewModel.fetchAvailableBackups()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("restore_cloud_backup_button"),
                            enabled = backupState !is CloudBackupState.InProgress && backupState !is CloudBackupState.CheckingBackups,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = EmeraldPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "بازیابی اطلاعات",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = EmeraldPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // نکته امنیتی
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "فایل بکاپ با کلید ۲۵۶ بیتی AES رمزنگاری می‌شود. رمز پین دستگاه شما هرگز ذخیره نمی‌شود.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(
                        onClick = {
                            viewModel.resetCloudBackupState()
                            onDismiss()
                        },
                        modifier = Modifier.testTag("close_cloud_backup_dialog_button")
                    ) {
                        Text(
                            text = "بستن پنجره",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // دیالوگ انتخاب نسخه پشتیبان (مرحله ۲ و ۳)
        if (showBackupSelectionDialog && availableBackups.isNotEmpty()) {
            BackupSelectionDialog(
                backups = availableBackups,
                onDismiss = { showBackupSelectionDialog = false },
                onSelectBackup = { chosenBackup ->
                    selectedBackupForRestore = chosenBackup
                    showBackupSelectionDialog = false
                    showConfirmRestoreDialog = true
                }
            )
        }

        // دیالوگ هشدار و تأیید بازیابی اطلاعات (مرحله ۴)
        if (showConfirmRestoreDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmRestoreDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFD97706)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تأیید بازیابی اطلاعات",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                },
                text = {
                    Column {
                        Text(
                            text = "اطلاعات فعلی با نسخه پشتیبان جایگزین می‌شود.\nآیا ادامه می‌دهید؟",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 20.sp
                        )
                        selectedBackupForRestore?.let { sel ->
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "نسخه انتخابی: ${sel.fileName}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showConfirmRestoreDialog = false
                            val target = selectedBackupForRestore ?: lastMetadata
                            if (target != null) {
                                viewModel.restoreSelectedBackup(target)
                            } else {
                                viewModel.restoreCloudBackup()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.testTag("confirm_restore_dialog_button")
                    ) {
                        Text("بله، بازیابی کن")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showConfirmRestoreDialog = false }
                    ) {
                        Text("انصراف")
                    }
                }
            )
        }

        // دیالوگ اتصال یا ورود حساب گوگل
        if (showGoogleConnectDialog) {
            GoogleConnectInputDialog(
                onDismiss = { showGoogleConnectDialog = false },
                onConnect = { email, name ->
                    viewModel.connectGoogleAccount(email, name)
                    showGoogleConnectDialog = false
                }
            )
        }
    }
}

@Composable
fun AccountConnectionCard(
    accountState: GoogleAccountState,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .border(
                1.dp,
                if (accountState is GoogleAccountState.Connected) EmeraldPrimary.copy(alpha = 0.4f)
                else Color.Transparent,
                RoundedCornerShape(16.dp)
            )
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(34.dp),
                    tint = if (accountState is GoogleAccountState.Connected) EmeraldPrimary else Color.Gray
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (accountState is GoogleAccountState.Connected) {
                            accountState.displayName ?: "حساب متصل است"
                        } else {
                            "حساب ابری Google"
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (accountState is GoogleAccountState.Connected) {
                            accountState.email
                        } else {
                            "فضای ابری متصل نیست"
                        },
                        fontSize = 11.sp,
                        color = if (accountState is GoogleAccountState.Connected) EmeraldDark else Color.Gray
                    )
                }
            }

            if (accountState is GoogleAccountState.Connected) {
                OutlinedButton(
                    onClick = onDisconnectClick,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("disconnect_google_account_button")
                ) {
                    Text("خروج", fontSize = 11.sp, color = Color.Red)
                }
            } else {
                Button(
                    onClick = onConnectClick,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("connect_google_account_button")
                ) {
                    Text("اتصال حساب", fontSize = 11.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun LastBackupInfoCard(
    metadata: com.example.backup.BackupMetadata?
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "وضعیت آخرین نسخه بکاپ",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (metadata != null && metadata.timestamp > 0L) {
                val dateStr = PersianUtils.formatPersianDate(metadata.timestamp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("تاریخ تهیه:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(dateStr, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("تعداد تراکنش‌ها:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${PersianUtils.formatNumber(metadata.transactionCount.toLong())} عدد", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("اندازه فایل رمزگذاری‌شده:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val kb = (metadata.sizeBytes / 1024).coerceAtLeast(1)
                    Text("${PersianUtils.formatNumber(kb)} کیلوبایت", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            } else {
                Text(
                    text = "تاکنون هیچ نسخه‌ای در فضای ابری ثبت نشده است.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun OperationStatusBox(
    state: CloudBackupState,
    onDismiss: () -> Unit
) {
    val (bgColor, contentColor, icon) = when (state) {
        is CloudBackupState.CheckingBackups -> Triple(
            EmeraldPrimary.copy(alpha = 0.12f),
            EmeraldDark,
            Icons.Default.Refresh
        )
        is CloudBackupState.InProgress -> Triple(
            EmeraldPrimary.copy(alpha = 0.1f),
            EmeraldDark,
            Icons.Default.Refresh
        )
        is CloudBackupState.BackupsListReady -> Triple(
            EmeraldPrimary.copy(alpha = 0.08f),
            EmeraldDark,
            Icons.Default.CloudDone
        )
        is CloudBackupState.Success -> Triple(
            Color(0xFFE8F5E9),
            Color(0xFF2E7D32),
            Icons.Default.CheckCircle
        )
        is CloudBackupState.Error -> Triple(
            Color(0xFFFFEBEE),
            Color(0xFFC62828),
            Icons.Default.ErrorOutline
        )
        else -> Triple(Color.Transparent, Color.Transparent, Icons.Default.CloudQueue)
    }

    val message = when (state) {
        is CloudBackupState.CheckingBackups -> state.message
        is CloudBackupState.InProgress -> state.message
        is CloudBackupState.BackupsListReady -> "نسخه‌های پشتیبان موجود دریافت شدند. نسخه مورد نظر را انتخاب کنید."
        is CloudBackupState.Success -> state.message
        is CloudBackupState.Error -> state.errorMessage
        else -> ""
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (state is CloudBackupState.InProgress || state is CloudBackupState.CheckingBackups) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = contentColor
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = message,
                fontSize = 12.sp,
                color = contentColor,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun GoogleConnectInputDialog(
    onDismiss: () -> Unit,
    onConnect: (email: String, name: String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var hasError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("اتصال به حساب گوگل", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column {
                Text(
                    text = "ایمیل حساب گوگل خود را جهت ذخیره‌سازی امن بکاپ‌ها وارد کنید:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        hasError = false
                    },
                    label = { Text("آدرس ایمیل گوگل") },
                    singleLine = true,
                    isError = hasError,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("google_email_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("نام نمایشی (اختیاری)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("google_name_input")
                )
                if (hasError) {
                    Text(
                        text = "لطفاً یک آدرس ایمیل معتبر وارد کنید",
                        color = Color.Red,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (email.contains("@") && email.contains(".")) {
                        onConnect(email.trim(), name.trim().ifEmpty { email.substringBefore("@") })
                    } else {
                        hasError = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                modifier = Modifier.testTag("submit_google_connect_button")
            ) {
                Text("اتصال")
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
fun BackupSelectionDialog(
    backups: List<com.example.backup.BackupMetadata>,
    onDismiss: () -> Unit,
    onSelectBackup: (com.example.backup.BackupMetadata) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .testTag("backup_selection_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "انتخاب نسخه پشتیبان",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "نسخه مورد نظر برای بازیابی را انتخاب کنید:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(backups) { backup ->
                        BackupItemCard(
                            backup = backup,
                            onClick = { onSelectBackup(backup) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("cancel_backup_selection_button")
                    ) {
                        Text(
                            text = "انصراف",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BackupItemCard(
    backup: com.example.backup.BackupMetadata,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("backup_item_${backup.fileName}"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = backup.fileName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = EmeraldDark
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EmeraldPrimary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "نسخه: ${backup.appVersion}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = EmeraldPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "تاریخ: ${PersianUtils.formatPersianDate(backup.timestamp)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    val sizeKb = (backup.sizeBytes / 1024).coerceAtLeast(1)
                    Text(
                        text = "حجم: ${PersianUtils.formatNumber(sizeKb)} کیلوبایت",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (backup.transactionCount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "شامل ${PersianUtils.formatNumber(backup.transactionCount.toLong())} تراکنش ثبت‌شده",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
