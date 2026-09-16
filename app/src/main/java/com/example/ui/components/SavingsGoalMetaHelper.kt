package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class GoalIconOption(
    val name: String,
    val icon: ImageVector,
    val label: String
)

data class GoalColorOption(
    val hex: String,
    val color: Color
)

object SavingsGoalMetaHelper {
    val icons = listOf(
        GoalIconOption("Laptop", Icons.Default.Laptop, "لپ‌تاپ/کالای دیجیتال"),
        GoalIconOption("PhoneAndroid", Icons.Default.PhoneAndroid, "گوشی موبایل"),
        GoalIconOption("DirectionsCar", Icons.Default.DirectionsCar, "خودرو/موتور"),
        GoalIconOption("Home", Icons.Default.Home, "مسکن/لوازم خانه"),
        GoalIconOption("Flight", Icons.Default.Flight, "سفر/گردشگری"),
        GoalIconOption("Savings", Icons.Default.Savings, "صندوق پس‌انداز"),
        GoalIconOption("CardGiftcard", Icons.Default.CardGiftcard, "هدیه/مناسبت"),
        GoalIconOption("School", Icons.Default.School, "آموزش/دوره"),
        GoalIconOption("Star", Icons.Default.Star, "سایر اهداف")
    )

    val colors = listOf(
        GoalColorOption("#0D5C46", Color(0xFF0D5C46)), // زمردی کیسه
        GoalColorOption("#D4AF37", Color(0xFFD4AF37)), // طلایی
        GoalColorOption("#2196F3", Color(0xFF2196F3)), // آبی
        GoalColorOption("#9C27B0", Color(0xFF9C27B0)), // بنفش
        GoalColorOption("#E91E63", Color(0xFFE91E63)), // صورتی
        GoalColorOption("#FF9800", Color(0xFFFF9800)), // نارنجی
        GoalColorOption("#00897B", Color(0xFF00897B)), // یشمی
        GoalColorOption("#3949AB", Color(0xFF3949AB))  // سرمه‌ای
    )

    fun getIcon(name: String): ImageVector {
        return icons.find { it.name == name }?.icon ?: Icons.Default.Savings
    }

    fun parseColor(hex: String): Color {
        return try {
            val cleanHex = hex.removePrefix("#")
            Color(android.graphics.Color.parseColor("#$cleanHex"))
        } catch (e: Exception) {
            Color(0xFF0D5C46)
        }
    }
}
