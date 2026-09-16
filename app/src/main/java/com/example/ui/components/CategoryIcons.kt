package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.data.CustomCategory

data class CategoryMeta(
    val name: String,
    val icon: ImageVector,
    val color: Color,
    val isIncome: Boolean = false,
    val isDefault: Boolean = false,
    val iconName: String = "",
    val id: Long = 0
)

object CategoryHelper {
    val categories = listOf(
        CategoryMeta("خوراکی", Icons.Default.Restaurant, Color(0xFFFF9800), isIncome = false, isDefault = true, iconName = "restaurant"),
        CategoryMeta("حمل‌ونقل", Icons.Default.DirectionsCar, Color(0xFF2196F3), isIncome = false, isDefault = true, iconName = "car"),
        CategoryMeta("درمان", Icons.Default.LocalHospital, Color(0xFFE91E63), isIncome = false, isDefault = true, iconName = "hospital"),
        CategoryMeta("پوشاک", Icons.Default.ShoppingBag, Color(0xFF9C27B0), isIncome = false, isDefault = true, iconName = "shopping_bag"),
        CategoryMeta("قبض", Icons.Default.Receipt, Color(0xFF00BCD4), isIncome = false, isDefault = true, iconName = "receipt"),
        CategoryMeta("تفریح", Icons.Default.Movie, Color(0xFF673AB7), isIncome = false, isDefault = true, iconName = "movie"),
        CategoryMeta("مسکن", Icons.Default.Home, Color(0xFF795548), isIncome = false, isDefault = true, iconName = "home"),
        CategoryMeta("درآمد", Icons.Default.AccountBalanceWallet, Color(0xFF4CAF50), isIncome = true, isDefault = true, iconName = "wallet"),
        CategoryMeta("متفرقه", Icons.Default.Category, Color(0xFF607D8B), isIncome = false, isDefault = true, iconName = "category")
    )

    val availableIcons: List<Pair<String, ImageVector>> = listOf(
        "restaurant" to Icons.Default.Restaurant,
        "fastfood" to Icons.Default.Fastfood,
        "cafe" to Icons.Default.LocalCafe,
        "shopping_bag" to Icons.Default.ShoppingBag,
        "shopping_cart" to Icons.Default.ShoppingCart,
        "car" to Icons.Default.DirectionsCar,
        "flight" to Icons.Default.Flight,
        "home" to Icons.Default.Home,
        "hospital" to Icons.Default.LocalHospital,
        "fitness" to Icons.Default.FitnessCenter,
        "movie" to Icons.Default.Movie,
        "game" to Icons.Default.SportsEsports,
        "receipt" to Icons.Default.Receipt,
        "phone" to Icons.Default.PhoneAndroid,
        "electric" to Icons.Default.ElectricBolt,
        "school" to Icons.Default.School,
        "work" to Icons.Default.Work,
        "wallet" to Icons.Default.AccountBalanceWallet,
        "paid" to Icons.Default.Paid,
        "bank" to Icons.Default.AccountBalance,
        "savings" to Icons.Default.Savings,
        "gift" to Icons.Default.CardGiftcard,
        "pets" to Icons.Default.Pets,
        "star" to Icons.Default.Star,
        "build" to Icons.Default.Build,
        "category" to Icons.Default.Category
    )

    val availableColors: List<Color> = listOf(
        Color(0xFFFF9800), // نارنجی
        Color(0xFF2196F3), // آبی
        Color(0xFF4CAF50), // سبز زمردی
        Color(0xFFE91E63), // صورتی پررنگ
        Color(0xFF9C27B0), // بنفش
        Color(0xFF00BCD4), // فیروزه‌ای
        Color(0xFFFF5722), // نارنجی تند
        Color(0xFF673AB7), // نیلی
        Color(0xFF795548), // قهوه‌ای
        Color(0xFF009688), // سبز دریایی
        Color(0xFFFFC107), // کهربایی
        Color(0xFFE53935), // سرخ
        Color(0xFF3F51B5), // لاجوردی
        Color(0xFF607D8B)  // خاکستری فولادی
    )

    private val customCategoriesMap = mutableMapOf<String, CategoryMeta>()

    fun updateCustomCategories(list: List<CustomCategory>) {
        synchronized(customCategoriesMap) {
            customCategoriesMap.clear()
            list.forEach { item ->
                customCategoriesMap[item.name] = CategoryMeta(
                    name = item.name,
                    icon = getIconByName(item.iconName),
                    color = parseColor(item.colorHex),
                    isIncome = item.isIncome,
                    isDefault = item.isDefault,
                    iconName = item.iconName,
                    id = item.id
                )
            }
        }
    }

    fun getIconByName(iconName: String): ImageVector {
        return availableIcons.find { it.first == iconName }?.second
            ?: categories.find { it.iconName == iconName }?.icon
            ?: Icons.Default.Category
    }

    fun parseColor(colorHex: String): Color {
        return try {
            val cleanHex = if (colorHex.startsWith("#")) colorHex else "#$colorHex"
            Color(android.graphics.Color.parseColor(cleanHex))
        } catch (e: Exception) {
            Color(0xFF607D8B)
        }
    }

    fun toColorHex(color: Color): String {
        return String.format("#%06X", (0xFFFFFF and color.toArgb()))
    }

    fun getCategoryMeta(name: String): CategoryMeta {
        synchronized(customCategoriesMap) {
            customCategoriesMap[name]?.let { return it }
        }
        categories.find { it.name == name }?.let { return it }
        // تطبیق هوشمند عناوین پیامک بانکی به دسته‌بندی‌های استاندارد برنامه
        when (name.trim()) {
            "خوراک", "رستوران و کافه", "رستوران", "کافه", "فست فود" -> categories.find { it.name == "خوراکی" }?.let { return it }
            "حمل و نقل", "حمل ونقل", "تاکسی", "بنزین" -> categories.find { it.name == "حمل‌ونقل" }?.let { return it }
            "پزشکی و سلامت", "پزشکی", "سلامت", "داروخانه" -> categories.find { it.name == "درمان" }?.let { return it }
            "قبوض و ارتباطات", "قبوض", "ارتباطات", "اینترنت" -> categories.find { it.name == "قبض" }?.let { return it }
            "تفریح و سرگرمی", "گردش" -> categories.find { it.name == "تفریح" }?.let { return it }
            "مسکن و خانه", "خانه", "اجاره" -> categories.find { it.name == "مسکن" }?.let { return it }
            "درآمد و حقوق", "حقوق" -> categories.find { it.name == "درآمد" }?.let { return it }
            "آموزش و کتاب", "خدمات آنلاین", "سایر" -> categories.find { it.name == "متفرقه" }?.let { return it }
        }
        return CategoryMeta(name, Icons.Default.Category, Color(0xFF607D8B))
    }

    fun getCategoryColor(name: String): Color {
        return getCategoryMeta(name).color
    }

    fun getCategoryIcon(name: String): ImageVector {
        return getCategoryMeta(name).icon
    }
}
