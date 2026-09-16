package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CategoryHelper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import com.example.billing.BillingManager
import com.example.ui.components.ProPaywallDialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.util.PersianUtils
import com.example.viewmodel.MainViewModel

@Composable
fun ReportsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val transactions by viewModel.transactions.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val budgetLimit by viewModel.budgetLimit.collectAsState()
    val totalSpent by viewModel.totalSpentThisMonth.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    val isProUser by BillingManager.isProState.collectAsState()
    var showBudgetDialog by remember { mutableStateOf(false) }
    var showPaywallDialog by remember { mutableStateOf(false) }

    // Calculate sum of expenses per category
    val expenseTransactions = transactions.filter { !it.isIncome }
    val categoryTotals = remember(expenseTransactions) {
        expenseTransactions
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }

    val totalAmount = categoryTotals.values.sum()

    var selectedSubTab by remember { mutableStateOf(0) } // 0: گزارش و تحلیل, 1: اهداف پس‌انداز, 2: تراکنش‌های دوره‌ای

    Column(modifier = modifier.fillMaxSize()) {
        // Sub-Tab Navigation Bar
        TabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = EmeraldPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedSubTab]),
                    color = EmeraldPrimary,
                    height = 3.dp
                )
            }
        ) {
            Tab(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "تحلیل و بودجه",
                            fontWeight = if (selectedSubTab == 0) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    }
                }
            )

            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "اهداف پس‌انداز",
                            fontWeight = if (selectedSubTab == 1) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    }
                }
            )

            Tab(
                selected = selectedSubTab == 2,
                onClick = { selectedSubTab = 2 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "تراکنش دوره‌ای",
                            fontWeight = if (selectedSubTab == 2) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    }
                }
            )
        }

        when (selectedSubTab) {
            1 -> {
                SavingsGoalsSection(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
            2 -> {
                RecurringTransactionsSection(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .testTag("reports_screen"),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
        // Header & Period Selection
        item {
            Column {
                Text(
                    text = "گزارش و تحلیل هزینه‌ها",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Period Selector
                PeriodSelector(
                    selectedPeriod = selectedPeriod,
                    onPeriodChange = { viewModel.changePeriod(it) }
                )
            }
        }

        if (!isProUser) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "قفل نسخه پرو",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "نمودارها و خروجی PDF ویژه پرو VIP است",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "نمودارهای پیشرفته و گزارش PDF ویژه پرو می‌باشد. (مدیریت سقف بودجه و هشدارها رایگان است)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { showPaywallDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("ارتقا ⭐️", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Donut Chart Card
        item {
            if (totalAmount > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "توزیع سهم هزینه‌ها",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        PieChart(
                            data = categoryTotals.map { (category, amount) ->
                                val meta = CategoryHelper.getCategoryMeta(category)
                                PieSlice(
                                    label = category,
                                    value = amount.toFloat(),
                                    color = meta.color
                                )
                            },
                            modifier = Modifier
                                .size(220.dp)
                                .padding(8.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "مجموع هزینه‌ها: ${PersianUtils.formatCurrencyToman(totalAmount)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = EmeraldPrimary
                        )
                    }
                }
            } else {
                EmptyState("هیچ هزینه‌ای در این بازه زمانی ثبت نشده است")
            }
        }

        // Category Breakdown Legend Items
        if (totalAmount > 0) {
            item {
                Text(
                    text = "جزئیات سهم دسته‌بندی‌ها",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            items(categoryTotals.entries.toList()) { (category, amount) ->
                val meta = CategoryHelper.getCategoryMeta(category)
                LegendItem(
                    category = category,
                    amount = amount,
                    percentage = if (totalAmount > 0) (amount.toFloat() / totalAmount * 100) else 0f,
                    color = meta.color,
                    icon = meta.icon
                )
            }
        }

        // Monthly Budget Warning Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (totalSpent > budgetLimit)
                        Color(0xFFFFEBEE)
                    else
                        Color(0xFFE8F5E9)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "مدیریت سقف بودجه",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TextButton(onClick = {
                            showBudgetDialog = true
                        }) {
                            Text("تنظیم بودجه", color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Progress bar
                    val progress = if (budgetLimit > 0)
                        (totalSpent.toFloat() / budgetLimit).coerceIn(0f, 1f)
                    else 0f

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = when {
                            progress >= 1.0f -> Color(0xFFD32F2F)
                            progress > 0.8f -> Color(0xFFF57C00)
                            else -> Color(0xFF388E3C)
                        },
                        trackColor = Color.White.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "مصرف شده: ${PersianUtils.formatCurrencyToman(totalSpent)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "سقف: ${PersianUtils.formatCurrencyToman(budgetLimit)}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    if (totalSpent > budgetLimit) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "⚠️ هشدار: ${PersianUtils.formatCurrencyToman(totalSpent - budgetLimit)} از سقف تعیین‌شده عبور کرده‌اید!",
                            color = Color(0xFFD32F2F),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // PDF Report Button
        item {
            Button(
                onClick = {
                    if (!isProUser) showPaywallDialog = true else viewModel.generatePdfReport(context)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("📄 اشتراک‌گذاری و دریافت گزارش PDF", fontWeight = FontWeight.Bold)
            }
        }
        
        // CSV Report Button
        item {
            Button(
                onClick = {
                    if (!isProUser) showPaywallDialog = true else viewModel.generateCsvReport(context)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
            ) {
                Text("📊 خروجی فایل اکسل (CSV)", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
        }
    }

        // Budget Configuration Dialog
        if (showBudgetDialog) {
            BudgetDialog(
                currentLimit = budgetLimit,
                onDismiss = { showBudgetDialog = false },
                onSave = { newLimit ->
                    viewModel.setBudgetLimit(newLimit)
                    showBudgetDialog = false
                }
            )
        }

        if (showPaywallDialog) {
            ProPaywallDialog(
                onDismiss = { showPaywallDialog = false },
                onSuccessPurchase = {
                    showPaywallDialog = false
                }
            )
        }
    }
}

@Composable
fun PieChart(data: List<PieSlice>, modifier: Modifier = Modifier) {
    val total = data.sumOf { it.value.toDouble() }
    val donutBackgroundColor = MaterialTheme.colorScheme.surface

    Canvas(modifier = modifier) {
        var startAngle = -90f

        data.forEach { slice ->
            val sweepAngle = if (total > 0) (slice.value / total * 360).toFloat() else 0f

            drawArc(
                color = slice.color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true,
                size = Size(size.width, size.height)
            )

            // Border separator line between pie slices
            drawArc(
                color = Color.White,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true,
                style = Stroke(width = 3f)
            )

            startAngle += sweepAngle
        }

        // Center circle (Donut Chart)
        drawCircle(
            color = donutBackgroundColor,
            radius = size.width / 3.6f
        )
    }
}

data class PieSlice(
    val label: String,
    val value: Float,
    val color: Color
)

@Composable
fun LegendItem(
    category: String,
    amount: Long,
    percentage: Float,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(color),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(color, CircleShape)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = category,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = PersianUtils.formatCurrencyToman(amount),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = PersianUtils.toPersianDigits(String.format("(%.1f%%)", percentage)),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun PeriodSelector(selectedPeriod: String, onPeriodChange: (String) -> Unit) {
    val periods = listOf("هفتگی", "ماهانه", "سه‌ماهه", "سالانه")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        periods.forEach { period ->
            val isSelected = selectedPeriod == period
            FilterChip(
                selected = isSelected,
                onClick = { onPeriodChange(period) },
                label = { Text(period, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = EmeraldPrimary,
                    selectedLabelColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
fun BudgetDialog(
    currentLimit: Long,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit
) {
    var amount by remember { mutableStateOf(currentLimit.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تنظیم سقف بودجه ماهانه", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() } },
                    label = { Text("مبلغ بودجه (تومان)") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                val parsed = amount.toLongOrNull() ?: 0L
                if (parsed > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "معادل: ${PersianUtils.formatCurrencyToman(parsed)}",
                        fontSize = 11.sp,
                        color = EmeraldPrimary
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val budget = amount.toLongOrNull() ?: 0
                    onSave(budget)
                }
            ) {
                Text("ذخیره", fontWeight = FontWeight.Bold, color = EmeraldPrimary)
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
fun EmptyState(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(message, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 13.sp)
        }
    }
}
