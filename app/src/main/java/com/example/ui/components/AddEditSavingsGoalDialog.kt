package com.example.ui.components

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SavingsGoal
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.util.PersianUtils

@Composable
fun AddEditSavingsGoalDialog(
    goal: SavingsGoal? = null,
    onDismiss: () -> Unit,
    onSave: (title: String, targetAmount: Long, initialSaved: Long, note: String, iconName: String, colorHex: String) -> Unit
) {
    var title by remember { mutableStateOf(goal?.title ?: "") }
    var targetAmountText by remember { mutableStateOf(if (goal != null && goal.targetAmount > 0) goal.targetAmount.toString() else "") }
    var initialSavedText by remember { mutableStateOf(if (goal != null && goal.savedAmount > 0) goal.savedAmount.toString() else "") }
    var note by remember { mutableStateOf(goal?.note ?: "") }
    var selectedIcon by remember { mutableStateOf(goal?.iconName ?: "Laptop") }
    var selectedColorHex by remember { mutableStateOf(goal?.colorHex ?: "#0D5C46") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isEditing = goal != null

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = if (isEditing) "ویرایش هدف مالی" else "هدف مالی و پس‌انداز جدید",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Title field
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            errorMessage = null
                        },
                        label = { Text("عنوان هدف (مثلاً خرید لپ‌تاپ، سفر شمال)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("goal_title_input"),
                        singleLine = true
                    )

                    // Target Amount field
                    OutlinedTextField(
                        value = targetAmountText,
                        onValueChange = {
                            if (it.all { char -> char.isDigit() }) {
                                targetAmountText = it
                                errorMessage = null
                            }
                        },
                        label = { Text("مبلغ هدف (تومان)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("goal_target_amount_input"),
                        singleLine = true,
                        supportingText = {
                            val parsed = targetAmountText.toLongOrNull() ?: 0L
                            if (parsed > 0) {
                                Text(
                                    text = PersianUtils.formatCurrencyToman(parsed),
                                    fontSize = 11.sp,
                                    color = EmeraldPrimary
                                )
                            }
                        }
                    )

                    // Initial Saved field
                    OutlinedTextField(
                        value = initialSavedText,
                        onValueChange = {
                            if (it.all { char -> char.isDigit() }) {
                                initialSavedText = it
                            }
                        },
                        label = { Text("مبلغ پس‌انداز شده تا الان (تومان)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("goal_initial_saved_input"),
                        singleLine = true,
                        supportingText = {
                            val parsed = initialSavedText.toLongOrNull() ?: 0L
                            if (parsed > 0) {
                                Text(
                                    text = PersianUtils.formatCurrencyToman(parsed),
                                    fontSize = 11.sp,
                                    color = EmeraldPrimary
                                )
                            }
                        }
                    )

                    // Note field
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("توضیحات یا انگیزه (اختیاری)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )

                    // Icon Selector
                    Text(
                        text = "انتخاب آیکون هدف:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SavingsGoalMetaHelper.icons.forEach { option ->
                            val isSelected = selectedIcon == option.name
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) EmeraldPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, EmeraldPrimary) else null,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clickable { selectedIcon = option.name }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = option.icon,
                                        contentDescription = option.label,
                                        tint = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Color Selector
                    Text(
                        text = "رنگ اختصاصی کارت:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SavingsGoalMetaHelper.colors.forEach { option ->
                            val isSelected = selectedColorHex == option.hex
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(option.color)
                                    .clickable { selectedColorHex = option.hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = targetAmountText.toLongOrNull() ?: 0L
                        val initial = initialSavedText.toLongOrNull() ?: 0L
                        if (title.isBlank()) {
                            errorMessage = "لطفاً عنوان هدف را وارد کنید"
                        } else if (target <= 0) {
                            errorMessage = "مبلغ هدف باید بیشتر از صفر باشد"
                        } else {
                            onSave(title, target, initial, note, selectedIcon, selectedColorHex)
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("save_goal_button")
                ) {
                    Text(if (isEditing) "ذخیره تغییرات" else "ایجاد هدف")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("انصراف")
                }
            }
        )
    }
}
