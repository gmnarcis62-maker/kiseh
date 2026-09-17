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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.data.RecurringTransaction
import com.example.ui.components.AddEditRecurringTransactionDialog
import com.example.ui.components.ProPaywallDialog
import com.example.ui.components.RecurringTransactionCard
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.util.PersianUtils
import com.example.viewmodel.MainViewModel

@Composable
fun RecurringTransactionsSection(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val recurringList by viewModel.allRecurringTransactions.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()
    val isVip by BillingManager.isVipState.collectAsState()
    val context = LocalContext.current

    var showPaywallDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<RecurringTransaction?>(null) }
    var itemToDelete by remember { mutableStateOf<RecurringTransaction?>(null) }

    val activeCount = recurringList.count { it.isActive }
    val totalActiveMonthlyEstimated = recurringList.filter { it.isActive && !it.isIncome }.sumOf {
        when (it.recurrencePeriod) {
            com.example.data.RecurrencePeriod.DAILY -> it.amount * 30
            com.example.data.RecurrencePeriod.WEEKLY -> it.amount * 4
            com.example.data.RecurrencePeriod.MONTHLY -> it.amount
            com.example.data.RecurrencePeriod.YEARLY -> it.amount / 12
        }
    }

    Scaffold(
        modifier = modifier.testTag("recurring_transactions_section"),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (!isVip && activeCount >= 1) {
                        showPaywallDialog = true
                        Toast.makeText(context, "در نسخه رایگان حداکثر ۱ تراکنش دوره‌ای فعال مجاز است. برای تعداد نامحدود نسخه VIP را فعال نمایید.", Toast.LENGTH_SHORT).show()
                    } else {
                        editingItem = null
                        showAddDialog = true
                    }
                },
                containerColor = EmeraldPrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_recurring_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "تعریف تراکنش دوره‌ای"
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // بنر سهمیه هوشمند (Smart Free Trial) برای کاربران رایگان
            if (!isVip) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("vip_recurring_gate_banner"),
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
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = GoldAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                val remaining = (1 - activeCount).coerceAtLeast(0)
                                Text(
                                    text = if (remaining > 0) "۱ مورد رایگان قابل استفاده" else "۱ مورد رایگان استفاده شده. برای موارد جدید ارتقا دهید",
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
                                modifier = Modifier.testTag("buy_vip_from_bazaar_recurring")
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
            }

            // کارت خلاصه تعهدات دوره‌ای
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = EmeraldDark
                    ),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Repeat,
                                    contentDescription = null,
                                    tint = GoldAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "تراکنش‌های دوره‌ای و اقساط",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = GoldAccent.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "$activeCount فعال",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldAccent,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // تخمین تعهد ماهانه
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "تخمین کل هزینه‌های دوره‌ای در ماه:",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = PersianUtils.formatCurrencyToman(totalActiveMonthlyEstimated),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            // دکمه بررسی و ثبت فوری سررسیدها
                            OutlinedButton(
                                onClick = {
                                    if (!isVip) {
                                        showPaywallDialog = true
                                    } else {
                                        viewModel.triggerProcessRecurringTransactions()
                                        Toast.makeText(
                                            context,
                                            "بررسی و همگام‌سازی تراکنش‌های دوره‌ای انجام شد",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = GoldAccent
                                )
                            ) {
                                Icon(
                                    imageVector = if (isVip) Icons.Default.Sync else Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("بررسی سررسید", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // لیست تراکنش‌های دوره‌ای یا حالت خالی
            if (recurringList.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "هیچ تراکنش دوره‌ای تعریف نشده است",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "می‌توانید پرداخت‌های ماهانه مثل اجاره، اقساط وام، قبض‌ها یا حقوق را اضافه کنید تا در موعد مشخص به صورت خودکار ثبت شوند.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 20.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    if (!isVip && activeCount >= 1) {
                                        showPaywallDialog = true
                                        Toast.makeText(context, "در نسخه رایگان حداکثر ۱ تراکنش دوره‌ای فعال مجاز است.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        editingItem = null
                                        showAddDialog = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("اولین تراکنش دوره‌ای را بسازید")
                            }
                        }
                    }
                }
            } else {
                items(
                    items = recurringList,
                    key = { it.id }
                ) { item ->
                    RecurringTransactionCard(
                        item = item,
                        onToggleActive = {
                            if (!isVip && !item.isActive && activeCount >= 1) {
                                showPaywallDialog = true
                            } else {
                                viewModel.toggleRecurringTransactionActive(item)
                            }
                        },
                        onEdit = {
                            editingItem = item
                            showAddDialog = true
                        },
                        onDelete = {
                            itemToDelete = item
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    // دیالوگ ثبت و ویرایش
    if (showAddDialog && (isVip || activeCount < 1 || editingItem != null)) {
        AddEditRecurringTransactionDialog(
            initialItem = editingItem,
            categories = allCategories,
            onDismiss = {
                showAddDialog = false
                editingItem = null
            },
            onSave = { title, amount, isIncome, category, period, startDate, endDate, note ->
                if (editingItem == null) {
                    viewModel.addRecurringTransaction(
                        title = title,
                        amount = amount,
                        isIncome = isIncome,
                        category = category,
                        period = period,
                        startDate = startDate,
                        endDate = endDate,
                        note = note
                    )
                    Toast.makeText(context, "تراکنش دوره‌ای «$title» ایجاد شد", Toast.LENGTH_SHORT).show()
                } else {
                    val updated = editingItem!!.copy(
                        title = title,
                        amount = amount,
                        isIncome = isIncome,
                        category = category,
                        period = period,
                        note = note
                    )
                    viewModel.updateRecurringTransaction(updated)
                    Toast.makeText(context, "تغییرات ذخیره شد", Toast.LENGTH_SHORT).show()
                }
                showAddDialog = false
                editingItem = null
            }
        )
    }

    // دیالوگ تایید حذف
    itemToDelete?.let { target ->
            AlertDialog(
                onDismissRequest = { itemToDelete = null },
                title = { Text("حذف تراکنش دوره‌ای", fontWeight = FontWeight.Bold) },
                text = {
                    Text("آیا از حذف تراکنش دوره‌ای «${target.title}» اطمینان دارید؟ تراکنش‌های قبلی ثبت شده پاک نخواهند شد.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteRecurringTransaction(target)
                            itemToDelete = null
                            Toast.makeText(context, "تراکنش دوره‌ای حذف شد", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE55656))
                    ) {
                        Text("حذف")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { itemToDelete = null }) {
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
