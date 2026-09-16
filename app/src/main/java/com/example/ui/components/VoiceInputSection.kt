package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.analyzer.AnalysisResult
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.util.PersianUtils
import com.example.voice.VoiceState

@Composable
fun VoiceInputSection(
    voiceState: VoiceState,
    analysisResult: AnalysisResult?,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onSimulateInput: (String) -> Unit,
    onConfirmResult: (AnalysisResult) -> Unit,
    onDismissResult: () -> Unit,
    modifier: Modifier = Modifier,
    onStartSystemVoiceInput: (() -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isProUser by com.example.billing.BillingManager.isProState.collectAsState()
    val remainingVoiceTrial = remember(voiceState, isProUser) {
        com.example.billing.BillingManager.getRemainingVoiceTrial(context)
    }

    var manualText by remember { mutableStateOf("") }

    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val sampleInputs = listOf(
        "۲۰ تومن نون خریدم",
        "۵۰ هزار تومان بنزین زدم",
        "۱۲۰ هزار تومن قبض اینترنت",
        "۲.۵ میلیون اجاره خونه دادم",
        "۳۵۰ هزار تومان کفش خریدم",
        "۱۵ هزار تومان دکتر رفتم",
        "۵ میلیون واریز حقوق",
        "۸۰۰ هزار تومان خرید فروشگاه"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("voice_input_section"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "ثبت هوشمند",
                        tint = EmeraldPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ثبت گفتاری / متنی هزینه",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isProUser) GoldAccent.copy(alpha = 0.25f) else EmeraldPrimary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = if (isProUser) "گفتاری نامحدود ✨" else "روزانه $remainingVoiceTrial از ۵ بار",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mic Button & Pulse Area
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                if (voiceState is VoiceState.Listening) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(EmeraldPrimary.copy(alpha = 0.3f))
                    )
                }

                IconButton(
                    onClick = {
                        if (voiceState is VoiceState.Listening) {
                            onStopListening()
                        } else {
                            onStartListening()
                        }
                    },
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            if (voiceState is VoiceState.Listening) Color(0xFFE55656) else EmeraldPrimary
                        )
                        .testTag("mic_button")
                ) {
                    Icon(
                        imageVector = if (voiceState is VoiceState.Listening) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "ضبط صدا",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Voice Status Text
            val statusText = when (voiceState) {
                is VoiceState.Idle -> "روی دکمه میکروفون بزنید و بگویید: مثلا «۲۰ تومن نون خریدم»"
                is VoiceState.Listening -> "در حال گوش دادن... صحبت کنید"
                is VoiceState.Processing -> voiceState.partialText
                is VoiceState.Success -> "تشخیص داده شد: \"${voiceState.text}\""
                is VoiceState.Error -> voiceState.message
            }

            Text(
                text = statusText,
                fontSize = 12.sp,
                color = if (voiceState is VoiceState.Error) Color(0xFFE55656) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )

            var showVoiceHelpDialog by remember { mutableStateOf(false) }

            if (voiceState is VoiceState.Error) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = { showVoiceHelpDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("راهنمای فعال‌سازی سرویس صوتی در دستگاه‌های مختلف", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }

                if (showVoiceHelpDialog) {
                    VoiceServiceHelpDialog(
                        onDismiss = { showVoiceHelpDialog = false },
                        onOpenSettings = {
                            com.example.voice.VoiceInputManager.openVoiceSettings(context)
                        },
                        onRetry = {
                            showVoiceHelpDialog = false
                            onStartListening()
                        },
                        onUseSystemAssistant = {
                            showVoiceHelpDialog = false
                            onStartSystemVoiceInput?.invoke()
                        }
                    )
                }
            }

            if (voiceState is VoiceState.Error || voiceState is VoiceState.Idle) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = "💡 راهنما: در صورت عدم کارکرد میکروفون مستقیم در شبیه‌ساز یا برخی گوشی‌ها، می‌توانید از میکروفون کیبورد (Gboard) یا نمونه‌جملات آماده زیر استفاده نمایید.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Text Input Box with Instant Analyze
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = manualText,
                    onValueChange = { manualText = it },
                    placeholder = { Text("یا اینجا بنویسید (مثلاً: ۵۰ هزار تومان بنزین)", fontSize = 12.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("manual_input_field"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        if (manualText.isNotBlank()) {
                            onSimulateInput(manualText)
                            manualText = ""
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    modifier = Modifier.testTag("analyze_text_button")
                ) {
                    Text("تحلیل", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Sample Test Chips
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "تست سریع نمونه جملات گفتاری:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sampleInputs.forEach { sample ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onSimulateInput(sample) }
                        ) {
                            Text(
                                text = sample,
                                fontSize = 11.sp,
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Parsed Result Card Preview
            AnimatedVisibility(
                visible = analysisResult != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                analysisResult?.let { result ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                            .border(1.dp, EmeraldPrimary.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(
                            containerColor = EmeraldPrimary.copy(alpha = 0.08f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "نتیجه تحلیل هوشمند گفتار",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )

                                IconButton(
                                    onClick = onDismissResult,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "انصراف",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "مبلغ: ${PersianUtils.formatCurrencyToman(result.amount)}",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (result.isIncome) Color(0xFF2E7D32) else Color(0xFFE55656)
                                    )
                                    Text(
                                        text = "توضیح: ${result.description}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = CategoryHelper.getCategoryMeta(result.category).color.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = result.category,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CategoryHelper.getCategoryMeta(result.category).color,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = { onConfirmResult(result) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("confirm_parsed_transaction_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "ذخیره در کیسه"
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تایید و ذخیره در کیسه", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
