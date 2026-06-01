package com.example.data.repository

import com.example.data.local.UserDao
import com.example.data.local.TransactionDao
import com.example.data.local.SettlementDao
import com.example.data.model.UserEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.SettlementEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ShopRepository(
    private val userDao: UserDao,
    private val transactionDao: TransactionDao,
    private val settlementDao: SettlementDao
) {

    val allUsersFlow: Flow<List<UserEntity>> = userDao.getAllUsersFlow()
    val allTransactionsFlow: Flow<List<TransactionEntity>> = transactionDao.getAllTransactionsFlow()
    val allSettlementsFlow: Flow<List<SettlementEntity>> = settlementDao.getAllSettlementsFlow()
    val allShiftDatesFlow: Flow<List<String>> = transactionDao.getAllUniqueShiftDatesFlow()

    fun getTransactionsForEmployee(employeeId: Int): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsForEmployeeFlow(employeeId)
    }

    fun getTransactionsForEmployeeToday(employeeId: Int, dateStr: String): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsForEmployeeTodayFlow(employeeId, dateStr)
    }

    fun getTransactionsForDate(dateStr: String): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsForDateFlow(dateStr)
    }

    suspend fun getUserByPhone(phone: String): UserEntity? {
        return userDao.getUserByPhone(phone)
    }

    suspend fun getUserById(id: Int): UserEntity? {
        return userDao.getUserById(id)
    }

    suspend fun insertUser(user: UserEntity): Long {
        return userDao.insertUser(user)
    }

    suspend fun updateUser(user: UserEntity) {
        userDao.updateUser(user)
    }

    suspend fun deleteUserById(id: Int) {
        userDao.deleteUserById(id)
    }

    suspend fun resetPassword(id: Int, newPassword: String) {
        userDao.resetPassword(id, newPassword)
    }

    suspend fun updatePhone(id: Int, newPhone: String) {
        userDao.updatePhone(id, newPhone)
    }

    suspend fun insertTransaction(transaction: TransactionEntity): Long {
        return transactionDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun deleteTransactionById(id: Int) {
        transactionDao.deleteTransactionById(id)
    }

    suspend fun getTransactionById(id: Int): TransactionEntity? {
        return transactionDao.getTransactionById(id)
    }

    // Settles the records for an employee for a day
    suspend fun settleEmployeeShift(
        employeeId: Int,
        employeeName: String,
        dateStr: String,
        totalIncome: Double,
        totalExpenseShop: Double,
        totalExpensePersonal: Double,
        calculatedSalary: Double,
        amountPaid: Double
    ): Long {
        // Create settlement record
        val settlement = SettlementEntity(
            employeeId = employeeId,
            employeeName = employeeName,
            dateString = dateStr,
            totalIncome = totalIncome,
            totalExpenseShop = totalExpenseShop,
            totalExpensePersonal = totalExpensePersonal,
            calculatedSalary = calculatedSalary,
            amountPaid = amountPaid,
            timestamp = System.currentTimeMillis()
        )
        val settlementId = settlementDao.insertSettlement(settlement)

        // Mark corresponding transactions as cleared
        transactionDao.clearTransactionsForEmployeeDate(employeeId, dateStr, System.currentTimeMillis())

        return settlementId
    }

    suspend fun getSettlementForEmployeeAndDate(employeeId: Int, dateStr: String): SettlementEntity? {
        return settlementDao.getSettlementForEmployeeAndDate(employeeId, dateStr)
    }

    suspend fun getTransactionsForEmployeeDateDirect(employeeId: Int, dateString: String): List<TransactionEntity> {
        return transactionDao.getTransactionsForEmployeeDateDirect(employeeId, dateString)
    }

    // DB Seeding helper
    suspend fun checkAndSeedDatabase() {
        val users = userDao.getAllUsersFlow().first()
        if (users.isEmpty()) {
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

            // 1. Seed Manager Jalal
            val managerId = userDao.insertUser(
                UserEntity(
                    phone = "770000000",
                    name = "جلال (المدير العام)",
                    role = "MANAGER",
                    password = "admin",
                    baseSalary = 0.0,
                    commissionPercent = 0.0
                )
            ).toInt()

            // 2. Seed Employee Ahmed
            val ahmedId = userDao.insertUser(
                UserEntity(
                    phone = "771111111",
                    name = "أحمد",
                    role = "EMPLOYEE",
                    password = "123",
                    baseSalary = 3000.0, // daily salary
                    commissionPercent = 10.0 // 10% percentage
                )
            ).toInt()

            // 3. Seed Employee Ali
            val aliId = userDao.insertUser(
                UserEntity(
                    phone = "772222222",
                    name = "علي",
                    role = "EMPLOYEE",
                    password = "123",
                    baseSalary = 4000.0,
                    commissionPercent = 12.0
                )
            ).toInt()

            // 4. Seed Employee Osama
            val osamaId = userDao.insertUser(
                UserEntity(
                    phone = "773333333",
                    name = "أسامة",
                    role = "EMPLOYEE",
                    password = "123",
                    baseSalary = 5000.0,
                    commissionPercent = 15.0
                )
            ).toInt()

            // --- Seed Ahmed's transactions (from User Story 1) ---
            // احمد: 500 + 1000 + 700 + 1000 + 750 = 3950 دخل
            // خرج: غداء 1000خصم ، 600 مستلزمات محل لا خصم
            val ahmedIncomes = listOf(500.0, 1000.0, 700.0, 1000.0, 750.0)
            ahmedIncomes.forEachIndexed { i, amount ->
                transactionDao.insertTransaction(
                    TransactionEntity(
                        employeeId = ahmedId,
                        employeeName = "أحمد",
                        type = "INCOME",
                        amount = amount,
                        description = "خدمة حلاقة لزبون رقم ${i + 1}",
                        shiftDate = dateStr,
                        timestamp = System.currentTimeMillis() - (10 - i) * 60 * 1000
                    )
                )
            }
            transactionDao.insertTransaction(
                TransactionEntity(
                    employeeId = ahmedId,
                    employeeName = "أحمد",
                    type = "EXPENSE",
                    expenseCategory = "SHOP",
                    amount = 600.0,
                    description = "شراء معجون ومستلزمات حلاقة للمحل",
                    shiftDate = dateStr,
                    timestamp = System.currentTimeMillis() - 4 * 60 * 1000
                )
            )
            transactionDao.insertTransaction(
                TransactionEntity(
                    employeeId = ahmedId,
                    employeeName = "أحمد",
                    type = "EXPENSE",
                    expenseCategory = "PERSONAL",
                    amount = 1000.0,
                    description = "مصروف وجبة الغداء شخصي",
                    shiftDate = dateStr,
                    timestamp = System.currentTimeMillis() - 2 * 60 * 1000
                )
            )

            // --- Seed Ali's transactions (from User Story 2) ---
            // علي: 2000 + 3500 = 5500 دخل
            // خرج: غداء 1500 خصم ، 1000 مستلزمات محل لا خصم
            val aliIncomes = listOf(2000.0, 3500.0)
            aliIncomes.forEachIndexed { i, amount ->
                transactionDao.insertTransaction(
                    TransactionEntity(
                        employeeId = aliId,
                        employeeName = "علي",
                        type = "INCOME",
                        amount = amount,
                        description = "خدمة حلاقة وتصفيف لزبون رقم ${i + 1}",
                        shiftDate = dateStr,
                        timestamp = System.currentTimeMillis() - (8 - i) * 60 * 1000
                    )
                )
            }
            transactionDao.insertTransaction(
                TransactionEntity(
                    employeeId = aliId,
                    employeeName = "علي",
                    type = "EXPENSE",
                    expenseCategory = "SHOP",
                    amount = 1000.0,
                    description = "شراء شامبو حلاقة ومطهرات للمحل",
                    shiftDate = dateStr,
                    timestamp = System.currentTimeMillis() - 3 * 60 * 1000
                )
            )
            transactionDao.insertTransaction(
                TransactionEntity(
                    employeeId = aliId,
                    employeeName = "علي",
                    type = "EXPENSE",
                    expenseCategory = "PERSONAL",
                    amount = 1500.0,
                    description = "شراء غداء متكامل خصمي السنوي",
                    shiftDate = dateStr,
                    timestamp = System.currentTimeMillis() - 1 * 60 * 1000
                )
            )

            // --- Seed Osama's transactions (from User Story 3) ---
            // اسامة: 5000 = 5000 دخل
            // خرج: غداء 1000 خصم ، 0 مستلزمات محل
            transactionDao.insertTransaction(
                TransactionEntity(
                    employeeId = osamaId,
                    employeeName = "أسامة",
                    type = "INCOME",
                    amount = 5000.0,
                    description = "مجموعة خدمات حلاقة لزبون خاص",
                    shiftDate = dateStr,
                    timestamp = System.currentTimeMillis() - 5 * 60 * 1000
                )
            )
            transactionDao.insertTransaction(
                TransactionEntity(
                    employeeId = osamaId,
                    employeeName = "أسامة",
                    type = "EXPENSE",
                    expenseCategory = "PERSONAL",
                    amount = 1000.0,
                    description = "مصروف وجبة الغداء ومشروب",
                    shiftDate = dateStr,
                    timestamp = System.currentTimeMillis() - 3 * 60 * 1000
                )
            )
        }
    }
}
