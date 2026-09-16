package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.billing.BillingManager
import com.example.billing.FreePlanLimits
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent

/**
 * دیالوگ جامع «راهنما و امکانات» برنامه کیسه همراه با نمایش دقیق وضعیت اشتراک (Free / VIP).
 */
@Composable
fun AppHelpAndGuideDialog(
    onDismiss: () -> Unit,
    onUpgradeToVip: () -> Unit
) {
    val context = LocalContext.current
    val isProUser = BillingManager.isProUser(context)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismiss,
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("app_help_and_guide_dialog"),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = EmeraldPrimary.copy(alpha = 0.15f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "راهنما و امکانات کیسه",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.testTag("help_dialog_title")
                            )
                            Text(
                                text = "دستیار هوشمند مدیریت مالی",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_help_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "بستن",
                            tint = Color.Gray
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
                    // ۱. کارت وضعیت اشتراک
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("subscription_status_card"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isProUser) EmeraldPrimary.copy(alpha = 0.1f) else GoldAccent.copy(alpha = 0.1f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isProUser) "نسخه فعلی: VIP ✓" else "نسخه فعلی: رایگان",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (isProUser) EmeraldDark else Color(0xFFD48800),
                                    modifier = Modifier.testTag("subscription_status_text")
                                )

                                if (!isProUser) {
                                    Button(
                                        onClick = {
                                            onDismiss()
                                            onUpgradeToVip()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.testTag("upgrade_from_help_button")
                                    ) {
                                        Text(
                                            text = "خرید VIP",
                                            color = Color.Black,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }

                            Text(
                                text = if (isProUser)
                                    "تمام امکانات بدون محدودیت فعال هستند."
                                else
                                    "تمام قابلیت‌ها جهت تست فعال می‌باشند اما دارای محدودیت تعداد استفاده هستند.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )

                            // نمایش لیست محدودیت‌ها فقط برای کاربران رایگان
                            if (!isProUser) {
                                Divider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    color = Color.LightGray.copy(alpha = 0.4f)
                                )
                                Text(
                                    text = "محدودیت‌های نسخه رایگان:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldDark
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("• ${FreePlanLimits.LIMIT_VOICE_LABEL}", fontSize = 11.5.sp)
                                    Text("• ${FreePlanLimits.LIMIT_BANK_SMS_LABEL}", fontSize = 11.5.sp)
                                    Text("• ${FreePlanLimits.LIMIT_INVOICE_LABEL}", fontSize = 11.5.sp)
                                    Text("• ${FreePlanLimits.LIMIT_SAVINGS_LABEL}", fontSize = 11.5.sp)
                                    Text("• ${FreePlanLimits.LIMIT_RECURRING_LABEL}", fontSize = 11.5.sp)
                                    Text("• ${FreePlanLimits.LIMIT_BACKUP_LABEL}", fontSize = 11.5.sp)
                                }
                            }
                        }
                    }

                    // ۲. معرفی برنامه
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "درباره اپلیکیشن کیسه",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = EmeraldPrimary
                            )
                            Text(
                                text = "کیسه یک دستیار مدیریت مالی شخصی است که به شما کمک می‌کند هزینه‌ها، درآمدها و برنامه مالی خود را مدیریت کنید.",
                                fontSize = 12.5.sp,
                                lineHeight = 20.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                modifier = Modifier.testTag("app_intro_text")
                            )
                        }
                    }

                    // ۳. لیست امکانات
                    Text(
                        text = "امکانات و قابلیت‌های اصلی:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    val featureList = listOf(
                        Triple(Icons.Default.EditNote, "ثبت دستی تراکنش‌ها", "ثبت سریع هزینه‌ها و درآمدها با مشخص نمودن دسته‌بندی و حساب"),
                        Triple(Icons.Default.Mic, "ثبت صوتی هزینه و درآمد", "تبدیل گفتار فارسی به تراکنش با تشخیص هوشمند مبلغ و دسته"),
                        Triple(Icons.Default.Sms, "هوش پیامکی بانکی", "شناسایی و ثبت خودکار تراکنش‌ها از پیامک‌های واریز و برداشت بانک"),
                        Triple(Icons.Default.DocumentScanner, "اسکن و تحلیل هوشمند فاکتور", "استخراج خودکار آیتم‌ها و قیمت‌ها از روی تصویر فاکتور خرید"),
                        Triple(Icons.Default.Savings, "اهداف پس‌انداز", "تعریف صندوق‌های هدف و پیگیری درصد پیشرفت پس‌اندازها"),
                        Triple(Icons.Default.Repeat, "تراکنش‌های دوره‌ای", "مدیریت اجاره، اقساط وام و درآمدهای تکرارشونده سر موعد"),
                        Triple(Icons.Default.PieChart, "گزارش‌های مالی", "نمایش نمودارها و تحلیل دسته‌بندی هزینه‌ها و روند ماهانه"),
                        Triple(Icons.Default.CloudSync, "پشتیبان‌گیری و بازیابی", "ذخیره‌سازی امن اطلاعات روی گوگل درایو شخصی")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        featureList.forEach { (icon, title, desc) ->
                            FeatureHelpRow(icon = icon, title = title, desc = desc)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("متوجه شدم", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        )
    }
}

@Composable
private fun FeatureHelpRow(
    icon: ImageVector,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = EmeraldPrimary.copy(alpha = 0.12f),
            modifier = Modifier.size(34.dp)
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
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = desc,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}
