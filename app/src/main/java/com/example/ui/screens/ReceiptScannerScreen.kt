package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.analyzer.ReceiptOCR
import com.example.analyzer.ReceiptResult
import com.example.billing.BillingManager
import com.example.billing.FeatureUsageManager
import com.example.ui.components.ProPaywallDialog
import com.example.ui.theme.EmeraldPrimary
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stars
import com.example.ui.util.PersianUtils
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ReceiptScannerScreen(
    viewModel: MainViewModel,
    onDismiss: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedImage by remember { mutableStateOf<Bitmap?>(null) }
    var analysisResult by remember { mutableStateOf<ReceiptResult?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    var editableAmountText by remember { mutableStateOf("") }
    var editableDescription by remember { mutableStateOf("خرید اسکن شده از فاکتور") }
    var selectedCategory by remember { mutableStateOf("خوراکی") }
    val isProUser by BillingManager.isProState.collectAsState()
    var showPaywallDialog by remember { mutableStateOf(false) }

    fun processBitmap(bitmap: Bitmap) {
        if (!FeatureUsageManager.canScanInvoice(context)) {
            showPaywallDialog = true
            Toast.makeText(context, "سقف ۳ اسکن رایگان فاکتور پایان یافته است. برای اسکن نامحدود، نسخه VIP را فعال کنید.", Toast.LENGTH_SHORT).show()
            return
        }
        selectedImage = bitmap
        isLoading = true
        scope.launch {
            if (!BillingManager.isProUser(context)) {
                FeatureUsageManager.incrementInvoiceScanUsage(context)
            }
            val ocr = ReceiptOCR(context)
            val result = withContext(Dispatchers.IO) {
                ocr.analyzeReceipt(bitmap)
            }
            analysisResult = result
            isLoading = false
            editableAmountText = if (result.totalAmount > 0) result.totalAmount.toString() else ""
            selectedCategory = result.category
            ocr.close()
        }
    }

    // ۱۰. اسکن مستقیم از دوربین
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let { processBitmap(it) }
    }

    // ۲۰. انتخاب عکس از گالری
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                bitmap?.let { b -> processBitmap(b) }
            } catch (_: Exception) {
                isLoading = false
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "اسکن و تحلیل هوشمند فاکتور",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "از فاکتور خرید عکس بگیرید یا عکس آن را از گالری انتخاب کنید تا مبلغ صادر شده را هوشمند استخراج کنیم.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            if (!isProUser) {
                val remainingScans = FeatureUsageManager.getInvoiceScanRemaining(context)
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Stars, contentDescription = null, tint = EmeraldPrimary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (remainingScans > 0) "${PersianUtils.toPersianDigits(remainingScans.toString())} از ۳ اسکن رایگان باقی مانده"
                                else "۳ از ۳ اسکن رایگان استفاده شده",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                if (remainingScans > 0) "می‌توانید فاکتور واقعی خود را اسکن کنید تا دقت OCR را بسنجید."
                                else "برای اسکن نامحدود فاکتورها به نسخه VIP ارتقا دهید.",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { showPaywallDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("خرید VIP از کافهبازار", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // دکمه اسکن مستقیم دوربین
                Button(
                    onClick = {
                        if (!FeatureUsageManager.canScanInvoice(context)) {
                            showPaywallDialog = true
                            Toast.makeText(context, "سقف ۳ اسکن رایگان فاکتور پایان یافته است. برای اسکن نامحدود، نسخه VIP را فعال کنید.", Toast.LENGTH_SHORT).show()
                        } else {
                            cameraLauncher.launch(null)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "دوربین")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("عکس با دوربین", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                // دکمه انتخاب از گالری
                Button(
                    onClick = {
                        if (!FeatureUsageManager.canScanInvoice(context)) {
                            showPaywallDialog = true
                            Toast.makeText(context, "سقف ۳ اسکن رایگان فاکتور پایان یافته است. برای اسکن نامحدود، نسخه VIP را فعال کنید.", Toast.LENGTH_SHORT).show()
                        } else {
                            imagePicker.launch("image/*")
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = "گالری")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("انتخاب از گالری", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // نمایش تصویر انتخاب شده
        selectedImage?.let { bitmap ->
            item {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "عکس فاکتور",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
            }
        }

        // نمایش حالت بارگذاری
        if (isLoading) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = EmeraldPrimary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "در حال استخراج متن و مبالغ فاکتور...",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // نمایش نتیجه با امکان ویرایش سریع
        analysisResult?.let { result ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "نتیجه استخراج فاکتور:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // فیلد مبلغ استخراج شده با قابلیت اصلاح
                        OutlinedTextField(
                            value = editableAmountText,
                            onValueChange = { editableAmountText = it },
                            label = { Text("مبلغ استخراج‌شده (تومان)") },
                            placeholder = { Text("مثلاً: ۵۰۰۰۰") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldPrimary
                            )
                        )

                        if (editableAmountText.isNotBlank()) {
                            val parsedAmount = editableAmountText.toLongOrNull() ?: 0L
                            Text(
                                text = "معادل: ${PersianUtils.formatCurrencyToman(parsedAmount)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary,
                                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // شرح فاکتور
                        OutlinedTextField(
                            value = editableDescription,
                            onValueChange = { editableDescription = it },
                            label = { Text("توضیح خرید / فاکتور") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (result.items.isNotEmpty()) {
                            Text(
                                text = "تمام مبالغ کشف‌شده در عکس:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                result.items.take(4).forEach { amount ->
                                    Button(
                                        onClick = { editableAmountText = amount.toString() },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = EmeraldPrimary.copy(alpha = 0.15f),
                                            contentColor = EmeraldPrimary
                                        )
                                    ) {
                                        Text(PersianUtils.formatCurrencyToman(amount), fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val finalAmount = editableAmountText.toLongOrNull() ?: 0L

                        Button(
                            onClick = {
                                if (finalAmount > 0) {
                                    viewModel.addTransaction(
                                        amount = finalAmount,
                                        category = selectedCategory,
                                        description = editableDescription.ifBlank { "خرید از فاکتور" },
                                        isIncome = false
                                    )
                                    onDismiss()
                                }
                            },
                            enabled = finalAmount > 0,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Text("✅ ثبت این هزینه در کیسه", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
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