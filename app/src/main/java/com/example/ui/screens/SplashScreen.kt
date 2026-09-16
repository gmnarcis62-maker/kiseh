package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldLight
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * صفحه اسپلش مدرن، مینیمال و لوکس اپلیکیشن مدیریت مالی «کیسه»
 * با زمان‌بندی دقیق ۳ ثانیه، انیمیشن‌های نرم و هماهنگ با هویت بصری برنامه
 */
@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    // پیشرفت لودینگ خطی دقیقاً در بازه ۳۰۰۰ میلی‌ثانیه (۳ ثانیه)
    val progress = remember { Animatable(0f) }

    // انیمیشن‌های ظریف ورود المان‌ها
    val logoScale = remember { Animatable(0.85f) }
    val logoAlpha = remember { Animatable(0f) }
    val contentAlpha = remember { Animatable(0f) }

    // هاله نوری ملایم و شناور در پس‌زمینه
    val infiniteTransition = rememberInfiniteTransition(label = "ambientGlow")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    // کنترل زمان‌بندی ۳ ثانیه و اجرای انیمیشن‌های هماهنگ
    LaunchedEffect(Unit) {
        // انیمیشن ملایم ورود محتوا
        launch {
            logoAlpha.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
        }
        launch {
            logoScale.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
        }
        launch {
            delay(200)
            contentAlpha.animateTo(1f, tween(800, easing = FastOutSlowInEasing))
        }

        // انیمیشن نوار پیشرفت در طول دقیقاً ۳ ثانیه
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 3000, easing = LinearEasing)
        )

        // پس از پایان ۳ ثانیه، انتقال به صفحه اصلی
        onSplashFinished()
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            EmeraldDark,
                            Color(0xFF06291F),
                            DarkBackground
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // هاله نورانی طلایی ملایم در پشت لوگو
            Box(
                modifier = Modifier
                    .size(320.dp)
                    .scale(glowScale)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                GoldAccent.copy(alpha = glowAlpha),
                                EmeraldPrimary.copy(alpha = glowAlpha * 0.5f),
                                Color.Transparent
                            ),
                            radius = 450f
                        )
                    )
            )

            // محتوای مرکزی اسپلش (نشان کیسه، نام برنامه و شعار مینیمال)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // نشان گرافیکی شکیل و لوکس کیسه
                Box(
                    modifier = Modifier
                        .scale(logoScale.value)
                        .graphicsLayer { alpha = logoAlpha.value },
                    contentAlignment = Alignment.Center
                ) {
                    // حلقه خارجی طلایی با استایل مینیمال
                    Surface(
                        shape = CircleShape,
                        color = Color.Transparent,
                        modifier = Modifier
                            .size(116.dp)
                            .border(
                                width = 1.5.dp,
                                brush = Brush.sweepGradient(
                                    listOf(
                                        GoldAccent.copy(alpha = 0.8f),
                                        EmeraldLight.copy(alpha = 0.3f),
                                        GoldLight,
                                        GoldAccent.copy(alpha = 0.8f)
                                    )
                                ),
                                shape = CircleShape
                            )
                    ) {}

                    // دایره پس‌زمینه آیکون مرکزی
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF0B4635),
                        shadowElevation = 14.dp,
                        tonalElevation = 6.dp,
                        modifier = Modifier
                            .size(96.dp)
                            .border(
                                width = 1.dp,
                                color = GoldAccent.copy(alpha = 0.4f),
                                shape = CircleShape
                            )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = "لوگوی کیسه",
                                tint = GoldLight,
                                modifier = Modifier.size(46.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // نام و تایپوگرافی کیسه با انیمیشن ورود آرام
                Column(
                    modifier = Modifier.graphicsLayer { alpha = contentAlpha.value },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "کیسه",
                            fontSize = 34.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "مدیریت هوشمند و امن دارایی‌ها",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = GoldLight.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // فوتر پایین صفحه شامل نوار پیشرفت باریک و برچسب نسخه
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 48.dp, vertical = 36.dp)
                    .graphicsLayer { alpha = contentAlpha.value }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LinearProgressIndicator(
                        progress = { progress.value },
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = GoldAccent,
                        trackColor = Color.White.copy(alpha = 0.15f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "طراحی شده برای آرامش مالی شما",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White.copy(alpha = 0.45f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
