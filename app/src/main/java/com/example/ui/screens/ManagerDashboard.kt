package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SettlementEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.UserEntity
import com.example.ui.ShopViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerDashboard(
    viewModel: ShopViewModel,
    currentUser: UserEntity,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val allTransactions by viewModel.allTransactions.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val allSettlements by viewModel.allSettlements.collectAsState()

    val tabs = listOf(
        Triple("الإيرادات والنشاط", Icons.Default.Dashboard, 0),
        Triple("تصفية الحسابات", Icons.Default.Payments, 1),
        Triple("إدارة العمليات", Icons.Default.ReceiptLong, 2),
        Triple("شؤون الموظفين والموقع", Icons.Default.PeopleAlt, 3)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "لوحة المدير: ${currentUser.name}",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "إدارة المحل وموثق الإيرادات",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "تسجيل خروج", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                tabs.forEach { (label, icon, index) ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> TabOverview(viewModel, allTransactions, allUsers)
                1 -> TabSettlements(viewModel, allTransactions, allUsers, allSettlements)
                2 -> TabTransactionsCrud(viewModel, allTransactions, allUsers)
                3 -> TabEmployeesAndGeo(viewModel, allUsers)
            }
        }
    }
}

// ==========================================
// TAB 0: OVERVIEW & ACTIVE STAFF ACTIVITIES
// ==========================================
@Composable
fun TabOverview(
    viewModel: ShopViewModel,
    allTransactions: List<TransactionEntity>,
    allUsers: List<UserEntity>
) {
    var rangeFilter by remember { mutableStateOf("DAY") } // DAY, MONTH, YEAR

    val calendar = remember { Calendar.getInstance() }
    val todayStr = viewModel.getTodayDateString()
    val thisMonthPrefix = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
    val thisYearPrefix = SimpleDateFormat("yyyy", Locale.US).format(Date())

    // Group transactions based on period selector
    val filteredTrans = remember(allTransactions, rangeFilter, todayStr, thisMonthPrefix, thisYearPrefix) {
        allTransactions.filter {
            when (rangeFilter) {
                "DAY" -> it.shiftDate == todayStr
                "MONTH" -> it.shiftDate.startsWith(thisMonthPrefix)
                "YEAR" -> it.shiftDate.startsWith(thisYearPrefix)
                else -> true
            }
        }
    }

    // Calculations
    val totalIncome = filteredTrans.filter { it.type == "INCOME" }.sumOf { it.amount }
    val totalShopExpense = filteredTrans.filter { it.type == "EXPENSE" && it.expenseCategory == "SHOP" }.sumOf { it.amount }
    val totalPersonalExpense = filteredTrans.filter { it.type == "EXPENSE" && it.expenseCategory == "PERSONAL" }.sumOf { it.amount }
    val netRevenue = totalIncome - totalShopExpense - totalPersonalExpense

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Date filter choices switcher buttons
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf(
                Triple("DAY", "الحصاد اليومي", Icons.Default.Today),
                Triple("MONTH", "الملخص الشهري", Icons.Default.CalendarMonth),
                Triple("YEAR", "السجل السنوي", Icons.Default.DateRange)
            ).forEach { (mode, label, icon) ->
                val isSelected = rangeFilter == mode
                Button(
                    onClick = { rangeFilter = mode },
                    modifier = Modifier.weight(1f),
                    shape = when (mode) {
                        "DAY" -> RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp)
                        "YEAR" -> RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp)
                        else -> RoundedCornerShape(0.dp)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
                        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Stats Visual Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "الحالة المالية للمحل (${if (rangeFilter == "DAY") "اليوم" else if (rangeFilter == "MONTH") "هذا الشهر" else "هذا العام"})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("إجمالي الدخل (خدمات)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${totalIncome.toInt()} ر.ي", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                    }
                    Column {
                        Text("مصاريف المحل مقتنيات", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${totalShopExpense.toInt()} ر.ي", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                    }
                    Column {
                        Text("سلف غذاء شخصي", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${totalPersonalExpense.toInt()} ر.ي", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.error)
                    }
                }

                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(vertical = 12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("صافي الأرباح النقدية المودعة:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${netRevenue.toInt()} ريال",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Active Employee Check Indicators Today (satisfies: "المدير لديه علم بالموظفين النشطين")
        Text(
            text = "الموظفين النشطين اليوم وتتبع حركتهم:",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        val employeesOnly = allUsers.filter { it.role == "EMPLOYEE" }

        employeesOnly.forEach { emp ->
            val empTransToday = allTransactions.filter { it.employeeId == emp.id && it.shiftDate == todayStr }
            val isActive = empTransToday.isNotEmpty()

            val empIncome = empTransToday.filter { it.type == "INCOME" }.sumOf { it.amount }
            val empExp = empTransToday.filter { it.type == "EXPENSE" }.sumOf { it.amount }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(12.dp)
                    ),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Profile initial icon circle with active/inactive dot
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(
                                        if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(21.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = emp.name.take(1),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(11.dp)
                                    .border(1.5.dp, MaterialTheme.colorScheme.surface, RoundedCornerShape(5.5.dp))
                                    .background(
                                        if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        shape = RoundedCornerShape(5.5.dp)
                                    )
                            )
                        }

                        Column {
                            Text(emp.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                "تبيان اليوم • ${empTransToday.size} حركات مسجلة",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Tally indicators for the specific employee to view on-demand
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (isActive) "دخل: +${empIncome.toInt()} ر.ي" else "خارج الدوام",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (isActive) {
                            Text(
                                text = "خرج: -${empExp.toInt()} ر.ي",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB 1: DAILY PAYOUT SETTLEMENT PROCESS
// ==========================================
@Composable
fun TabSettlements(
    viewModel: ShopViewModel,
    allTransactions: List<TransactionEntity>,
    allUsers: List<UserEntity>,
    allSettlements: List<SettlementEntity>
) {
    val todayStr = viewModel.getTodayDateString()
    val employees = allUsers.filter { it.role == "EMPLOYEE" }

    var showSuccessBanner by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "استلام المبالغ وتصفية حسابات الورديات (اليوم)",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        AnimatedVisibility(visible = showSuccessBanner != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = showSuccessBanner ?: "",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(employees) { emp ->
                val myTransactionsToday = allTransactions.filter {
                    it.employeeId == emp.id && it.shiftDate == todayStr
                }

                // Check if already settled in Settlements database
                val alreadySettled = allSettlements.any {
                    it.employeeId == emp.id && it.dateString == todayStr
                }

                val totalIncome = myTransactionsToday.filter { it.type == "INCOME" }.sumOf { it.amount }
                val totalShopExpense = myTransactionsToday.filter { it.type == "EXPENSE" && it.expenseCategory == "SHOP" }.sumOf { it.amount }
                val totalPersonalExpense = myTransactionsToday.filter { it.type == "EXPENSE" && it.expenseCategory == "PERSONAL" }.sumOf { it.amount }

                val commissionEarned = (totalIncome * emp.commissionPercent) / 100.0
                val netCalculatedSalary = emp.baseSalary + commissionEarned - totalPersonalExpense

                // Cash in hand the employee must yield to owner:
                // Total Income (collected) - Total Shop Expense (paid for shop by employee) - Total Personal Expense (already consumed/withdrawn)
                // Wait! The cash-in-hand to yield represents the collected income of sales. Personal loan (غذاء) is kept by the employee as personal withdrawal, and shop supplies paid out of hand are reimbursed or taken from collected sales.
                // Let's formulate the exact "صندوق الوردية المتبقي" (Cash to extract from employee):
                // `Total Income` (المدخلات النقدية) - `Total Shop Expense` (تجهيزات اشتراها الموظف للمحل) - `Total Personal Expense` (غذاء الموظف المسحوب).
                val cashierYield = totalIncome - totalShopExpense - totalPersonalExpense

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (alreadySettled) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(12.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = emp.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (alreadySettled) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (alreadySettled) "✓ تمت التسوية والقبول" else "⌛ معلق ونشط",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (alreadySettled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Arithmetic breakdown rows
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("العمليات المسجلة اليوم:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${myTransactionsToday.size} حركات", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("مجموع دخل الوردية (الزبائن):", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("+${totalIncome.toInt()} ر.ي", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("مصاريف محل (مسددة من الموظف):", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("-${totalShopExpense.toInt()} ر.ي (لا تخصم)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("سلف غذاء المسحوبة للموظف :", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("-${totalPersonalExpense.toInt()} ر.ي (تخصم)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                        }

                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(vertical = 8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("المبلغ النقدي الواجب استلامه في الدرج:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                                Text("(صافي إيراد الدرج المطلوب تسليمه لك)", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                text = "${cashierYield.toInt()} ر.ي",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("مستحقات راتب وعمولة الموظف لليوم:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                Text("(الراتب اليومي + النسبة - السُلف)", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                text = "${netCalculatedSalary.toInt()} ر.ي",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (!alreadySettled) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    viewModel.settleEmployeeAccount(
                                        employeeId = emp.id,
                                        dateStr = todayStr,
                                        totalIncome = totalIncome,
                                        totalExpenseShop = totalShopExpense,
                                        totalExpensePersonal = totalPersonalExpense,
                                        calculatedSalary = netCalculatedSalary,
                                        amountPaid = netCalculatedSalary,
                                        onSuccess = {
                                            showSuccessBanner = "✓ تم نجاح استلام المبالغ (${cashierYield.toInt()} ر.ي) وتصفية وتصفير حساب الموظف ${emp.name} وصرف كامل مستحقاته لليوم!"
                                        }
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.PriceCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("تصفية الحساب واستلام الإيرادات النقدي", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB 2: GLOBAL TRANSACTION CRUD & SEARCH
// ==========================================
@Composable
fun TabTransactionsCrud(
    viewModel: ShopViewModel,
    allTransactions: List<TransactionEntity>,
    allUsers: List<UserEntity>
) {
    var searchQuery by remember { mutableStateOf("") }
    var searchDate by remember { mutableStateOf("") } // YYYY-MM-DD format
    var selectedEmployeeId by remember { mutableStateOf<Int?>(null) }

    var showEditDialog by remember { mutableStateOf<TransactionEntity?>(null) }
    var showDeleteDialog by remember { mutableStateOf<TransactionEntity?>(null) }
    var showAddManualDialog by remember { mutableStateOf(false) }

    val filteredTransactions = remember(allTransactions, searchQuery, searchDate, selectedEmployeeId) {
        allTransactions.filter { item ->
            val matchesText = item.description.contains(searchQuery, ignoreCase = true) ||
                    item.employeeName.contains(searchQuery, ignoreCase = true)

            val matchesDate = if (searchDate.isNotEmpty()) item.shiftDate == searchDate else true

            val matchesEmployee = if (selectedEmployeeId != null) item.employeeId == selectedEmployeeId else true

            matchesText && matchesDate && matchesEmployee
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "سجل العمليات والبحث المالي والتعديل",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            IconButton(onClick = { showAddManualDialog = true }) {
                Icon(Icons.Default.PostAdd, contentDescription = "قيد حركة مالية", tint = MaterialTheme.colorScheme.primary)
            }
        }

        // Filters Container Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Text Search input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ابحث باسم الموظف أو البيان...", style = MaterialTheme.typography.bodySmall) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Date Filter Input box (simplified text helper, but handles YYYY-MM-DD matching)
                    OutlinedTextField(
                        value = searchDate,
                        onValueChange = { searchDate = it },
                        placeholder = { Text("التاريخ كـ YYYY-MM-DD", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.weight(1.2f)
                    )

                    // Reset button for date filter
                    if (searchDate.isNotEmpty()) {
                        IconButton(onClick = { searchDate = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "تصفية التاريخ", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                // Horizontal list of employee filter options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("موظف:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val employees = allUsers.filter { it.role == "EMPLOYEE" }

                    // All chip option
                    FilterChip(
                        selected = selectedEmployeeId == null,
                        onClick = { selectedEmployeeId = null },
                        label = { Text("الكل", style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary,
                            containerColor = MaterialTheme.colorScheme.outline,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    employees.forEach { emp ->
                        FilterChip(
                            selected = selectedEmployeeId == emp.id,
                            onClick = { selectedEmployeeId = emp.id },
                            label = { Text(emp.name, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                                containerColor = MaterialTheme.colorScheme.outline,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }

        // List Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "العمليات الحسابية المطابقة (${filteredTransactions.size})",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Dynamic Excel-style exporter preview
            Text(
                text = "قيد التعديل متاح للمدير ✒",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (filteredTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("لا توجد عمليات مطابقة لشروط البحث.", fontSize = 13.sp, color = TextMuted)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredTransactions, key = { it.id }) { trans ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(
                                                color = if (trans.type == "INCOME") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                    )
                                    Text(
                                        text = trans.employeeName,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "• ${trans.shiftDate}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Text(
                                    text = "${if (trans.type == "INCOME") "+" else "-"}${trans.amount.toInt()} ر.ي",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    color = if (trans.type == "INCOME") MaterialTheme.colorScheme.primary else if (trans.expenseCategory == "SHOP") MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = trans.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = if (trans.type == "INCOME") "دخل الزبائن" else if (trans.expenseCategory == "SHOP") "مقتنيات المحل" else "مصاريف شخصية",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text("•", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = if (trans.isCleared) "مسوى نقداً ✓" else "⌛ معلق",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (trans.isCleared) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconButton(
                                        onClick = { showEditDialog = trans },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = { showDeleteDialog = trans },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Manager CRUD dialog: EDIT transaction
    if (showEditDialog != null) {
        val trans = showEditDialog!!
        var editType by remember { mutableStateOf(trans.type) }
        var editCat by remember { mutableStateOf(trans.expenseCategory ?: "SHOP") }
        var editAmount by remember { mutableStateOf(trans.amount.toInt().toString()) }
        var editDesc by remember { mutableStateOf(trans.description) }

        AlertDialog(
            onDismissRequest = { showEditDialog = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("تعديل العملية المالية", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { editType = "INCOME" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = if (editType == "INCOME") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                        ) {
                            Text("دخل", style = MaterialTheme.typography.labelSmall, color = if (editType == "INCOME") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(
                            onClick = { editType = "EXPENSE" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = if (editType == "EXPENSE") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline)
                        ) {
                            Text("خرج", style = MaterialTheme.typography.labelSmall, color = if (editType == "EXPENSE") MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    if (editType == "EXPENSE") {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { editCat = "SHOP" },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = if (editCat == "SHOP") MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline)
                            ) { Text("محل", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface) }
                            Button(
                                onClick = { editCat = "PERSONAL" },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = if (editCat == "PERSONAL") MaterialTheme.colorScheme.error.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline)
                            ) { Text("شخصي", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface) }
                        }
                    }

                    OutlinedTextField(
                        value = editAmount,
                        onValueChange = { editAmount = it },
                        label = { Text("المبلغ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface)
                    )

                    OutlinedTextField(
                        value = editDesc,
                        onValueChange = { editDesc = it },
                        label = { Text("البيان") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = editAmount.toDoubleOrNull() ?: trans.amount
                        viewModel.managerUpdateTransaction(
                            trans.copy(
                                type = editType,
                                expenseCategory = if (editType == "EXPENSE") editCat else null,
                                amount = amt,
                                description = editDesc
                            )
                        )
                        showEditDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                ) { Text("تحديث الحفظ") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = null }) { Text("إلغاء", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        )
    }

    // Modal Manager CRUD dialog: DELETE confirmation
    if (showDeleteDialog != null) {
        val trans = showDeleteDialog!!
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("تأكيد الحذف النهائي؟", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
            text = { Text("هل أنت متأكد من رغبتك في حذف العملية المالية من سجل الموظف ${trans.employeeName} بقيمة (${trans.amount.toInt()} ر.ي)؟ لا يمكن استرجاع هذا السجل قسرياً.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.managerDeleteTransaction(trans.id)
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError)
                ) { Text("نعم، احذف") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text("إلغاء", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        )
    }

    // Modal Manager CRUD dialog: ADD manual transaction
    if (showAddManualDialog) {
        var trEmpId by remember { mutableStateOf<Int?>(null) }
        var trType by remember { mutableStateOf("INCOME") }
        var trCat by remember { mutableStateOf("SHOP") }
        var trAmount by remember { mutableStateOf("") }
        var trDesc by remember { mutableStateOf("") }
        var trDate by remember { mutableStateOf(viewModel.getTodayDateString()) }

        val employeesOnly = allUsers.filter { it.role == "EMPLOYEE" }

        AlertDialog(
            onDismissRequest = { showAddManualDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("قيد عملية يدوية اختيارية", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("اختر الموظف المستهدف:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        employeesOnly.forEach { em ->
                            FilterChip(
                                selected = trEmpId == em.id,
                                onClick = { trEmpId = em.id },
                                label = { Text(em.name, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { trType = "INCOME" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = if (trType == "INCOME") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                        ) { Text("دخل", style = MaterialTheme.typography.labelSmall, color = if (trType == "INCOME") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant) }
                        Button(
                            onClick = { trType = "EXPENSE" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = if (trType == "EXPENSE") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline)
                        ) { Text("خرج", style = MaterialTheme.typography.labelSmall, color = if (trType == "EXPENSE") MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurfaceVariant) }
                    }

                    if (trType == "EXPENSE") {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { trCat = "SHOP" },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = if (trCat == "SHOP") MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline)
                            ) { Text("مقتنيات محل", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface) }
                            Button(
                                onClick = { trCat = "PERSONAL" },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = if (trCat == "PERSONAL") MaterialTheme.colorScheme.error.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline)
                            ) { Text("شخصي", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface) }
                        }
                    }

                    OutlinedTextField(
                        value = trAmount,
                        onValueChange = { trAmount = it },
                        label = { Text("المبلغ (ريال يمني)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface)
                    )

                    OutlinedTextField(
                        value = trDesc,
                        onValueChange = { trDesc = it },
                        label = { Text("البيان والتعليق") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface)
                    )

                    OutlinedTextField(
                        value = trDate,
                        onValueChange = { trDate = it },
                        label = { Text("تاريخ العملية YYYY-MM-DD") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetId = trEmpId
                        val amt = trAmount.toDoubleOrNull() ?: 0.0
                        if (targetId != null && amt > 0.0) {
                            viewModel.managerAddTransactionForEmployee(
                                employeeId = targetId,
                                type = trType,
                                amount = amt,
                                description = trDesc,
                                expenseCategory = if (trType == "EXPENSE") trCat else null,
                                dateStr = trDate
                            )
                        }
                        showAddManualDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                ) { Text("إدراج قسري") }
            },
            dismissButton = {
                TextButton(onClick = { showAddManualDialog = false }) { Text("إلغاء", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        )
    }
}

// ==========================================
// TAB 3: EMPLOYEES ACCOUNTS & SHOP LBS POSITION
// ==========================================
@Composable
fun TabEmployeesAndGeo(
    viewModel: ShopViewModel,
    allUsers: List<UserEntity>
) {
    var showAddEmployee by remember { mutableStateOf(false) }
    var editEmployee by remember { mutableStateOf<UserEntity?>(null) }

    val employees = allUsers.filter { it.role == "EMPLOYEE" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "إدارة شؤون الحسابات وهيكل الأجور والموقع",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Button(
                onClick = { showAddEmployee = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("أضف موظف", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }
            }
        }

        // List Employees Accounts
        employees.forEach { emp ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(emp.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                            Text("هاتف: ${emp.phone}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(onClick = { editEmployee = emp }) {
                                Icon(Icons.Default.ManageAccounts, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { viewModel.managerDeleteEmployee(emp.id) }) {
                                Icon(Icons.Default.DeleteForever, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Salary structures
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("الراتب الدوامي الأساسي اليومي", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${emp.baseSalary.toInt()} ر.ي", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                        }
                        Column {
                            Text("نسبة العقد (من دخل اليوم)", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${emp.commissionPercent.toInt()}% نسبة", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                        }
                        Column {
                            Text("رمز المرور السري الخاص", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(emp.password, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Virtual Coordinate and Boundary config (supports Shop geofence restriction)
        Text(
            text = "تثبيت وتحديد الموقع الجغرافي المعتمد للمحل:",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val currentLocName by viewModel.shopLocationName.collectAsState()

                Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))

                Text("تحديد النطاق لصالون الحلاقة لفرض قيود تسجيل الحضور والغياب:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                var inputLocName by remember { mutableStateOf(currentLocName) }

                OutlinedTextField(
                    value = inputLocName,
                    onValueChange = { inputLocName = it },
                    label = { Text("وصف احداثية موقع الصالون الجغرافي") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { viewModel.updateShopLocation(inputLocName) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("حفظ وتحديث ومحاكاة النطاق الجغرافي للصالون", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }

                Text(
                    text = "سيقوم التطبيق تلقائياً بالتحقق من مطابقة جهاز الموظف مع هذا النظاق لمنع أي محاولة لتسجيل الدخول من خارج أوقات وموقع العمل المسجل.",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start
                )
            }
        }
    }

    // Modal Create Employee account
    if (showAddEmployee) {
        var newName by remember { mutableStateOf("") }
        var newPhone by remember { mutableStateOf("") }
        var newPass by remember { mutableStateOf("") }
        var newBase by remember { mutableStateOf("") }
        var newComm by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddEmployee = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("إنشاء حساب موظف جديد", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("اسم الموظف") })
                    OutlinedTextField(value = newPhone, onValueChange = { newPhone = it }, label = { Text("رقم الهاتف") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                    OutlinedTextField(value = newPass, onValueChange = { newPass = it }, label = { Text("كلمة المرور") })
                    OutlinedTextField(value = newBase, onValueChange = { newBase = it }, label = { Text("الراتب اليومي ر.ي") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(value = newComm, onValueChange = { newComm = it }, label = { Text("نسبة العمولة %") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val base = newBase.toDoubleOrNull() ?: 0.0
                        val comm = newComm.toDoubleOrNull() ?: 0.0
                        if (newName.isNotEmpty() && newPhone.isNotEmpty() && newPass.isNotEmpty()) {
                            viewModel.managerCreateEmployee(newName, newPhone, newPass, base, comm)
                        }
                        showAddEmployee = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                ) { Text("إنشاء وتفعيل") }
            },
            dismissButton = {
                TextButton(onClick = { showAddEmployee = false }) { Text("إلغاء", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        )
    }

    // Modal Edit Employee details (Reset password, change phone number)
    if (editEmployee != null) {
        val emp = editEmployee!!
        var eName by remember { mutableStateOf(emp.name) }
        var ePhone by remember { mutableStateOf(emp.phone) }
        var ePass by remember { mutableStateOf(emp.password) }
        var eBase by remember { mutableStateOf(emp.baseSalary.toInt().toString()) }
        var eComm by remember { mutableStateOf(emp.commissionPercent.toInt().toString()) }
        var eActive by remember { mutableStateOf(emp.isActive) }

        AlertDialog(
            onDismissRequest = { editEmployee = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("تعديل حساب موظف", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = eName, onValueChange = { eName = it }, label = { Text("الاسم") })
                    OutlinedTextField(value = ePhone, onValueChange = { ePhone = it }, label = { Text("تعديل رقم الهاتف") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                    OutlinedTextField(value = ePass, onValueChange = { ePass = it }, label = { Text("إعادة تعيين كلمة السر") })
                    OutlinedTextField(value = eBase, onValueChange = { eBase = it }, label = { Text("الراتب اليومي ر.ي") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(value = eComm, onValueChange = { eComm = it }, label = { Text("النسبة %") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("الحساب نشط ويسمح له بالعمل", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                        Switch(checked = eActive, onCheckedChange = { eActive = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val base = eBase.toDoubleOrNull() ?: emp.baseSalary
                        val comm = eComm.toDoubleOrNull() ?: emp.commissionPercent
                        viewModel.managerUpdateEmployee(emp.id, eName, ePhone, ePass, base, comm, eActive)
                        editEmployee = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                ) { Text("حفظ التغييرات") }
            },
            dismissButton = {
                TextButton(onClick = { editEmployee = null }) { Text("إلغاء", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        )
    }
}
