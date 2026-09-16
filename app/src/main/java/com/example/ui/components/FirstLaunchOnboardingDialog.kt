package com.example.ui.components

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent

/**
 * صفحه و دیالوگ خوش‌آمدگویی و معرفی امکانات در اولین اجرای واقعی برنامه
 */
@Composable
fun FirstLaunchOnboardingDialog(
    onDismiss: () -> Unit,
    onStart: () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .heightIn(max = 700.dp)
                    .padding(vertical = 16.dp)
                    .testTag("first_launch_dialog"),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                ) {
                    // هدر دیالوگ خوش‌آمدگویی
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = EmeraldPrimary.copy(alpha = 0.12f),
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "کیسه",
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "به کیسه خوش آمدید",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldDark,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "مدیریت ساده و هوشمند دخل و خرج شما",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // محتوای قابل اسکرول شامل کارت‌های قابلیت و مقایسه Free / VIP
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // معرفی قابلیت‌ها به صورت کارت‌های شکیل
                        Text(
                            text = "✨ قابلیت‌های اصلی کیسه",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )

                        OnboardingFeatureCard(
                            tag = "feature_voice_card",
                            icon = Icons.Default.Mic,
                            iconColor = EmeraldPrimary,
                            title = "🎙️ ثبت صوتی دخل و خرج",
                            description = "تراکنش‌های خود را سریع و با صدا ثبت کنید."
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OnboardingFeatureCard(
                            tag = "feature_sms_card",
                            icon = Icons.Default.Sms,
                            iconColor = Color(0xFF2563EB),
                            title = "🧠 هوش پیامکی بانکی",
                            description = "تراکنش‌های بانکی را هوشمندانه شناسایی و دسته‌بندی کنید."
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OnboardingFeatureCard(
                            tag = "feature_report_card",
                            icon = Icons.Default.Insights,
                            iconColor = Color(0xFF7C3AED),
                            title = "📊 مدیریت و تحلیل مالی",
                            description = "هزینه‌ها و درآمدهای خود را بهتر مدیریت و بررسی کنید."
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OnboardingFeatureCard(
                            tag = "feature_backup_card",
                            icon = Icons.Default.CloudDone,
                            iconColor = Color(0xFF0284C7),
                            title = "☁️ پشتیبان‌گیری و بازیابی",
                            description = "از اطلاعات خود نسخه پشتیبان تهیه کنید و در صورت نیاز بازیابی کنید."
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OnboardingFeatureCard(
                            tag = "feature_security_card",
                            icon = Icons.Default.Security,
                            iconColor = EmeraldDark,
                            title = "🔒 امنیت و حریم خصوصی",
                            description = "اطلاعات شما با حفظ حریم خصوصی و امنیت نگهداری می‌شود."
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // تفاوت نسخه رایگان و VIP
                        Text(
                            text = "⚖️ مقایسه نسخه رایگان و VIP",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldDark,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // کارت نسخه رایگان
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("free_tier_card"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "نسخه رایگان",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF475569)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TierBulletItem("ثبت صوتی دخل و خرج")
                                    TierBulletItem("۵ ثبت موفق در روز")
                                }
                            }

                            // کارت نسخه VIP
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("vip_tier_card"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = GoldAccent.copy(alpha = 0.08f)
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldAccent.copy(alpha = 0.6f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = GoldAccent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "VIP",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = EmeraldDark
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TierBulletItem("تمام امکانات برنامه", isVip = true)
                                    TierBulletItem("ثبت صوتی نامحدود", isVip = true)
                                    TierBulletItem("هوش پیامکی حرفه‌ای", isVip = true)
                                    TierBulletItem("امکانات هوشمند", isVip = true)
                                    TierBulletItem("امکانات حرفه‌ای", isVip = true)
                                    TierBulletItem("بدون محدودیت", isVip = true)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // دکمه‌های اقدام First Launch
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(
                            onClick = onStart,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("first_launch_start_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = "شروع استفاده از کیسه",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("first_launch_later_button")
                        ) {
                            Text(
                                text = "بعداً",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingFeatureCard(
    tag: String,
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = iconColor.copy(alpha = 0.1f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun TierBulletItem(
    text: String,
    isVip: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = if (isVip) GoldAccent else EmeraldPrimary,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            fontSize = 10.5.sp,
            fontWeight = if (isVip) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isVip) EmeraldDark else Color(0xFF334155)
        )
    }
}
