package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.billing.BillingManager
import com.example.billing.FeatureUsageManager
import com.example.analyzer.AnalysisResult
import com.example.analyzer.ExpenseAnalyzer
import com.example.data.AppDatabase
import com.example.data.Transaction
import com.example.data.TransactionRepository
import com.example.notification.BudgetAlertWorker
import com.example.report.PdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import org.json.JSONArray
import org.json.JSONObject
import com.example.ui.util.PersianUtils

import com.example.data.SavingsGoal
import com.example.data.SavingsGoalRepository
import com.example.data.PendingBankSms
import com.example.data.PendingBankSmsRepository
import com.example.backup.BackupCryptoManager
import com.example.backup.CloudBackupState
import com.example.backup.KisehBackupPayload
import com.example.backup.AppPreferencesBackup
import com.example.data.RecurringTransaction
import com.example.data.RecurringTransactionRepository
import com.example.data.CustomCategory
import com.example.data.CategoryRepository
import com.example.ui.components.CategoryHelper
import com.example.ui.components.CategoryMeta
import com.example.notification.RecurringTransactionWorker
import com.example.security.BiometricAuthManager
import com.example.security.SecurityPreferencesManager
import com.example.sms.SmartCategoryMatcher
import com.example.sms.UserCategoryLearner
import kotlinx.coroutines.flow.first

enum class PeriodFilter(val title: String) {
    ALL("همه"),
    TODAY("امروز"),
    THIS_WEEK("این هفته"),
    THIS_MONTH("این ماه")
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database: AppDatabase
    private val repository: TransactionRepository
    private val savingsGoalRepository: SavingsGoalRepository
    private val recurringTransactionRepository: RecurringTransactionRepository
    private val categoryRepository: CategoryRepository
    private val pendingBankSmsRepository: PendingBankSmsRepository
    private val securityManager: SecurityPreferencesManager
    private val userCategoryLearner: UserCategoryLearner = UserCategoryLearner.getInstance(application)
    private val analyzer = ExpenseAnalyzer()

    // --- Local Backup State ---
    private val _backupState = MutableStateFlow<CloudBackupState>(CloudBackupState.Idle)
    val cloudBackupState: StateFlow<CloudBackupState> = _backupState.asStateFlow()

    // --- Security & App Lock State ---
    val isAppLockEnabled: StateFlow<Boolean>
    val isLockOnLaunchEnabled: StateFlow<Boolean>
    val isBiometricEnabled: StateFlow<Boolean>
    val pinLength: StateFlow<Int>
    val hasPinSet: StateFlow<Boolean>

    private val _isAppCurrentlyLocked = MutableStateFlow(false)
    val isAppCurrentlyLocked: StateFlow<Boolean> = _isAppCurrentlyLocked.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    val selectedCategoryFilter: StateFlow<String?> = _selectedCategoryFilter.asStateFlow()

    private val _selectedPeriodFilter = MutableStateFlow(PeriodFilter.ALL)
    val selectedPeriodFilter: StateFlow<PeriodFilter> = _selectedPeriodFilter.asStateFlow()

    private val _lastAnalysis = MutableStateFlow<AnalysisResult?>(null)
    val lastAnalysis: StateFlow<AnalysisResult?> = _lastAnalysis.asStateFlow()

    private val _isCurrencyInRial = MutableStateFlow(false)
    val isCurrencyInRial: StateFlow<Boolean> = _isCurrencyInRial.asStateFlow()

    private val _budgetLimit = MutableStateFlow(5000000L)
    val budgetLimit: StateFlow<Long> = _budgetLimit.asStateFlow()

    val isProUser: StateFlow<Boolean> = BillingManager.isProState
    val vipPrice: StateFlow<String?> = BillingManager.skuPriceState

    private val _selectedPeriod = MutableStateFlow("ماهانه")
    val selectedPeriod: StateFlow<String> = _selectedPeriod.asStateFlow()

    init {
        database = AppDatabase.getDatabase(application)
        val dao = database.transactionDao()
        val goalsDao = database.savingsGoalDao()
        val recurringDao = database.recurringTransactionDao()
        val customCatDao = database.customCategoryDao()
        val pendingSmsDao = database.pendingBankSmsDao()
        repository = TransactionRepository(dao)
        savingsGoalRepository = SavingsGoalRepository(goalsDao)
        recurringTransactionRepository = RecurringTransactionRepository(recurringDao)
        categoryRepository = CategoryRepository(customCatDao)
        pendingBankSmsRepository = PendingBankSmsRepository(pendingSmsDao)
        securityManager = SecurityPreferencesManager(application)

        isAppLockEnabled = securityManager.isAppLockEnabled
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)
        isLockOnLaunchEnabled = securityManager.isLockOnLaunchEnabled
            .stateIn(viewModelScope, SharingStarted.Eagerly, true)
        isBiometricEnabled = securityManager.isBiometricEnabled
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)
        pinLength = securityManager.pinLength
            .stateIn(viewModelScope, SharingStarted.Eagerly, 4)
        hasPinSet = securityManager.hasPinSet
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

        viewModelScope.launch {
            val isEnabled = securityManager.isAppLockEnabled.first()
            val lockOnLaunch = securityManager.isLockOnLaunchEnabled.first()
            val hasPin = securityManager.hasPinSet.first()
            if (isEnabled && lockOnLaunch && hasPin) {
                _isAppCurrentlyLocked.value = true
            }
        }

        val prefs = application.getSharedPreferences("kiseh_prefs", Context.MODE_PRIVATE)
        val savedLimit = prefs.getLong("budget_limit", 5000000L)
        _budgetLimit.value = savedLimit

        scheduleBudgetCheck()
        scheduleRecurringTransactionsCheck()

        viewModelScope.launch {
            categoryRepository.allCustomCategories.collect { list ->
                CategoryHelper.updateCustomCategories(list)
            }
        }
    }

    val allRecurringTransactions: StateFlow<List<RecurringTransaction>> = recurringTransactionRepository.allRecurringTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allSavingsGoals: StateFlow<List<SavingsGoal>> = savingsGoalRepository.allGoals
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allTransactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val pendingSmsTransactions: StateFlow<List<PendingBankSms>> = combine(
        pendingBankSmsRepository.allPendingSms,
        BillingManager.isProState
    ) { list, isPro ->
        if (isPro || FeatureUsageManager.canProcessBankSms(getApplication())) list else emptyList()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allCustomCategories: StateFlow<List<CustomCategory>> = categoryRepository.allCustomCategories
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allCategories: StateFlow<List<CategoryMeta>> = allCustomCategories
        .combine(MutableStateFlow(CategoryHelper.categories)) { customList, defaultList ->
            val customMetas = customList.map { item ->
                CategoryMeta(
                    name = item.name,
                    icon = CategoryHelper.getIconByName(item.iconName),
                    color = CategoryHelper.parseColor(item.colorHex),
                    isIncome = item.isIncome,
                    isDefault = item.isDefault,
                    iconName = item.iconName,
                    id = item.id
                )
            }
            val customNames = customMetas.map { it.name }.toSet()
            customMetas + defaultList.filter { it.name !in customNames }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CategoryHelper.categories
        )

    val totalSpentThisMonth: StateFlow<Long> = allTransactions
        .combine(_selectedPeriod) { txs, period ->
            val now = System.currentTimeMillis()
            val periodMs = when (period) {
                "هفتگی" -> 7L * 24 * 60 * 60 * 1000
                "ماهانه" -> 30L * 24 * 60 * 60 * 1000
                "سه‌ماهه" -> 90L * 24 * 60 * 60 * 1000
                "سالانه" -> 365L * 24 * 60 * 60 * 1000
                else -> 30L * 24 * 60 * 60 * 1000
            }
            txs.filter { !it.isIncome && (now - it.date) <= periodMs }.sumOf { it.amount }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0L
        )

    fun scheduleBudgetCheck() {
        try {
            val workRequest = PeriodicWorkRequestBuilder<BudgetAlertWorker>(
                6, TimeUnit.HOURS
            ).build()

            WorkManager.getInstance(getApplication())
                .enqueueUniquePeriodicWork(
                    "budget_check",
                    ExistingPeriodicWorkPolicy.KEEP,
                    workRequest
                )

            val oneTimeWork = OneTimeWorkRequestBuilder<BudgetAlertWorker>().build()
            WorkManager.getInstance(getApplication()).enqueue(oneTimeWork)
        } catch (_: Exception) {}
    }

    fun scheduleRecurringTransactionsCheck() {
        try {
            val workRequest = PeriodicWorkRequestBuilder<RecurringTransactionWorker>(
                4, TimeUnit.HOURS
            ).build()

            WorkManager.getInstance(getApplication())
                .enqueueUniquePeriodicWork(
                    "recurring_tx_check",
                    ExistingPeriodicWorkPolicy.KEEP,
                    workRequest
                )

            val immediateWork = OneTimeWorkRequestBuilder<RecurringTransactionWorker>().build()
            WorkManager.getInstance(getApplication()).enqueue(immediateWork)
        } catch (_: Exception) {}
    }

    fun setBudgetLimit(amount: Long) {
        val prefs = getApplication<Application>()
            .getSharedPreferences("kiseh_prefs", Context.MODE_PRIVATE)
        prefs.edit().putLong("budget_limit", amount).apply()

        _budgetLimit.value = amount
        scheduleBudgetCheck()
    }

    fun changePeriod(period: String) {
        _selectedPeriod.value = period
    }

    val filteredTransactions: StateFlow<List<Transaction>> = combine(
        allTransactions,
        _searchQuery,
        _selectedCategoryFilter,
        _selectedPeriodFilter
    ) { transactions, query, category, period ->
        val now = System.currentTimeMillis()
        val oneDayMs = 24 * 60 * 60 * 1000L
        val oneWeekMs = 7 * oneDayMs
        val oneMonthMs = 30 * oneDayMs

        transactions.filter { tx ->
            val matchesQuery = query.isBlank() ||
                    tx.description.contains(query, ignoreCase = true) ||
                    tx.category.contains(query, ignoreCase = true) ||
                    tx.amount.toString().contains(query)

            val matchesCategory = category == null || tx.category == category

            val matchesPeriod = when (period) {
                PeriodFilter.ALL -> true
                PeriodFilter.TODAY -> (now - tx.date) <= oneDayMs
                PeriodFilter.THIS_WEEK -> (now - tx.date) <= oneWeekMs
                PeriodFilter.THIS_MONTH -> (now - tx.date) <= oneMonthMs
            }

            matchesQuery && matchesCategory && matchesPeriod
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val transactions: StateFlow<List<Transaction>> = allTransactions

    fun analyzeVoiceInput(text: String) {
        val result = analyzeVoiceOrTextInput(text)
        if (result.amount > 0) {
            saveAnalysisResult(result)
        }
    }

    fun deleteTransaction(transactionId: Long) {
        viewModelScope.launch {
            allTransactions.value.find { it.id == transactionId }?.let {
                repository.delete(it)
            }
        }
    }

    fun getTotalThisMonth(): Long {
        val now = System.currentTimeMillis()
        val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000
        val monthAgo = now - thirtyDaysMs
        return allTransactions.value
            .filter { !it.isIncome && it.date >= monthAgo }
            .sumOf { it.amount }
    }

    fun toggleCurrencyUnit() {
        _isCurrencyInRial.value = !_isCurrencyInRial.value
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategoryFilter(category: String?) {
        _selectedCategoryFilter.value = if (_selectedCategoryFilter.value == category) null else category
    }

    fun setPeriodFilter(period: PeriodFilter) {
        _selectedPeriodFilter.value = period
    }

    fun analyzeVoiceOrTextInput(text: String): AnalysisResult {
        val result = analyzer.analyze(text)
        _lastAnalysis.value = result
        return result
    }

    fun saveAnalysisResult(result: AnalysisResult) {
        if (result.amount > 0) {
            val transaction = Transaction(
                amount = result.amount,
                category = result.category,
                description = result.description,
                isIncome = result.isIncome
            )
            viewModelScope.launch {
                repository.insert(transaction)
            }
        }
    }

    fun addTransaction(amount: Long, category: String, description: String, isIncome: Boolean = false) {
        viewModelScope.launch {
            val transaction = Transaction(
                amount = amount,
                category = category,
                description = description.ifBlank { "ثبت دستی" },
                isIncome = isIncome
            )
            repository.insert(transaction)
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.update(transaction)
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.delete(transaction)
        }
    }

    fun clearAllTransactions() {
        viewModelScope.launch {
            repository.deleteAll()
        }
    }

    // --- Pending Bank SMS Operations ---
    fun confirmPendingSms(pendingSms: PendingBankSms) {
        val app = getApplication<Application>()
        viewModelScope.launch {
            if (pendingSms.merchant.isNotBlank() && BillingManager.isProUser(app)) {
                userCategoryLearner.learn(pendingSms.merchant, pendingSms.suggestedCategory)
            }

            val description = buildString {
                append(pendingSms.bankName)
                if (pendingSms.merchant.isNotBlank()) {
                    append(" - ")
                    append(pendingSms.merchant)
                }
                if (!pendingSms.cardLastDigits.isNullOrBlank()) {
                    append(" (کارت *")
                    append(pendingSms.cardLastDigits)
                    append(")")
                }
            }

            val canonicalCategory = SmartCategoryMatcher.toCanonicalKisehCategory(pendingSms.suggestedCategory)

            val transaction = Transaction(
                amount = pendingSms.amount,
                category = canonicalCategory,
                description = description,
                isIncome = pendingSms.isIncome,
                date = pendingSms.createdAt
            )

            repository.insert(transaction)
            pendingBankSmsRepository.markAsConfirmed(pendingSms.id)
            com.example.sms.BankSmsDuplicateDetector.recordProcessed(
                bankName = pendingSms.bankName,
                amount = pendingSms.amount,
                isIncome = pendingSms.isIncome,
                cardLastDigits = pendingSms.cardLastDigits,
                merchant = pendingSms.merchant,
                timestamp = pendingSms.createdAt
            )
        }
    }

    fun rejectPendingSms(pendingSms: PendingBankSms) {
        viewModelScope.launch {
            pendingBankSmsRepository.markAsRejected(pendingSms.id)
        }
    }

    fun cleanupOldProcessedSms(thresholdTimestamp: Long) {
        viewModelScope.launch {
            pendingBankSmsRepository.cleanupOldProcessedSms(thresholdTimestamp)
        }
    }

    fun editAndConfirmPendingSms(
        pendingSms: PendingBankSms,
        amount: Long,
        category: String,
        description: String,
        isIncome: Boolean
    ) {
        val app = getApplication<Application>()
        viewModelScope.launch {
            if (pendingSms.merchant.isNotBlank() && BillingManager.isProUser(app)) {
                userCategoryLearner.learn(pendingSms.merchant, category)
            }

            val canonicalCategory = SmartCategoryMatcher.toCanonicalKisehCategory(category)

            val transaction = Transaction(
                amount = amount,
                category = canonicalCategory,
                description = description.ifBlank { pendingSms.bankName },
                isIncome = isIncome,
                date = pendingSms.createdAt
            )

            repository.insert(transaction)
            pendingBankSmsRepository.markAsConfirmed(pendingSms.id)
            com.example.sms.BankSmsDuplicateDetector.recordProcessed(
                bankName = pendingSms.bankName,
                amount = amount,
                isIncome = isIncome,
                cardLastDigits = pendingSms.cardLastDigits,
                merchant = pendingSms.merchant,
                timestamp = pendingSms.createdAt
            )
        }
    }

    // Savings Goals
    fun addSavingsGoal(
        title: String,
        targetAmount: Long,
        initialSaved: Long = 0L,
        targetDate: Long? = null,
        note: String = "",
        iconName: String = "Star",
        colorHex: String = "#0D5C46"
    ) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val currentCount = savingsGoalRepository.allGoals.first().size
            if (!FeatureUsageManager.canAddSavingsGoal(app, currentCount)) return@launch
            val goal = SavingsGoal(
                title = title.trim(),
                targetAmount = targetAmount,
                savedAmount = initialSaved,
                targetDate = targetDate,
                note = note.trim(),
                iconName = iconName,
                colorHex = colorHex
            )
            savingsGoalRepository.insert(goal)
        }
    }

    fun updateSavingsGoal(goal: SavingsGoal) {
        viewModelScope.launch { savingsGoalRepository.update(goal) }
    }

    fun deleteSavingsGoal(goal: SavingsGoal) {
        viewModelScope.launch { savingsGoalRepository.delete(goal) }
    }

    fun addDepositToGoal(goalId: Long, depositAmount: Long) {
        if (depositAmount <= 0) return
        viewModelScope.launch { savingsGoalRepository.addDeposit(goalId, depositAmount) }
    }

    // Recurring Transactions
    fun addRecurringTransaction(
        title: String,
        amount: Long,
        isIncome: Boolean,
        category: String,
        period: String,
        startDate: Long,
        endDate: Long? = null,
        note: String = ""
    ) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val currentCount = recurringTransactionRepository.allRecurringTransactions.first().size
            if (!FeatureUsageManager.canAddRecurringTransaction(app, currentCount)) return@launch
            val item = RecurringTransaction(
                title = title.trim(),
                amount = amount,
                isIncome = isIncome,
                category = category,
                period = period,
                startDate = startDate,
                endDate = endDate,
                nextExecutionDate = startDate,
                lastExecutedDate = null,
                isActive = true,
                note = note.trim()
            )
            recurringTransactionRepository.insert(item)
            if (startDate <= System.currentTimeMillis()) {
                triggerProcessRecurringTransactions()
            }
        }
    }

    fun updateRecurringTransaction(recurringTransaction: RecurringTransaction) {
        viewModelScope.launch { recurringTransactionRepository.update(recurringTransaction) }
    }

    fun deleteRecurringTransaction(recurringTransaction: RecurringTransaction) {
        viewModelScope.launch { recurringTransactionRepository.delete(recurringTransaction) }
    }

    fun toggleRecurringTransactionActive(recurringTransaction: RecurringTransaction) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            if (!recurringTransaction.isActive && !BillingManager.isVipUser(app)) {
                val currentActive = recurringTransactionRepository.allRecurringTransactions.first().count { it.isActive }
                if (currentActive >= FeatureUsageManager.MAX_FREE_RECURRING_TRANSACTIONS) return@launch
            }
            val newState = !recurringTransaction.isActive
            recurringTransactionRepository.setActiveState(recurringTransaction.id, newState)
        }
    }

    fun triggerProcessRecurringTransactions() {
        if (!BillingManager.isVipUser(getApplication<Application>())) return
        try {
            val work = OneTimeWorkRequestBuilder<RecurringTransactionWorker>().build()
            WorkManager.getInstance(getApplication<Application>()).enqueue(work)
        } catch (_: Exception) {}
    }

    fun clearLastAnalysis() {
        _lastAnalysis.value = null
    }

    private val _sharePdfIntent = MutableSharedFlow<Intent>()
    val sharePdfIntent: SharedFlow<Intent> = _sharePdfIntent.asSharedFlow()

    private val _shareCsvIntent = MutableSharedFlow<Intent>()
    val shareCsvIntent: SharedFlow<Intent> = _shareCsvIntent.asSharedFlow()

    fun generateCsvReport(context: Context? = null) {
        viewModelScope.launch {
            val transactions = allTransactions.value
            val csvFile = withContext(Dispatchers.IO) {
                com.example.report.CsvGenerator.createExpenseReportCsv(getApplication(), transactions)
            }

            if (csvFile != null) {
                val uri = FileProvider.getUriForFile(
                    getApplication(),
                    "${getApplication<Application>().packageName}.fileprovider",
                    csvFile
                )

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "گزارش مالی (اکسل/CSV) کیسه")
                    putExtra(Intent.EXTRA_TEXT, "فایل اکسل هزینه‌ها تولید شده توسط اپلیکیشن کیسه")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooserIntent = Intent.createChooser(intent, "اشتراک‌گذاری گزارش اکسل (CSV)").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                _shareCsvIntent.emit(chooserIntent)

                context?.let { ctx ->
                    withContext(Dispatchers.Main) {
                        ctx.startActivity(chooserIntent)
                        Toast.makeText(ctx, "فایل گزارش CSV ذخیره و آماده ارسال شد.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    fun generatePdfReport(context: Context? = null) {
        viewModelScope.launch {
            val transactions = allTransactions.value
            val totalAmount = transactions.filter { !it.isIncome }.sumOf { it.amount }

            val pdfGen = PdfGenerator(getApplication())
            val pdfFile = withContext(Dispatchers.IO) {
                pdfGen.generateReport(
                    transactions = transactions,
                    periodTitle = _selectedPeriod.value,
                    totalAmount = totalAmount
                )
            }

            val uri = FileProvider.getUriForFile(
                getApplication(),
                "${getApplication<Application>().packageName}.fileprovider",
                pdfFile
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "گزارش مالی و هزینه‌های کیسه")
                putExtra(Intent.EXTRA_TEXT, "گزارش PDF هزینه‌ها تولید شده توسط اپلیکیشن کیسه")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooserIntent = Intent.createChooser(intent, "اشتراک‌گذاری گزارش PDF با پیام‌رسان‌ها").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            _sharePdfIntent.emit(chooserIntent)

            context?.let { ctx ->
                try {
                    Toast.makeText(ctx, "فایل PDF گزارش در پوشه Kiseh حافظه گوشی ذخیره شد.", Toast.LENGTH_LONG).show()
                    ctx.startActivity(chooserIntent)
                } catch (_: Exception) {
                    getApplication<Application>().startActivity(chooserIntent)
                }
            }
        }
    }

    fun exportCsvReport(context: Context) {
        viewModelScope.launch {
            try {
                val csvContent = StringBuilder()
                csvContent.append("شناسه,تاریخ,دسته‌بندی,توضیحات,مبلغ (تومان),نوع\n")
                val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                allTransactions.value.forEach { tx ->
                    val type = if (tx.isIncome) "درآمد" else "هزینه"
                    val dateStr = sdf.format(Date(tx.date))
                    csvContent.append("${tx.id},\"$dateStr\",\"${tx.category}\",\"${tx.description}\",${tx.amount},$type\n")
                }

                val fileName = "Kiseh_Backup_${SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(Date())}.csv"
                val appDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Kiseh").apply {
                    if (!exists()) mkdirs()
                }
                val csvFile = File(appDir, fileName)
                csvFile.writeText(csvContent.toString(), Charsets.UTF_8)

                try {
                    val publicFolder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Kiseh").apply {
                        if (!exists()) mkdirs()
                    }
                    csvFile.copyTo(File(publicFolder, fileName), overwrite = true)
                } catch (_: Exception) {}

                val uri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    csvFile
                )

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "پشتیبان اکسل/CSV اطلاعات کیسه")
                    putExtra(Intent.EXTRA_TEXT, "فایل پشتیبان جدول تراکنش‌های اپلیکیشن کیسه")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooserIntent = Intent.createChooser(intent, "اشتراک‌گذاری فایل CSV/اکسل").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                Toast.makeText(context, "فایل CSV ذخیره شد", Toast.LENGTH_LONG).show()
                context.startActivity(chooserIntent)
            } catch (e: Exception) {
                Toast.makeText(context, "خطا در خروجی CSV: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // =========================================================================
    // Local Backup & Restore (SAF - کاربر محل ذخیره را انتخاب می‌کند)
    // =========================================================================

    /**
     * ساخت بکاپ رمزنگاری‌شده و نوشتن آن در URI انتخاب‌شده توسط کاربر.
     * کاربر از طریق پنجره SAF (Storage Access Framework) محل و نام فایل را انتخاب می‌کند.
     */
    fun createBackupToUri(uri: Uri) {
        viewModelScope.launch {
            _backupState.value = CloudBackupState.InProgress("در حال ساخت و رمزنگاری بکاپ...")
            try {
                val payload = buildBackupPayload()
                val encryptedBytes = withContext(Dispatchers.IO) {
                    BackupCryptoManager.encryptBackup(payload)
                }

                withContext(Dispatchers.IO) {
                    val outputStream = getApplication<Application>().contentResolver.openOutputStream(uri)
                        ?: throw Exception("امکان نوشتن در محل انتخاب‌شده وجود ندارد")
                    outputStream.use { it.write(encryptedBytes) }
                }

                Log.i("LocalBackup", "Backup written to URI (${encryptedBytes.size} bytes)")
                _backupState.value = CloudBackupState.Success(
                    "فایل بکاپ با موفقیت ذخیره شد (${payload.transactions.size} تراکنش)"
                )
            } catch (e: Exception) {
                Log.e("LocalBackup", "Backup failed", e)
                _backupState.value = CloudBackupState.Error(
                    e.localizedMessage ?: "خطا در ساخت فایل بکاپ"
                )
            }
        }
    }

    /**
     * خواندن فایل بکاپ از URI انتخاب‌شده توسط کاربر، رمزگشایی و بازنشانی دیتابیس.
     */
    fun restoreBackupFromUri(uri: Uri) {
        viewModelScope.launch {
            _backupState.value = CloudBackupState.InProgress("در حال خواندن و رمزگشایی فایل بکاپ...")
            try {
                val encryptedBytes = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: throw Exception("امکان خواندن فایل انتخاب‌شده وجود ندارد")
                }

                val payload = withContext(Dispatchers.IO) {
                    BackupCryptoManager.decryptBackup(encryptedBytes)
                }

                // بازنشانی اتمیک در دیتابیس
                withContext(Dispatchers.IO) {
                    database.withTransaction {
                        if (payload.customCategories.isNotEmpty()) {
                            database.customCategoryDao().insertAll(payload.customCategories)
                        }
                        if (payload.transactions.isNotEmpty()) {
                            database.transactionDao().insertAll(payload.transactions)
                        }
                        if (payload.savingsGoals.isNotEmpty()) {
                            database.savingsGoalDao().insertAll(payload.savingsGoals)
                        }
                        if (payload.recurringTransactions.isNotEmpty()) {
                            database.recurringTransactionDao().insertAll(payload.recurringTransactions)
                        }
                    }
                }

                // بازیابی داده‌های یادگیری پیامک بانکی
                if (payload.learnedMerchants.isNotEmpty()) {
                    try {
                        userCategoryLearner.learnAll(payload.learnedMerchants)
                    } catch (e: Exception) {
                        Log.w("LocalBackup", "Failed to restore learned merchants: ${e.message}")
                    }
                }

                // بازیابی تنظیمات
                _budgetLimit.value = payload.appPreferences.budgetLimit
                _isCurrencyInRial.value = payload.appPreferences.isCurrencyInRial
                _selectedPeriod.value = payload.appPreferences.selectedPeriod

                val prefs = getApplication<Application>().getSharedPreferences("kiseh_prefs", Context.MODE_PRIVATE)
                prefs.edit()
                    .putLong("budget_limit", payload.appPreferences.budgetLimit)
                    .putBoolean("is_currency_rial", payload.appPreferences.isCurrencyInRial)
                    .putString("selected_period", payload.appPreferences.selectedPeriod)
                    .apply()

                Log.i("LocalBackup", "Restore complete: tx=${payload.transactions.size}")
                _backupState.value = CloudBackupState.Success(
                    "اطلاعات با موفقیت بازیابی شد (${payload.transactions.size} تراکنش)"
                )
            } catch (e: Exception) {
                Log.e("LocalBackup", "Restore failed", e)
                _backupState.value = CloudBackupState.Error(
                    e.localizedMessage ?: "خطا در بازیابی اطلاعات. فایل ممکن است خراب یا رمزنگاری‌شده با کلید متفاوت باشد"
                )
            }
        }
    }

    fun resetCloudBackupState() {
        _backupState.value = CloudBackupState.Idle
    }

    /**
     * ساخت payload بکاپ از داده‌های فعلی دیتابیس
     */
    private suspend fun buildBackupPayload(): KisehBackupPayload = withContext(Dispatchers.IO) {
        val transactions = database.transactionDao().getAllTransactionsList()
        val goals = database.savingsGoalDao().getAllGoalsList()
        val recurring = database.recurringTransactionDao().getAllList()
        val categories = database.customCategoryDao().getAllCategoriesList()

        val learnedMerchants = try {
            userCategoryLearner.getAllLearned()
        } catch (e: Exception) {
            emptyMap()
        }

        val prefsBackup = AppPreferencesBackup(
            budgetLimit = _budgetLimit.value,
            isCurrencyInRial = _isCurrencyInRial.value,
            selectedPeriod = _selectedPeriod.value,
            isVip = BillingManager.isProUser(getApplication())
        )

        KisehBackupPayload(
            timestamp = System.currentTimeMillis(),
            transactions = transactions,
            savingsGoals = goals,
            recurringTransactions = recurring,
            customCategories = categories,
            learnedMerchants = learnedMerchants,
            appPreferences = prefsBackup
        )
    }

    // =========================================================================
    // Security
    // =========================================================================

    fun unlockApp() { _isAppCurrentlyLocked.value = false }

    fun lockApp() {
        if (isAppLockEnabled.value) _isAppCurrentlyLocked.value = true
    }

    fun verifyPin(pin: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val isCorrect = securityManager.verifyPin(pin)
            if (isCorrect) _isAppCurrentlyLocked.value = false
            onResult(isCorrect)
        }
    }

    fun setupNewPin(
        pin: String,
        enableBiometric: Boolean = false,
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        if (pin.length != 4 && pin.length != 6) {
            onError("رمز عبور باید ۴ یا ۶ رقمی باشد")
            return
        }
        viewModelScope.launch {
            securityManager.savePin(pin)
            if (enableBiometric) securityManager.setBiometricEnabled(true)
            _isAppCurrentlyLocked.value = false
            onSuccess()
        }
    }

    fun changePin(
        currentPin: String,
        newPin: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (newPin.length != 4 && newPin.length != 6) {
            onError("رمز جدید باید ۴ یا ۶ رقمی باشد")
            return
        }
        viewModelScope.launch {
            val isCurrentValid = securityManager.verifyPin(currentPin)
            if (!isCurrentValid) {
                onError("رمز عبور فعلی نادرست است")
                return@launch
            }
            securityManager.savePin(newPin)
            onSuccess()
        }
    }

    fun disableAppLock(
        currentPin: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val isCurrentValid = securityManager.verifyPin(currentPin)
            if (!isCurrentValid) {
                onError("رمز عبور فعلی نادرست است")
                return@launch
            }
            securityManager.clearSecurityData()
            _isAppCurrentlyLocked.value = false
            onSuccess()
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch { securityManager.setBiometricEnabled(enabled) }
    }

    fun setLockOnLaunchEnabled(enabled: Boolean) {
        viewModelScope.launch { securityManager.setLockOnLaunchEnabled(enabled) }
    }

    fun checkBiometricAvailability(): BiometricAuthManager.BiometricStatus {
        return BiometricAuthManager.checkBiometricAvailability(getApplication())
    }
}