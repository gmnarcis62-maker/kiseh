package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.billing.BillingManager
import com.example.data.SavingsGoal
import com.example.ui.components.AddDepositDialog
import com.example.ui.components.AddEditSavingsGoalDialog
import com.example.ui.components.ProPaywallDialog
import com.example.ui.components.SavingsGoalCard
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.util.PersianUtils
import com.example.viewmodel.MainViewModel

@Composable
fun SavingsGoalsSection(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val goals by viewModel.allSavingsGoals.collectAsState()
    val isVip by BillingManager.isVipState.collectAsState()
    val context = LocalContext.current

    var showPaywallDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<SavingsGoal?>(null) }
    var depositGoal by remember { mutableStateOf<SavingsGoal?>(null) }
    var goalToDelete by remember { mutableStateOf<SavingsGoal?>(null) }

    val totalTarget = goals.sumOf { it.targetAmount }
    val totalSaved = goals.sumOf { it.savedAmount }
    val completedCount = goals.count { it.isCompleted }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("savings_goals_section")
    ) {
        // بنر وضعیت سهمیه هوشمند (Smart Free Trial) برای کاربران رایگان
        if (!isVip) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("vip_savings_gate_banner"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2620)),
                border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stars,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val remaining = (1 - goals.size).coerceAtLeast(0)
                        Text(
                            text = if (remaining > 0) "۱ هدف رایگان فعال است" else "۱ هدف رایگان فعال است. برای ثبت اهداف جدید ارتقا دهید",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { showPaywallDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("buy_vip_from_bazaar_savings")
                    ) {
                        Text(
                            text = "خرید VIP از کافهبازار",
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Overall Summary Card for Savings Goals
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = EmeraldDark),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = GoldAccent.copy(alpha = 0.2f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Savings,
                                    contentDescription = null,
                                    tint = GoldAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "صندوق اهداف پس‌انداز",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                            Text(
                                text = if (goals.isEmpty()) "هدف مالی جدیدی ثبت نشده" else "${PersianUtils.toPersianDigits(completedCount.toString())} از ${PersianUtils.toPersianDigits(goals.size.toString())} هدف محقق شده",
                                fontSize = 11.sp,
                                color = GoldAccent
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (!isVip && goals.size >= 1) {
                                showPaywallDialog = true
                                Toast.makeText(context, "در نسخه رایگان حداکثر ۱ هدف پس‌انداز فعال مجاز است. برای اهداف نامحدود نسخه VIP را فعال کنید.", Toast.LENGTH_SHORT).show()
                            } else {
                                editingGoal = null
                                showAddDialog = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("new_goal_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "هدف جدید",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                if (goals.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "مجموع پس‌انداز شده",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Text(
                                text = PersianUtils.formatCurrencyToman(totalSaved),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldAccent
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "مجموع مبلغ اهداف",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Text(
                                text = PersianUtils.formatCurrencyToman(totalTarget),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Goals List or Empty State
        if (goals.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = EmeraldPrimary.copy(alpha = 0.1f),
                        modifier = Modifier.size(80.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Savings,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "هنوز هیچ هدف پس‌اندازی ثبت نکرده‌اید",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "برای خریدهای بزرگ (مثل لپ‌تاپ، سفر یا خودرو) هدف تعیین کنید تا گام‌به‌گام به آن برسید.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = {
                            if (!isVip && goals.size >= 1) {
                                showPaywallDialog = true
                                Toast.makeText(context, "در نسخه رایگان حداکثر ۱ هدف پس‌انداز فعال مجاز است.", Toast.LENGTH_SHORT).show()
                            } else {
                                editingGoal = null
                                showAddDialog = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("empty_add_goal_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "تعریف اولین هدف مالی",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(goals, key = { it.id }) { goal ->
                    SavingsGoalCard(
                        goal = goal,
                        onAddDeposit = {
                            depositGoal = it
                        },
                        onEditGoal = {
                            editingGoal = it
                            showAddDialog = true
                        },
                        onDeleteGoal = {
                            goalToDelete = it
                        }
                    )
                }
            }
        }
    }

    // Add / Edit Dialog
    if (showAddDialog && (isVip || goals.size < 1 || editingGoal != null)) {
        AddEditSavingsGoalDialog(
            goal = editingGoal,
            onDismiss = {
                showAddDialog = false
                editingGoal = null
            },
            onSave = { title, targetAmount, initialSaved, note, iconName, colorHex ->
                if (editingGoal != null) {
                    viewModel.updateSavingsGoal(
                        editingGoal!!.copy(
                            title = title,
                            targetAmount = targetAmount,
                            savedAmount = initialSaved,
                            note = note,
                            iconName = iconName,
                            colorHex = colorHex
                        )
                    )
                    Toast.makeText(context, "هدف مالی «$title» ویرایش شد", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.addSavingsGoal(
                        title = title,
                        targetAmount = targetAmount,
                        initialSaved = initialSaved,
                        note = note,
                        iconName = iconName,
                        colorHex = colorHex
                    )
                    Toast.makeText(context, "هدف جدید «$title» با موفقیت اضافه شد", Toast.LENGTH_SHORT).show()
                }
                showAddDialog = false
                editingGoal = null
            }
        )
    }

    // Deposit Dialog
    depositGoal?.let { goal ->
        AddDepositDialog(
            goal = goal,
            onDismiss = { depositGoal = null },
            onConfirmDeposit = { amount ->
                viewModel.addDepositToGoal(goal.id, amount)
                val newSaved = goal.savedAmount + amount
                if (newSaved >= goal.targetAmount && goal.targetAmount > 0) {
                    Toast.makeText(context, "🎉 تبریک! شما به هدف «${goal.title}» رسیدید!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "مبلغ ${PersianUtils.formatCurrencyToman(amount)} به هدف «${goal.title}» واریز شد", Toast.LENGTH_SHORT).show()
                }
                depositGoal = null
            }
        )
    }

    // Delete Confirmation Dialog
    goalToDelete?.let { goal ->
            AlertDialog(
                onDismissRequest = { goalToDelete = null },
                title = { Text("حذف هدف مالی", fontWeight = FontWeight.Bold) },
                text = { Text("آیا از حذف هدف «${goal.title}» اطمینان دارید؟") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteSavingsGoal(goal)
                            Toast.makeText(context, "هدف «${goal.title}» حذف شد", Toast.LENGTH_SHORT).show()
                            goalToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE55656))
                    ) {
                        Text("حذف")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { goalToDelete = null }) {
                        Text("انصراف")
                    }
                }
            )
        }

    // Paywall Dialog
    if (showPaywallDialog) {
        ProPaywallDialog(
            onDismiss = { showPaywallDialog = false }
        )
    }
}
