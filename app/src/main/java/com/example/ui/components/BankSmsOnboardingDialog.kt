package com.example.ui.components

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sms.SmsPermissionManager
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent

/**
 * دیالوگ معرفی، شفافیت امنیتی و فعال‌سازی هوش پیامک بانکی
 */
@Composable
fun BankSmsOnboardingDialog(
    onDismiss: () -> Unit,
    onActivate: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var isPermanentlyDenied by remember {
        mutableStateOf(
            !SmsPermissionManager.hasSmsPermission(context) &&
                    activity != null &&
                    !androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
                        activity,
                        SmsPermissionManager.SMS_PERMISSION
                    )
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onActivate()
        } else {
            if (activity != null &&
                !androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    SmsPermissionManager.SMS_PERMISSION
                )
            ) {
                isPermanentlyDenied = true
            }
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = EmeraldPrimary.copy(alpha = 0.15f),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Sms,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "ثبت خودکار هزینه‌ها با هوش پیامکی",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldDark
                        )
                        Text(
                            text = "دستیار پیامکی آفلاین و ایمن کیسه",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "با فعال‌سازی این قابلیت، تراکنش‌های بانکی به صورت آنی شناسایی شده و آماده ثبت می‌شوند:",
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        textAlign = TextAlign.Justify
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    BenefitItem(
                        icon = Icons.Default.CloudOff,
                        title = "پردازش آفلاین روی دستگاه",
                        desc = "پیامک‌های بانکی فقط روی دستگاه شما پردازش می‌شوند."
                    )
                    BenefitItem(
                        icon = Icons.Default.Security,
                        title = "حفظ کامل حریم خصوصی",
                        desc = "هیچ پیامکی به هیچ سروری ارسال نمی‌شود."
                    )
                    BenefitItem(
                        icon = Icons.Default.Lock,
                        title = "عدم ذخیره اطلاعات حساس",
                        desc = "اطلاعات حساس و متن خام پیامک ذخیره نمی‌شود."
                    )
                    BenefitItem(
                        icon = Icons.Default.Speed,
                        title = "کنترل کامل با شماست",
                        desc = "شما همیشه قبل از ثبت نهایی تایید می‌کنید."
                    )

                    if (isPermanentlyDenied) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFFF3E0),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "مجوز دریافت پیامک قبلاً مسدود شده است. برای فعال‌سازی لازم است از بخش تنظیمات گوشی مجوز RECEIVE_SMS را فعال فرمایید.",
                                fontSize = 11.sp,
                                color = Color(0xFFE65100),
                                modifier = Modifier.padding(10.dp),
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (isPermanentlyDenied) {
                    Button(
                        onClick = {
                            SmsPermissionManager.openAppSettings(context)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("باز کردن تنظیمات", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            if (SmsPermissionManager.hasSmsPermission(context)) {
                                onActivate()
                            } else {
                                permissionLauncher.launch(SmsPermissionManager.SMS_PERMISSION)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("فعال‌سازی هوش پیامک", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("بعداً", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun BenefitItem(
    icon: ImageVector,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = CircleShape,
            color = EmeraldPrimary.copy(alpha = 0.12f),
            modifier = Modifier
                .padding(top = 2.dp)
                .size(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = desc,
                fontSize = 10.5.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                lineHeight = 15.sp
            )
        }
    }
}
