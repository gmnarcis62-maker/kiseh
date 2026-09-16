package com.example.ui.components

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleanHands
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.billing.BillingManager
import com.example.sms.BankSmsPreferencesManager
import com.example.sms.BankType
import com.example.sms.SmsPermissionManager
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.launch

/**
 * دیالوگ تنظیمات جامع هوش پیامک بانکی
 */
@Composable
fun BankSmsSettingsDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onShowOnboarding: () -> Unit,
    onUpgradeToPro: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember { BankSmsPreferencesManager.getInstance(context) }
    val isProUser by BillingManager.isProState.collectAsState()

    val isBankSmsEnabled by prefs.isBankSmsEnabledFlow.collectAsState(initial = false)
    val isNotificationEnabled by prefs.isNotificationEnabledFlow.collectAsState(initial = true)
    val isQuickActionEnabled by prefs.isQuickActionEnabledFlow.collectAsState(initial = true)
    val isSmartCategoryEnabled by prefs.isSmartCategoryEnabledFlow.collectAsState(initial = true)
    val isUserLearningEnabled by prefs.isUserLearningEnabledFlow.collectAsState(initial = true)
    val isAutoCleanupEnabled by prefs.isAutoCleanupEnabledFlow.collectAsState(initial = false)
    val isAutoConfirmEnabled by prefs.isAutoConfirmEnabledFlow.collectAsState(initial = false)

    var hasPermission by remember { mutableStateOf(SmsPermissionManager.hasSmsPermission(context)) }
    var isSupportedBanksExpanded by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            coroutineScope.launch { prefs.setBankSmsEnabled(true) }
            Toast.makeText(context, "هوش پیامک بانکی فعال شد", Toast.LENGTH_SHORT).show()
        } else {
            coroutineScope.launch { prefs.setBankSmsEnabled(false) }
            Toast.makeText(context, "برای فعال‌سازی، مجوز دریافت پیامک الزامی است", Toast.LENGTH_LONG).show()
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = EmeraldPrimary.copy(alpha = 0.15f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Sms,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "تنظیمات هوش پیامک بانکی",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldDark
                            )
                            Text(
                                text = if (isProUser) "نسخه نامحدود VIP" else "۳ از ۳ تراکنش آزمایشی استفاده شده",
                                fontSize = 11.sp,
                                color = if (isProUser) EmeraldPrimary else GoldAccent
                            )
                        }
                    }

                    IconButton(onClick = onShowOnboarding) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "راهنما و امنیت",
                            tint = EmeraldPrimary
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // ۱. کلید اصلی فعال / غیرفعال
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isBankSmsEnabled && hasPermission)
                                EmeraldPrimary.copy(alpha = 0.08f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
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
                                    text = "فعال‌سازی هوش پیامک",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "شناسایی خودکار تراکنش‌ها از پیامک‌های بانکی",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                )
                            }
                            Switch(
                                checked = isBankSmsEnabled && hasPermission,
                                onCheckedChange = { enable ->
                                    if (enable) {
                                        if (SmsPermissionManager.hasSmsPermission(context)) {
                                            coroutineScope.launch { prefs.setBankSmsEnabled(true) }
                                        } else {
                                            permissionLauncher.launch(SmsPermissionManager.SMS_PERMISSION)
                                        }
                                    } else {
                                        coroutineScope.launch { prefs.setBankSmsEnabled(false) }
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = EmeraldPrimary
                                )
                            )
                        }
                    }

                    // ۲. حالت تشخیص و ثبت تراکنش
                    Text(
                        text = "نحوه ثبت تراکنش‌ها:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // گزینه ۱: تأیید قبل از ثبت (پیش‌فرض)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (!isAutoConfirmEnabled) EmeraldPrimary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (!isAutoConfirmEnabled) 1.5.dp else 1.dp,
                                color = if (!isAutoConfirmEnabled) EmeraldPrimary else Color.LightGray.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    coroutineScope.launch { prefs.setAutoConfirmEnabled(false) }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (!isAutoConfirmEnabled) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (!isAutoConfirmEnabled) EmeraldPrimary else Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "تأیید قبل از ثبت (پیش‌فرض و مطمئن)",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "پیامک‌ها ابتدا به صف تایید اضافه شده و با بررسی شما ثبت می‌شوند.",
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                    )
                                }
                            }
                        }

                        // گزینه ۲: ثبت خودکار تراکنش‌های مطمئن
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isAutoConfirmEnabled) EmeraldPrimary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isAutoConfirmEnabled) 1.5.dp else 1.dp,
                                color = if (isAutoConfirmEnabled) EmeraldPrimary else Color.LightGray.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    coroutineScope.launch { prefs.setAutoConfirmEnabled(true) }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isAutoConfirmEnabled) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (isAutoConfirmEnabled) EmeraldPrimary else Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "ثبت خودکار تراکنش‌های مطمئن",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "تراکنش‌های دارای ضریب اطمینان بالا به طور خودکار به حساب اضافه می‌شوند.",
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                    // ۳. سوییچ‌های قابلیت‌ها
                    Text(
                        text = "سایر قابلیت‌های هوشمند:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )

                    SettingToggleItem(
                        icon = Icons.Default.Notifications,
                        title = "دریافت اعلان تراکنش جدید",
                        desc = "نمایش اعلان لحظه‌ای هنگام دریافت پیامک بانکی",
                        checked = isNotificationEnabled,
                        onCheckedChange = { coroutineScope.launch { prefs.setNotificationEnabled(it) } }
                    )

                    SettingToggleItem(
                        icon = Icons.Default.TouchApp,
                        title = "ثبت سریع از اعلان",
                        desc = "امکان تایید یا نادیده گرفتن مستقیم از دکمه‌های نوتیفیکیشن",
                        checked = isQuickActionEnabled,
                        onCheckedChange = { coroutineScope.launch { prefs.setQuickActionEnabled(it) } }
                    )

                    SettingToggleItem(
                        icon = Icons.Default.Psychology,
                        title = "دسته‌بندی هوشمند",
                        desc = "پیشنهاد خودکار دسته هزینه بر اساس نام پذیرنده و توضیحات",
                        checked = isSmartCategoryEnabled,
                        onCheckedChange = { coroutineScope.launch { prefs.setSmartCategoryEnabled(it) } }
                    )

                    SettingToggleItem(
                        icon = Icons.Default.AutoMode,
                        title = "یادگیری از انتخاب‌های کاربر",
                        desc = "به‌خاطرسپاری دسته‌بندی فروشگاه‌ها از اصلاحات قبلی شما",
                        checked = isUserLearningEnabled,
                        onCheckedChange = { coroutineScope.launch { prefs.setUserLearningEnabled(it) } }
                    )

                    SettingToggleItem(
                        icon = Icons.Default.CleanHands,
                        title = "پاکسازی خودکار پیامک‌های قدیمی",
                        desc = "حذف خودکار رکوردهای پردازش‌شده بالای ۳۰ روز برای صرفه‌جویی در حافظه",
                        checked = isAutoCleanupEnabled,
                        onCheckedChange = { coroutineScope.launch { prefs.setAutoCleanupEnabled(it) } }
                    )

                    // دکمه پاکسازی دستی پیامک‌های قدیمی
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 3600 * 1000L
                                viewModel.cleanupOldProcessedSms(thirtyDaysAgo)
                                Toast.makeText(context, "پیامک‌های پردازش‌شده قدیمی پاکسازی شدند", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("پاکسازی پیامک‌های پردازش‌شده گذشته", fontSize = 12.sp)
                    }

                    // ۴. بخش بانک‌های پشتیبانی‌شده
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isSupportedBanksExpanded = !isSupportedBanksExpanded }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = EmeraldPrimary.copy(alpha = 0.12f),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.AccountBalance,
                                                contentDescription = null,
                                                tint = EmeraldPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "بانک‌های پشتیبانی شده",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${BankType.supportedBanks.size} بانک و نئوبانک فعال",
                                            fontSize = 10.5.sp,
                                            color = EmeraldPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                Text(
                                    text = if (isSupportedBanksExpanded) "بستن ▲" else "مشاهده لیست ▼",
                                    fontSize = 11.sp,
                                    color = EmeraldPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            if (isSupportedBanksExpanded) {
                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                Spacer(modifier = Modifier.height(8.dp))

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    BankType.supportedBanks.forEach { bank ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = EmeraldPrimary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = bank.displayName,
                                                fontSize = 11.5.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (bank.isNeobank) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = GoldAccent.copy(alpha = 0.2f)
                                                ) {
                                                    Text(
                                                        text = "نئوبانک",
                                                        fontSize = 9.sp,
                                                        color = EmeraldDark,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ۵. کارت وضعیت حریم خصوصی و پردازش محلی
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = EmeraldPrimary.copy(alpha = 0.07f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "حریم خصوصی و امنیت تضمین‌شده",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldDark
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "تمام پردازش‌ها ۱۰۰٪ آفلاین و روی دستگاه انجام می‌شود. متن خام پیامک‌ها و رمزهای پویا هرگز ذخیره یا ارسال نمی‌شوند.",
                                    fontSize = 10.5.sp,
                                    lineHeight = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("بستن", fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun SettingToggleItem(
    icon: ImageVector,
    title: String,
    desc: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = EmeraldPrimary.copy(alpha = 0.1f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = desc,
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    lineHeight = 14.sp
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = EmeraldPrimary
            )
        )
    }
}
