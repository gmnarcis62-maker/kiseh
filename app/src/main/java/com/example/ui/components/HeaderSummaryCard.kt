package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.util.PersianUtils

@Composable
fun HeaderSummaryCard(
    totalExpense: Long,
    totalIncome: Long,
    isRial: Boolean,
    onToggleCurrency: () -> Unit,
    budgetLimit: Long = 0L,
    onEditBudget: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val netBalance = totalIncome - totalExpense

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("header_summary_card"),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            EmeraldDark,
                            EmeraldPrimary
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                // Header Top Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "موجودی کل (کیسه)",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )

                    // Currency Unit Toggle Badge
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(GoldAccent.copy(alpha = 0.25f))
                            .clickable { onToggleCurrency() }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CurrencyExchange,
                            contentDescription = "تغییر واحد پول",
                            tint = GoldAccent,
                            modifier = Modifier.height(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isRial) "واحد: ریال" else "واحد: تومان",
                            color = GoldAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Net Balance Text
                AnimatedContent(targetState = netBalance, label = "balance_anim") { balance ->
                    val formattedValue = if (isRial) {
                        PersianUtils.formatCurrencyRial(balance)
                    } else {
                        PersianUtils.formatCurrencyToman(balance)
                    }

                    Text(
                        text = formattedValue,
                        color = if (balance >= 0) Color.White else Color(0xFFFF8A8A),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Sub Statistics Row (Expenses & Income)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.2f))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Total Expense Item
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE55656).copy(alpha = 0.25f))
                                .padding(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "کل هزینه‌ها",
                                tint = Color(0xFFFF8A8A)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "مجموع هزینه‌ها",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )
                            val expText = if (isRial) {
                                PersianUtils.formatCurrencyRial(totalExpense)
                            } else {
                                PersianUtils.formatCurrencyToman(totalExpense)
                            }
                            Text(
                                text = expText,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .width(1.dp)
                            .background(Color.White.copy(alpha = 0.2f))
                    )

                    // Total Income Item
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF4CAF50).copy(alpha = 0.25f))
                                .padding(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = "کل درآمدها",
                                tint = Color(0xFF81C784)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "مجموع درآمدها",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )
                            val incText = if (isRial) {
                                PersianUtils.formatCurrencyRial(totalIncome)
                            } else {
                                PersianUtils.formatCurrencyToman(totalIncome)
                            }
                            Text(
                                text = incText,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Monthly Budget Indicator Section
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .clickable { onEditBudget() }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (budgetLimit > 0) "بودجه ماهانه:" else "تعیین سقف بودجه ماهانه 🎯",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            if (budgetLimit > 0) {
                                val percent = ((totalExpense.toDouble() / budgetLimit.toDouble()) * 100).toInt().coerceIn(0, 100)
                                Text(
                                    text = "$percent٪ مصرف شده",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (percent > 90) Color(0xFFFF8A8A) else GoldAccent
                                )
                            }
                        }

                        if (budgetLimit > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            val progress = (totalExpense.toFloat() / budgetLimit.toFloat()).coerceIn(0f, 1f)
                            val barColor = when {
                                progress > 0.9f -> Color(0xFFFF5252)
                                progress > 0.75f -> Color(0xFFFFB74D)
                                else -> Color(0xFF81C784)
                            }
                            androidx.compose.material3.LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = barColor,
                                trackColor = Color.White.copy(alpha = 0.2f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${PersianUtils.formatCurrencyToman(totalExpense)} از ${PersianUtils.formatCurrencyToman(budgetLimit)}",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            if (totalExpense > budgetLimit) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "⚠️ عبور از سقف: ${PersianUtils.formatCurrencyToman(totalExpense - budgetLimit)} مازاد هزینه!",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF8A8A)
                                )
                            }
                        } else if (totalIncome > 0 && totalExpense > totalIncome) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "🚨 هشدار دخل و خرج: مجموع هزینه‌ها بیشتر از درآمد است!",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF8A8A)
                            )
                        }
                    }
                }
            }
        }
    }
}
