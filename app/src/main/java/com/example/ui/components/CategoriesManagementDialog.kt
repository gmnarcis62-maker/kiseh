package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.billing.BillingManager
import com.example.billing.FeatureUsageManager
import com.example.data.CustomCategory
import com.example.ui.components.ProPaywallDialog
import com.example.ui.theme.EmeraldPrimary
import com.example.viewmodel.MainViewModel

@Composable
fun CategoriesManagementDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isVip by BillingManager.isVipState.collectAsState()
    val allCustomCategories by viewModel.allCustomCategories.collectAsState()
    val allMergedCategories by viewModel.allCategories.collectAsState()

    var showPaywallDialog by remember { mutableStateOf(false) }
    var selectedTabIsIncome by remember { mutableStateOf(false) } // false = expense, true = income
    var showAddEditDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<CustomCategory?>(null) }
    var categoryToDelete by remember { mutableStateOf<CustomCategory?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .fillMaxHeight(0.86f)
                    .padding(vertical = 16.dp)
                    .testTag("categories_management_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "مدیریت دسته‌بندی‌ها",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "ایجاد، ویرایش و رنگ‌بندی اختصاصی دسته‌ها",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_categories_management_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "بستن",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Type Switch (Expense / Income)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !selectedTabIsIncome,
                            onClick = { selectedTabIsIncome = false },
                            label = { Text("دسته‌های هزینه", fontWeight = FontWeight.Bold) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFF5252).copy(alpha = 0.15f),
                                selectedLabelColor = Color(0xFFD32F2F),
                                selectedLeadingIconColor = Color(0xFFD32F2F)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("filter_chip_expense_categories")
                        )

                        FilterChip(
                            selected = selectedTabIsIncome,
                            onClick = { selectedTabIsIncome = true },
                            label = { Text("دسته‌های درآمد", fontWeight = FontWeight.Bold) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary.copy(alpha = 0.15f),
                                selectedLabelColor = EmeraldPrimary,
                                selectedLeadingIconColor = EmeraldPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("filter_chip_income_categories")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Categories List
                    val filteredCategories = allMergedCategories.filter { it.isIncome == selectedTabIsIncome }

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 12.dp)
                    ) {
                        items(filteredCategories, key = { "${it.name}_${it.id}_${it.isDefault}" }) { catMeta ->
                            val customEntity = allCustomCategories.find { it.name == catMeta.name }
                            val isCustom = !catMeta.isDefault && customEntity != null

                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("category_item_${catMeta.name}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(catMeta.color),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = catMeta.icon,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = catMeta.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                if (catMeta.isDefault) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(Color(0xFF607D8B).copy(alpha = 0.15f))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(
                                                                imageVector = Icons.Default.Lock,
                                                                contentDescription = null,
                                                                tint = Color(0xFF607D8B),
                                                                modifier = Modifier.size(10.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(3.dp))
                                                            Text(
                                                                text = "سیستمی",
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                color = Color(0xFF607D8B)
                                                            )
                                                        }
                                                    }
                                                } else {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(catMeta.color.copy(alpha = 0.15f))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = "سفارشی",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = catMeta.color
                                                        )
                                                    }
                                                }
                                            }

                                            Text(
                                                text = if (catMeta.isIncome) "درآمد" else "هزینه",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    // Action buttons for custom categories
                                    if (isCustom && customEntity != null) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = {
                                                    categoryToEdit = customEntity
                                                    showAddEditDialog = true
                                                },
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .testTag("edit_category_${customEntity.name}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "ویرایش",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            IconButton(
                                                onClick = { categoryToDelete = customEntity },
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .testTag("delete_category_${customEntity.name}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "حذف",
                                                    tint = Color(0xFFFF5252),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Add Category Floating Action Button
                    Button(
                        onClick = {
                            if (!isVip && allCustomCategories.size >= FeatureUsageManager.MAX_FREE_CUSTOM_CATEGORIES) {
                                showPaywallDialog = true
                                Toast.makeText(context, "در نسخه رایگان حداکثر امکان ساخت ۲ دسته‌بندی اختصاصی وجود دارد. برای دسته‌بندی‌های نامحدود، نسخه VIP را فعال نمایید.", Toast.LENGTH_SHORT).show()
                            } else {
                                categoryToEdit = null
                                showAddEditDialog = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("add_new_category_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ایجاد دسته‌بندی جدید",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Dialog
    if (showAddEditDialog) {
        AddEditCategoryDialog(
            initialCategory = categoryToEdit,
            onDismiss = {
                showAddEditDialog = false
                categoryToEdit = null
            },
            onConfirm = { name, iconName, colorHex, isIncome ->
                if (categoryToEdit != null) {
                    val updated = categoryToEdit!!.copy(
                        name = name,
                        iconName = iconName,
                        colorHex = colorHex,
                        isIncome = isIncome
                    )
                    viewModel.updateCustomCategory(
                        category = updated,
                        oldName = categoryToEdit!!.name,
                        onSuccess = {
                            Toast.makeText(context, "دسته‌بندی با موفقیت به‌روزرسانی شد", Toast.LENGTH_SHORT).show()
                            showAddEditDialog = false
                            categoryToEdit = null
                        },
                        onError = { err ->
                            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                        }
                    )
                } else {
                    viewModel.addCustomCategory(
                        name = name,
                        iconName = iconName,
                        colorHex = colorHex,
                        isIncome = isIncome,
                        onSuccess = {
                            Toast.makeText(context, "دسته‌بندی جدید با موفقیت اضافه شد", Toast.LENGTH_SHORT).show()
                            showAddEditDialog = false
                        },
                        onError = { err ->
                            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                        }
                    )
                }
            }
        )
    }

    // Confirm Delete Dialog
    if (categoryToDelete != null) {
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = {
                Text(
                    text = "حذف دسته‌بندی",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Text(
                    text = "آیا از حذف دسته‌بندی «${categoryToDelete!!.name}» اطمینان دارید؟\n\nنکته: تراکنش‌های قبلی ثبت‌شده با این دسته بدون تغییر باقی خواهند ماند.",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toDelete = categoryToDelete!!
                        viewModel.deleteCustomCategory(
                            category = toDelete,
                            onSuccess = {
                                Toast.makeText(context, "دسته‌بندی با موفقیت حذف شد", Toast.LENGTH_SHORT).show()
                                categoryToDelete = null
                            },
                            onError = { err ->
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                categoryToDelete = null
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                ) {
                    Text("حذف دسته", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("انصراف")
                }
            }
        )
    }

    if (showPaywallDialog) {
        ProPaywallDialog(onDismiss = { showPaywallDialog = false })
    }
}
