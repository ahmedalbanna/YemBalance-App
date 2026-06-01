package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.SettlementEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.UserEntity
import com.example.data.repository.ShopRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ShopViewModel(
    application: Application,
    private val repository: ShopRepository
) : AndroidViewModel(application) {

    // Authentication session state
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    // Location Simulation
    private val _isUserInShopRange = MutableStateFlow(true) // Defaults to true to make testing effortless
    val isUserInShopRange: StateFlow<Boolean> = _isUserInShopRange.asStateFlow()

    private val _shopLocationName = MutableStateFlow("محل الحلاقة الفرع الرئيسي - صنعاء")
    val shopLocationName: StateFlow<String> = _shopLocationName.asStateFlow()

    // Active sync indicators
    private val _syncProgress = MutableStateFlow<Int?>(null)
    val syncProgress: StateFlow<Int?> = _syncProgress.asStateFlow()

    private val _syncStatusText = MutableStateFlow<String?>(null)
    val syncStatusText: StateFlow<String?> = _syncStatusText.asStateFlow()

    // Query states for manager search
    val searchQuery = MutableStateFlow("")
    val selectedFilterDate = MutableStateFlow("") // e.g. YYYY-MM-DD, empty means today or all
    val selectedEmployeeFilter = MutableStateFlow<Int?>(null) // null means all

    // Reactive streams
    val allUsers: StateFlow<List<UserEntity>> = repository.allUsersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUniqueShiftDates: StateFlow<List<String>> = repository.allShiftDatesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSettlements: StateFlow<List<SettlementEntity>> = repository.allSettlementsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Calculations of employee earnings today
    val todayDateStr = getTodayDateString()

    init {
        viewModelScope.launch {
            // Check & populate default users as specified by the user stories and requirements
            repository.checkAndSeedDatabase()
        }
    }

    // Helper to get today's date
    fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    // Login checks
    fun login(phone: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            if (!_isUserInShopRange.value) {
                // Location checking system
                onError("خطأ: أنت خارج نطاق المحل الجغرافي المحدد! لا يسمح بتسجيل الدخول.")
                return@launch
            }

            val user = repository.getUserByPhone(phone)
            if (user != null) {
                if (user.password == password) {
                    if (user.isActive) {
                        _currentUser.value = user
                        onSuccess()
                    } else {
                        onError("رقم الحساب معطل حالياً من قِبل المدير.")
                    }
                } else {
                    onError("خطأ: كلمة المرور المدخلة غير صحيحة.")
                }
            } else {
                onError("خطأ: لا يوجد موظف مregistered بهذا الرقم.")
            }
        }
    }

    fun logout() {
        _currentUser.value = null
    }

    fun setLocationConstraint(isInRange: Boolean) {
        _isUserInShopRange.value = isInRange
    }

    fun updateShopLocation(newName: String) {
        _shopLocationName.value = newName
    }

    // Employee specific adding transaction
    fun addTransaction(
        type: String, // INCOME or EXPENSE
        amount: Double,
        description: String,
        expenseCategory: String? = null // SHOP or PERSONAL
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val transaction = TransactionEntity(
                employeeId = user.id,
                employeeName = user.name,
                type = type,
                expenseCategory = expenseCategory,
                amount = amount,
                description = description,
                shiftDate = getTodayDateString()
            )
            repository.insertTransaction(transaction)
        }
    }

    // Manager direct CRUD on transactions
    fun managerAddTransactionForEmployee(
        employeeId: Int,
        type: String,
        amount: Double,
        description: String,
        expenseCategory: String?,
        dateStr: String
    ) {
        viewModelScope.launch {
            val emp = repository.getUserById(employeeId)
            val name = emp?.name ?: "موظف مجهول"
            val transaction = TransactionEntity(
                employeeId = employeeId,
                employeeName = name,
                type = type,
                expenseCategory = expenseCategory,
                amount = amount,
                description = description,
                shiftDate = if (dateStr.isEmpty()) getTodayDateString() else dateStr
            )
            repository.insertTransaction(transaction)
        }
    }

    fun managerUpdateTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
        }
    }

    fun managerDeleteTransaction(id: Int) {
        viewModelScope.launch {
            repository.deleteTransactionById(id)
        }
    }

    // Manager employee accounts CRUD
    fun managerCreateEmployee(
        name: String,
        phone: String,
        password: String,
        baseSalary: Double,
        commissionPercent: Double
    ) {
        viewModelScope.launch {
            val employee = UserEntity(
                name = name,
                phone = phone,
                role = "EMPLOYEE",
                password = password,
                baseSalary = baseSalary,
                commissionPercent = commissionPercent
            )
            repository.insertUser(employee)
        }
    }

    fun managerUpdateEmployee(
        id: Int,
        name: String,
        phone: String,
        password: String,
        baseSalary: Double,
        commissionPercent: Double,
        isActive: Boolean
    ) {
        viewModelScope.launch {
            val existing = repository.getUserById(id)
            if (existing != null) {
                val updated = existing.copy(
                    name = name,
                    phone = phone,
                    password = password,
                    baseSalary = baseSalary,
                    commissionPercent = commissionPercent,
                    isActive = isActive
                )
                repository.updateUser(updated)
            }
        }
    }

    fun managerDeleteEmployee(id: Int) {
        viewModelScope.launch {
            repository.deleteUserById(id)
        }
    }

    // MOCK P2P wifi or Bluetooth sync log simulation
    fun triggerSyncSimulation(syncType: String) {
        viewModelScope.launch {
            _syncProgress.value = 0
            _syncStatusText.value = "جاري الكشف عن أجهزة قريبة عبر $syncType..."
            delay(1200)

            _syncProgress.value = 35
            _syncStatusText.value = "تم العثور على جهاز المدير. جاري تأسيس اتصال آمن..."
            delay(10 * 100)

            _syncProgress.value = 70
            _syncStatusText.value = "جاري ترحيل وتحديث السجلات الحسابية لليوم..."
            delay(1200)

            // Inject one test custom entry to simulate sync incoming data if logged in as manager
            val user = _currentUser.value
            if (user?.role == "MANAGER") {
                // Manager sync updates counts
                _syncStatusText.value = "تمت المزامنة بنجاح واسترداد حركات الموظفين بالكامل!"
            } else {
                _syncStatusText.value = "نجاح المزامنة مع شبكة المحل! تم تسليم جميع الحركات المالية المسجلة للمدير."
            }
            _syncProgress.value = 100
            delay(1500)
            _syncProgress.value = null
            _syncStatusText.value = null
        }
    }

    // Settles active employee shift at the end of the day or and clears dues
    fun settleEmployeeAccount(
        employeeId: Int,
        dateStr: String,
        totalIncome: Double,
        totalExpenseShop: Double,
        totalExpensePersonal: Double,
        calculatedSalary: Double,
        amountPaid: Double,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val emp = repository.getUserById(employeeId)
            val name = emp?.name ?: "موظف"
            repository.settleEmployeeShift(
                employeeId = employeeId,
                employeeName = name,
                dateStr = dateStr,
                totalIncome = totalIncome,
                totalExpenseShop = totalExpenseShop,
                totalExpensePersonal = totalExpensePersonal,
                calculatedSalary = calculatedSalary,
                amountPaid = amountPaid
            )
            onSuccess()
        }
    }
}

class ShopViewModelFactory(
    private val application: Application,
    private val repository: ShopRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShopViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ShopViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
