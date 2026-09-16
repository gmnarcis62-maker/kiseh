package com.example.ui.screens

import android.Manifest
import android.widget.Toast
import com.example.billing.BillingManager
import com.example.ui.components.ProPaywallDialog
import com.example.notification.QuickVoiceNotificationHelper
import com.example.ui.components.DashboardSavingsGoalsPreviewCard
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.outlined.Info
import com.example.ui.components.AppHelpAndGuideDialog
import com.example.ui.components.FreePlanReminderBottomSheet
import com.example.sms.BankSmsPreferencesManager
import com.example.sms.SmsPermissionManager
import com.example.ui.components.AboutUsDialog
import com.example.ui.components.BankSmsOnboardingDialog
import com.example.ui.components.BankSmsSettingsDialog
import com.example.ui.components.CategoriesManagementDialog
import com.example.ui.components.CloudBackupDialog
import com.example.ui.components.FirstLaunchOnboardingDialog
import com.example.ui.components.SecuritySettingsDialog
import com.example.ui.onboarding.OnboardingPreferencesManager
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Transaction
import com.example.data.PendingBankSms
import com.example.ui.components.AddEditTransactionDialog
import com.example.ui.components.CategoryBreakdownCard
import com.example.ui.components.HeaderSummaryCard
import com.example.ui.components.PendingSmsCard
import com.example.ui.components.TransactionItemCard
import com.example.ui.components.VoiceInputSection
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.util.PersianUtils
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.PeriodFilter
import com.example.voice.VoiceInputManager
import kotlinx.coroutines.launch

import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.mutableIntStateOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KisehDashboardScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val allTransactions by viewModel.allTransactions.collectAsState()
    val filteredTransactions by viewModel.filteredTransactions.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategoryFilter.collectAsState()
    val selectedPeriod by viewModel.selectedPeriodFilter.collectAsState()
    val lastAnalysis by viewModel.lastAnalysis.collectAsState()
    val isRial by viewModel.isCurrencyInRial.collectAsState()
    val budgetLimit by viewModel.budgetLimit.collectAsState()
    val savingsGoals by viewModel.allSavingsGoals.collectAsState()
    val pendingSmsList by viewModel.pendingSmsTransactions.collectAsState()

    val voiceManager = remember { VoiceInputManager(context) }
    val voiceState by voiceManager.voiceState.collectAsState()

    val recordAudioLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            voiceManager.startListening()
        } else {
            Toast.makeText(context, "برای ثبت گفتاری، مجوز دسترسی به میکروفون لازم است", Toast.LENGTH_LONG).show()
        }
    }

    val systemVoiceLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                val text = matches[0]
                voiceManager.simulateInput(text)
                viewModel.analyzeVoiceOrTextInput(text)
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(voiceState) {
        if (voiceState is com.example.voice.VoiceState.Success) {
            val text = (voiceState as com.example.voice.VoiceState.Success).text
            if (text.isNotBlank()) {
                val result = viewModel.analyzeVoiceOrTextInput(text)
                if (result.amount > 0) {
                    viewModel.saveAnalysisResult(result)
                    viewModel.clearLastAnalysis()
                    voiceManager.resetState()
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("تراکنش «${result.description}» به مبلغ ${PersianUtils.formatCurrencyToman(result.amount)} با موفقیت ثبت شد")
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            voiceManager.destroy()
        }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        BillingManager.init(context)
    }

    val isProUser by BillingManager.isProState.collectAsState()
    var isQuickVoiceEnabled by remember { mutableStateOf(BillingManager.isQuickVoiceNotificationEnabled(context)) }

    androidx.compose.runtime.LaunchedEffect(isProUser) {
        if (isProUser && isQuickVoiceEnabled) {
            QuickVoiceNotificationHelper.showQuickVoiceNotification(context)
        } else if (!isProUser && isQuickVoiceEnabled) {
            isQuickVoiceEnabled = false
            BillingManager.setQuickVoiceNotificationEnabled(context, false)
            QuickVoiceNotificationHelper.cancelQuickVoiceNotification(context)
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }
    var editingPendingSms by remember { mutableStateOf<PendingBankSms?>(null) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var showPaywallDialog by remember { mutableStateOf(false) }
    var showCategoriesDialog by remember { mutableStateOf(false) }
    var showSecurityDialog by remember { mutableStateOf(false) }
    var showCloudBackupDialog by remember { mutableStateOf(false) }
    var showBankSmsOnboardingDialog by remember { mutableStateOf(false) }
    var showBankSmsSettingsDialog by remember { mutableStateOf(false) }
    var showFirstLaunchDialog by remember { mutableStateOf(false) }
    var showFreePlanReminderSheet by remember { mutableStateOf(false) }
    var showAppHelpDialog by remember { mutableStateOf(false) }

    val onboardingPrefs = remember { OnboardingPreferencesManager(context) }

    LaunchedEffect(isProUser) {
        if (!isProUser && onboardingPrefs.hasSeenFirstLaunch()) {
            // بررسی می‌کنیم که آیا کاربر در ماه جاری قبلاً یادآوری را دیده است یا خیر
            val hasSeenThisMonth = onboardingPrefs.hasSeenFreePlanReminderThisMonth()
            if (!hasSeenThisMonth) {
                showFreePlanReminderSheet = true
                // ثبت نمایش یادآوری برای ماه جاری
                onboardingPrefs.markFreePlanReminderShown()
            } else {
                showFreePlanReminderSheet = false
            }
        } else {
            showFreePlanReminderSheet = false
        }
    }

    LaunchedEffect(Unit) {
        if (!onboardingPrefs.hasSeenFirstLaunch()) {
            showFirstLaunchDialog = true
        }
    }

    val bankSmsPrefs = remember { BankSmsPreferencesManager.getInstance(context) }
    val isBankSmsEnabled by bankSmsPrefs.isBankSmsEnabledFlow.collectAsState(initial = bankSmsPrefs.isBankSmsEnabled())

    val isAppLockEnabled by viewModel.isAppLockEnabled.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()

    val totalExpense = allTransactions.filter { !it.isIncome }.sumOf { it.amount }
    val totalIncome = allTransactions.filter { it.isIncome }.sumOf { it.amount }

    // Enforce RTL Layout Direction for Persian language UI
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = GoldAccent.copy(alpha = 0.2f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "کیسه",
                                        tint = GoldAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "کیسه",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (isProUser) "نسخه پرو VIP ✨" else "مدیریت صوتی و هوشمند هزینه",
                                    fontSize = 11.sp,
                                    color = if (isProUser) GoldAccent else Color.White.copy(alpha = 0.7f),
                                    fontWeight = if (isProUser) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    },
                    actions = {
                        // Pro VIP Badge Button
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isProUser) GoldAccent.copy(alpha = 0.25f) else EmeraldPrimary,
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .clickable { showPaywallDialog = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isProUser) Icons.Default.Verified else Icons.Default.Star,
                                    contentDescription = "کیسه پرو",
                                    tint = GoldAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isProUser) "پرو VIP" else "ارتقای پرو",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                if (!BillingManager.isProUser(context)) {
                                    showPaywallDialog = true
                                    Toast.makeText(context, "دستیار ثبت سریع صوتی فقط در نسخه پرو VIP فعال می‌شود.", Toast.LENGTH_SHORT).show()
                                } else {
                                    val newState = !isQuickVoiceEnabled
                                    BillingManager.setQuickVoiceNotificationEnabled(context, newState)
                                    isQuickVoiceEnabled = newState
                                    if (newState) {
                                        QuickVoiceNotificationHelper.showQuickVoiceNotification(context)
                                        Toast.makeText(context, "اعلان میانبر ثبت سریع صوتی در نوار اعلانات فعال شد 🎙️", Toast.LENGTH_SHORT).show()
                                    } else {
                                        QuickVoiceNotificationHelper.cancelQuickVoiceNotification(context)
                                        Toast.makeText(context, "دستیار ثبت سریع صوتی غیرفعال شد", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.testTag("quick_voice_notification_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "میانبر نوار اعلانات",
                                tint = if (isQuickVoiceEnabled) EmeraldPrimary else GoldAccent
                            )
                        }

                        IconButton(
                            onClick = { showCategoriesDialog = true },
                            modifier = Modifier.testTag("manage_categories_top_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Category,
                                contentDescription = "مدیریت دسته‌بندی‌ها",
                                tint = Color.White
                            )
                        }

                        IconButton(
                            onClick = { showCloudBackupDialog = true },
                            modifier = Modifier.testTag("cloud_backup_top_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = "پشتیبان‌گیری ابری",
                                tint = Color.White
                            )
                        }

                        IconButton(
                            onClick = {
                                if (SmsPermissionManager.hasSmsPermission(context)) {
                                    showBankSmsSettingsDialog = true
                                } else {
                                    showBankSmsOnboardingDialog = true
                                }
                            },
                            modifier = Modifier.testTag("bank_sms_settings_top_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MarkEmailRead,
                                contentDescription = "هوش پیامک بانکی",
                                tint = if (isBankSmsEnabled && SmsPermissionManager.hasSmsPermission(context)) GoldAccent else Color.White
                            )
                        }

                        IconButton(
                            onClick = { showSecurityDialog = true },
                            modifier = Modifier.testTag("security_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "امنیت و قفل برنامه",
                                tint = if (isAppLockEnabled) GoldAccent else Color.White
                            )
                        }

                        IconButton(
                            onClick = { showAboutDialog = true },
                            modifier = Modifier.testTag("about_us_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = "درباره ما",
                                tint = Color.White
                            )
                        }

                        IconButton(
                            onClick = { showAppHelpDialog = true },
                            modifier = Modifier.testTag("app_help_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = "راهنما و امکانات",
                                tint = Color.White
                            )
                        }

                        if (allTransactions.isNotEmpty()) {
                            IconButton(
                                onClick = { showClearDialog = true },
                                modifier = Modifier.testTag("clear_all_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ClearAll,
                                    contentDescription = "پاکسازی",
                                    tint = Color(0xFFFF8A8A)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = EmeraldDark
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Home, contentDescription = "داشبورد") },
                        label = { Text("داشبورد", fontSize = 11.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EmeraldPrimary,
                            selectedTextColor = EmeraldPrimary,
                            indicatorColor = EmeraldPrimary.copy(alpha = 0.15f)
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.PhotoCamera, contentDescription = "اسکن فاکتور") },
                        label = { Text("اسکن فاکتور", fontSize = 11.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EmeraldPrimary,
                            selectedTextColor = EmeraldPrimary,
                            indicatorColor = EmeraldPrimary.copy(alpha = 0.15f)
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.Assessment, contentDescription = "گزارش و بودجه") },
                        label = { Text("گزارش و بودجه", fontSize = 11.sp, fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EmeraldPrimary,
                            selectedTextColor = EmeraldPrimary,
                            indicatorColor = EmeraldPrimary.copy(alpha = 0.15f)
                        )
                    )
                }
            },
            floatingActionButton = {
                if (selectedTab == 0) {
                    FloatingActionButton(
                        onClick = {
                            editingTransaction = null
                            showAddDialog = true
                        },
                        containerColor = EmeraldPrimary,
                        contentColor = Color.White,
                        modifier = Modifier.testTag("add_transaction_fab")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "افزودن تراکنش")
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            when (selectedTab) {
                1 -> {
                    Box(modifier = Modifier.padding(innerPadding)) {
                        ReceiptScannerScreen(
                            viewModel = viewModel,
                            onDismiss = {
                                selectedTab = 0
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("هزینه فاکتور با موفقیت ثبت شد")
                                }
                            }
                        )
                    }
                }
                2 -> {
                    ReportsScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(MaterialTheme.colorScheme.background),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                // Item 1: Summary Banner Card
                item {
                    HeaderSummaryCard(
                        totalExpense = totalExpense,
                        totalIncome = totalIncome,
                        isRial = isRial,
                        onToggleCurrency = { viewModel.toggleCurrencyUnit() },
                        budgetLimit = budgetLimit,
                        onEditBudget = { showBudgetDialog = true }
                    )
                }

                // Item 2: Voice & Smart Input Hero Box
                item {
                    VoiceInputSection(
                        voiceState = voiceState,
                        analysisResult = lastAnalysis,
                        onStartListening = {
                            if (!BillingManager.canUseVoiceInput(context)) {
                                showPaywallDialog = true
                                Toast.makeText(context, "سهمیه ثبت رایگان امروز شما تمام شده است. برای ثبت نامحدود و استفاده از امکانات هوشمند، VIP را فعال کنید.", Toast.LENGTH_LONG).show()
                            } else {
                                if (androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                    voiceManager.startListening()
                                } else {
                                    recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        onStopListening = { voiceManager.stopListening() },
                        onSimulateInput = { text ->
                            voiceManager.simulateInput(text)
                            viewModel.analyzeVoiceOrTextInput(text)
                        },
                        onConfirmResult = { result ->
                            if (!BillingManager.canUseVoiceInput(context)) {
                                showPaywallDialog = true
                                Toast.makeText(context, "سهمیه ثبت رایگان امروز شما تمام شده است. برای ثبت نامحدود و استفاده از امکانات هوشمند، VIP را فعال کنید.", Toast.LENGTH_LONG).show()
                            } else {
                                viewModel.saveAnalysisResult(result)
                                if (!isProUser) {
                                    BillingManager.incrementVoiceUsage(context)
                                }
                                viewModel.clearLastAnalysis()
                                voiceManager.resetState()
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("تراکنش ${result.description} با موفقیت ثبت شد")
                                }
                            }
                        },
                        onDismissResult = {
                            viewModel.clearLastAnalysis()
                            voiceManager.resetState()
                        },
                        onStartSystemVoiceInput = {
                            if (!BillingManager.canUseVoiceInput(context)) {
                                showPaywallDialog = true
                                Toast.makeText(context, "سهمیه ثبت رایگان امروز شما تمام شده است. برای ثبت نامحدود و استفاده از امکانات هوشمند، VIP را فعال کنید.", Toast.LENGTH_LONG).show()
                            } else {
                                systemVoiceLauncher.launch(voiceManager.createSpeechIntent())
                            }
                        }
                    )
                }

                // Item 2.5: Quick Voice Assistant Toggle Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = EmeraldPrimary.copy(alpha = 0.15f),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Mic,
                                            contentDescription = null,
                                            tint = EmeraldPrimary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "دستیار ثبت سریع صوتی",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isProUser) GoldAccent.copy(alpha = 0.25f) else Color.LightGray.copy(alpha = 0.3f)
                                        ) {
                                            Text(
                                                text = if (isProUser) "ویژه پرو ✨" else "نسخه پرو",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = EmeraldPrimary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isQuickVoiceEnabled) "میانبر ثبت صوتی نوار اعلانات فعال است" else "اعلان میانبر همیشگی برای ثبت سریع بدون باز کردن برنامه",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                    )
                                }
                            }

                            Switch(
                                checked = isQuickVoiceEnabled,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        if (!BillingManager.isProUser(context)) {
                                            showPaywallDialog = true
                                            Toast.makeText(context, "دستیار ثبت سریع صوتی فقط در نسخه پرو VIP فعال می‌شود.", Toast.LENGTH_SHORT).show()
                                        } else {
                                            BillingManager.setQuickVoiceNotificationEnabled(context, true)
                                            isQuickVoiceEnabled = true
                                            QuickVoiceNotificationHelper.showQuickVoiceNotification(context)
                                            Toast.makeText(context, "دستیار ثبت سریع صوتی در نوار اعلانات فعال شد 🎙️", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        BillingManager.setQuickVoiceNotificationEnabled(context, false)
                                        isQuickVoiceEnabled = false
                                        QuickVoiceNotificationHelper.cancelQuickVoiceNotification(context)
                                        Toast.makeText(context, "دستیار ثبت سریع صوتی غیرفعال شد", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = EmeraldPrimary
                                )
                            )
                        }
                    }
                }

                // Item 2.7: Pending Bank SMS Queue Card
                if (pendingSmsList.isNotEmpty()) {
                    item {
                        PendingSmsCard(
                            pendingSmsList = pendingSmsList,
                            isRial = isRial,
                            onConfirm = { sms ->
                                viewModel.confirmPendingSms(sms)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("پیامک بانکی با موفقیت به عنوان تراکنش ثبت شد")
                                }
                            },
                            onReject = { sms ->
                                viewModel.rejectPendingSms(sms)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("پیامک بانکی نادیده گرفته شد")
                                }
                            },
                            onEdit = { sms ->
                                editingPendingSms = sms
                            },
                            onOpenSettings = {
                                showBankSmsSettingsDialog = true
                            }
                        )
                    }
                }

                // Item 2.8: Savings Goals Preview Widget
                item {
                    DashboardSavingsGoalsPreviewCard(
                        goals = savingsGoals,
                        onNavigateToGoals = {
                            selectedTab = 2
                        }
                    )
                }

                // Item 3: Category Spending Distribution Chart
                item {
                    CategoryBreakdownCard(
                        transactions = allTransactions,
                        selectedCategory = selectedCategory,
                        onSelectCategory = { cat -> viewModel.setCategoryFilter(cat) }
                    )
                }

                // Item 4: Search & Period Filter Header
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "لیست تراکنش‌ها",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Search Field
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("جستجو در توضیحات یا مبلغ...", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "جستجو",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_input_field"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Period Filter Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PeriodFilter.entries.forEach { period ->
                                val isSelected = selectedPeriod == period
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setPeriodFilter(period) },
                                    label = { Text(period.title, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = EmeraldPrimary,
                                        selectedLabelColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                )
                            }
                        }
                    }
                }

                // Items list or Empty state
                if (filteredTransactions.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = "کیسه خالی است",
                                    tint = EmeraldPrimary.copy(alpha = 0.4f),
                                    modifier = Modifier.size(56.dp)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "هنوز تراکنشی ثبت نشده است",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "دکمه میکروفون را بزنید و بگویید: «۲۰ تومن نون خریدم» یا از دکمه + استفاده کنید.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(
                        items = filteredTransactions,
                        key = { it.id }
                    ) { transaction ->
                        TransactionItemCard(
                            transaction = transaction,
                            isRial = isRial,
                            onEdit = { tx ->
                                editingTransaction = tx
                                showAddDialog = true
                            },
                            onDelete = { tx ->
                                viewModel.deleteTransaction(tx)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("تراکنش حذف شد")
                                }
                            }
                        )
                    }
                }
            }
        }
    }

            // Dialog for Manual Add or Edit
            if (showAddDialog) {
                AddEditTransactionDialog(
                    initialTransaction = editingTransaction,
                    categories = allCategories,
                    onDismiss = { showAddDialog = false },
                    onSave = { amount, category, description, isIncome ->
                        if (editingTransaction != null) {
                            viewModel.updateTransaction(
                                editingTransaction!!.copy(
                                    amount = amount,
                                    category = category,
                                    description = description,
                                    isIncome = isIncome
                                )
                            )
                        } else {
                            viewModel.addTransaction(amount, category, description, isIncome)
                        }
                        showAddDialog = false
                    },
                    onAddNewCategory = { name, iconName, colorHex, isInc, onDone ->
                        viewModel.addCustomCategory(
                            name = name,
                            iconName = iconName,
                            colorHex = colorHex,
                            isIncome = isInc,
                            onSuccess = {
                                Toast.makeText(context, "دسته‌بندی «$name» ایجاد شد", Toast.LENGTH_SHORT).show()
                                onDone(name)
                            },
                            onError = { err ->
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                )
            }

            // Dialog for Editing and Confirming Pending Bank SMS
            if (editingPendingSms != null) {
                val currentSms = editingPendingSms!!
                val initialDescription = buildString {
                    append(currentSms.bankName)
                    if (currentSms.merchant.isNotBlank()) {
                        append(" - ")
                        append(currentSms.merchant)
                    }
                    if (!currentSms.cardLastDigits.isNullOrBlank()) {
                        append(" (کارت *")
                        append(currentSms.cardLastDigits)
                        append(")")
                    }
                }
                val mockTx = remember(currentSms) {
                    Transaction(
                        amount = currentSms.amount,
                        category = currentSms.suggestedCategory,
                        description = initialDescription,
                        isIncome = currentSms.isIncome,
                        date = currentSms.createdAt
                    )
                }

                AddEditTransactionDialog(
                    initialTransaction = mockTx,
                    categories = allCategories,
                    onDismiss = { editingPendingSms = null },
                    onSave = { amount, category, description, isIncome ->
                        viewModel.editAndConfirmPendingSms(
                            pendingSms = currentSms,
                            amount = amount,
                            category = category,
                            description = description,
                            isIncome = isIncome
                        )
                        editingPendingSms = null
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("پیامک بانکی پس از ویرایش ثبت شد")
                        }
                    },
                    onAddNewCategory = { name, iconName, colorHex, isInc, onDone ->
                        viewModel.addCustomCategory(
                            name = name,
                            iconName = iconName,
                            colorHex = colorHex,
                            isIncome = isInc,
                            onSuccess = {
                                Toast.makeText(context, "دسته‌بندی «$name» ایجاد شد", Toast.LENGTH_SHORT).show()
                                onDone(name)
                            },
                            onError = { err ->
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                )
            }

            // Dialog for Custom Categories Management
            if (showCategoriesDialog) {
                CategoriesManagementDialog(
                    viewModel = viewModel,
                    onDismiss = { showCategoriesDialog = false }
                )
            }

            // Dialog for First Launch Onboarding
            if (showFirstLaunchDialog) {
                FirstLaunchOnboardingDialog(
                    onDismiss = {
                        showFirstLaunchDialog = false
                        coroutineScope.launch {
                            onboardingPrefs.setHasSeenFirstLaunch(true)
                        }
                    },
                    onStart = {
                        showFirstLaunchDialog = false
                        coroutineScope.launch {
                            onboardingPrefs.setHasSeenFirstLaunch(true)
                        }
                    }
                )
            }

            // Dialog for Security & App Lock Settings
            if (showSecurityDialog) {
                SecuritySettingsDialog(
                    viewModel = viewModel,
                    onDismiss = { showSecurityDialog = false }
                )
            }

            // Dialog for Cloud Backup & Restore
            if (showCloudBackupDialog) {
                CloudBackupDialog(
                    viewModel = viewModel,
                    onDismiss = { showCloudBackupDialog = false }
                )
            }

            // Dialog for Bank SMS Onboarding & Permission Explanation
            if (showBankSmsOnboardingDialog) {
                BankSmsOnboardingDialog(
                    onDismiss = { showBankSmsOnboardingDialog = false },
                    onActivate = {
                        showBankSmsOnboardingDialog = false
                        showBankSmsSettingsDialog = true
                    }
                )
            }

            // Dialog for Bank SMS Settings & Control Panel
            if (showBankSmsSettingsDialog) {
                BankSmsSettingsDialog(
                    viewModel = viewModel,
                    onDismiss = { showBankSmsSettingsDialog = false },
                    onShowOnboarding = {
                        showBankSmsSettingsDialog = false
                        showBankSmsOnboardingDialog = true
                    },
                    onUpgradeToPro = {
                        showBankSmsSettingsDialog = false
                        showPaywallDialog = true
                    }
                )
            }

            // Set Monthly Budget Dialog
            if (showBudgetDialog) {
                var budgetInput by remember { mutableStateOf(if (budgetLimit > 0) budgetLimit.toString() else "") }
                AlertDialog(
                    onDismissRequest = { showBudgetDialog = false },
                    title = { Text("تعیین سقف بودجه ماهانه", fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            Text("مبلغ سقف بودجه ماهانه خود را به تومان وارد کنید:", fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = budgetInput,
                                onValueChange = { budgetInput = it.filter { char -> char.isDigit() } },
                                label = { Text("سقف بودجه (تومان)") },
                                placeholder = { Text("مثلاً: ۱۰,۰۰۰,۰۰۰") },
                                singleLine = true,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (budgetInput.isNotBlank()) {
                                val amount = budgetInput.toLongOrNull() ?: 0L
                                Text(
                                    text = "معادل: ${PersianUtils.formatCurrencyToman(amount)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary,
                                    modifier = Modifier.padding(top = 6.dp, start = 4.dp)
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val amount = budgetInput.toLongOrNull() ?: 0L
                                viewModel.setBudgetLimit(amount)
                                showBudgetDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("ثبت و ذخیره")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showBudgetDialog = false }) {
                            Text("انصراف")
                        }
                    }
                )
            }

            // About Us Dialog
            if (showAboutDialog) {
                AboutUsDialog(
                    onDismiss = { showAboutDialog = false }
                )
            }

            // App Help & Guide Dialog
            if (showAppHelpDialog) {
                AppHelpAndGuideDialog(
                    onDismiss = { showAppHelpDialog = false },
                    onUpgradeToVip = {
                        showAppHelpDialog = false
                        showPaywallDialog = true
                    }
                )
            }

            // Free Plan Reminder Bottom Sheet (Only for Free users)
            if (showFreePlanReminderSheet && !isProUser) {
                FreePlanReminderBottomSheet(
                    onDismiss = { showFreePlanReminderSheet = false },
                    onActivateVip = {
                        showFreePlanReminderSheet = false
                        showPaywallDialog = true
                    }
                )
            }

            // Pro Paywall Dialog
            if (showPaywallDialog) {
                ProPaywallDialog(
                    onDismiss = { showPaywallDialog = false },
                    onSuccessPurchase = {
                        showPaywallDialog = false
                    }
                )
            }

            // Confirm Clear All Dialog
            if (showClearDialog) {
                AlertDialog(
                    onDismissRequest = { showClearDialog = false },
                    title = { Text("پاکسازی کیسه") },
                    text = { Text("آیا از حذف تمام تراکنش‌های ثبت شده مطمئن هستید؟") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.clearAllTransactions()
                                showClearDialog = false
                            }
                        ) {
                            Text("بله، پاک شود", color = Color(0xFFE55656), fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showClearDialog = false }) {
                            Text("انصراف")
                        }
                    }
                )
            }
        }
    }
}
