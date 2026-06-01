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
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextLight
                        )
                        Text(
                            text = "إدارة المحل وموثق الإيرادات",
                            fontSize = 12.sp,
                            color = NeonTeal
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "تسجيل خروج", tint = DarkRed)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CardSlate)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = CardSlate,
                tonalElevation = 8.dp
            ) {
                tabs.forEach { (label, icon, index) ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = NeonTeal,
                            indicatorColor = NeonTeal,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        )
                    )
                }
            }
        },
        containerColor = CyberObsidian
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
                        containerColor = if (isSelected) NeonTeal else CardSlate,
                        contentColor = if (isSelected) Color.White else TextMuted
                    ),
                    border = BorderStroke(1.dp, if (isSelected) NeonTeal else BorderDark)
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
                .border(1.dp, BorderDark, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = CardSlate)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "الحالة المالية للمحل (${if (rangeFilter == "DAY") "اليوم" else if (rangeFilter == "MONTH") "هذا الشهر" else "هذا العام"})",
                    fontSize = 14.sp,
                    color = TextMuted,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("إجمالي الدخل (خدمات)", fontSize = 11.sp, color = TextMuted)
                        Text("${totalIncome.toInt()} ر.ي", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DarkGreen)
                    }
                    Column {
                        Text("مصاريف المحل مقتنيات", fontSize = 11.sp, color = TextMuted)
                        Text("${totalShopExpense.toInt()} ر.ي", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextLight)
                    }
                    Column {
                        Text("سلف غذاء شخصي", fontSize = 11.sp, color = TextMuted)
                        Text("${totalPersonalExpense.toInt()} ر.ي", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DarkRed)
                    }
                }

                HorizontalDivider(thickness = 1.dp, color = BorderDark, modifier = Modifier.padding(vertical = 12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("صافي الأرباح النقدية المودعة:", fontSize = 13.sp, color = TextMuted)
                    Text(
                        text = "${netRevenue.toInt()} ريال",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = NeonTeal
                    )
                }
            }
        }

        // Active Employee Check Indicators Today (satisfies: "المدير لديه علم بالموظفين النشطين")
        Text(
            text = "الموظفين النشطين اليوم وتتبع حركتهم:",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextLight
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
                        if (isActive) NeonTeal.copy(alpha = 0.3f) else BorderDark,
                        RoundedCornerShape(12.dp)
                    ),
                colors = CardDefaults.cardColors(containerColor = CardSlate)
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
                                        if (isActive) NeonTeal.copy(alpha = 0.1f) else Color.DarkGray,
                                        shape = RoundedCornerShape(21.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = emp.name.take(1),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = if (isActive) NeonTeal else TextMuted
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(11.dp)
                                    .border(1.5.dp, CardSlate, RoundedCornerShape(5.5.dp))
                                    .background(
                                        if (isActive) DarkGreen else Color.Gray,
                                        shape = RoundedCornerShape(5.5.dp)
                                    )
                            )
                        }

                        Column {
                            Text(emp.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextLight)
                            Text(
                                "تبيان اليوم • ${empTransToday.size} حركات مسجلة",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }

                    // Tally indicators for the specific employee to view on-demand
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (isActive) "دخل: +${empIncome.toInt()} ر.ي" else "خارج الدوام",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) DarkGreen else TextMuted
                        )
                        if (isActive) {
                            Text(
                                text = "خرج: -${empExp.toInt()} ر.ي",
                                fontSize = 11.sp,
                                color = DarkRed
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
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = TextLight
        )

        AnimatedVisibility(visible = showSuccessBanner != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkGreen.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, DarkGreen)
            ) {
                Text(
                    text = showSuccessBanner ?: "",
                    color = DarkGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
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
                            if (alreadySettled) DarkGreen.copy(alpha = 0.4f) else BorderDark,
                            RoundedCornerShape(12.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = CardSlate)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = emp.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = TextLight
                            )

                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (alreadySettled) DarkGreen.copy(alpha = 0.15f) else Color.DarkGray,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (alreadySettled) "✓ تمت التسوية والقبول" else "⌛ معلق ونشط",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (alreadySettled) DarkGreen else AccentYellow
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Arithmetic breakdown rows
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("العمليات المسجلة اليوم:", fontSize = 11.sp, color = TextMuted)
                            Text("${myTransactionsToday.size} حركات", fontSize = 11.sp, color = TextLight, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("مجموع دخل الوردية (الزبائن):", fontSize = 11.sp, color = TextMuted)
                            Text("+${totalIncome.toInt()} ر.ي", fontSize = 11.sp, color = DarkGreen)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("مصاريف محل (مسددة من الموظف):", fontSize = 11.sp, color = TextMuted)
                            Text("-${totalShopExpense.toInt()} ر.ي (لا تخصم)", fontSize = 11.sp, color = TextLight)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("سلف غذاء المسحوبة للموظف :", fontSize = 11.sp, color = TextMuted)
                            Text("-${totalPersonalExpense.toInt()} ر.ي (تخصم)", fontSize = 11.sp, color = DarkRed)
                        }

                        HorizontalDivider(thickness = 0.5.dp, color = BorderDark, modifier = Modifier.padding(vertical = 8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("المبلغ النقدي الواجب استلامه في الدرج:", fontSize = 11.sp, color = AccentYellow)
                                Text("(صافي إيراد الدرج المطلوب تسليمه لك)", fontSize = 8.sp, color = TextMuted)
                            }
                            Text(
                                text = "${cashierYield.toInt()} ر.ي",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentYellow
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("مستحقات راتب وعمولة الموظف لليوم:", fontSize = 11.sp, color = NeonTeal)
                                Text("(الراتب اليومي + النسبة - السُلف)", fontSize = 8.sp, color = TextMuted)
                            }
                            Text(
                                text = "${netCalculatedSalary.toInt()} ر.ي",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonTeal
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
                                colors = ButtonDefaults.buttonColors(containerColor = NeonTeal, contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.PriceCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("تصفية الحساب واستلام الإيرادات النقدي", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextLight
            )

            IconButton(onClick = { showAddManualDialog = true }) {
                Icon(Icons.Default.PostAdd, contentDescription = "قيد حركة مالية", tint = NeonTeal)
            }
        }

        // Filters Container Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderDark, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = CardSlate)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Text Search input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ابحث باسم الموظف أو البيان...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonTeal,
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = Color.White
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
                        placeholder = { Text("التاريخ كـ YYYY-MM-DD", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonTeal,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = Color.White
                        ),
                        modifier = Modifier.weight(1.2f)
                    )

                    // Reset button for date filter
                    if (searchDate.isNotEmpty()) {
                        IconButton(onClick = { searchDate = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "تصفية التاريخ", tint = DarkRed)
                        }
                    }
                }

                // Horizontal list of employee filter options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("موظف:", fontSize = 10.sp, color = TextMuted)
                    val employees = allUsers.filter { it.role == "EMPLOYEE" }

                    // All chip option
                    FilterChip(
                        selected = selectedEmployeeId == null,
                        onClick = { selectedEmployeeId = null },
                        label = { Text("الكل", fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonTeal.copy(alpha = 0.2f),
                            selectedLabelColor = NeonTeal,
                            containerColor = BorderDark,
                            labelColor = TextMuted
                        )
                    )

                    employees.forEach { emp ->
                        FilterChip(
                            selected = selectedEmployeeId == emp.id,
                            onClick = { selectedEmployeeId = emp.id },
                            label = { Text(emp.name, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonTeal.copy(alpha = 0.2f),
                                selectedLabelColor = NeonTeal,
                                containerColor = BorderDark,
                                labelColor = TextMuted
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
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextLight
            )

            // Dynamic Excel-style exporter preview
            Text(
                text = "قيد التعديل متاح للمدير ✒",
                fontSize = 11.sp,
                color = TextMuted
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
                            .border(1.dp, BorderDark, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = CardSlate)
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
                                                color = if (trans.type == "INCOME") DarkGreen else DarkRed,
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                    )
                                    Text(
                                        text = trans.employeeName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = NeonTeal
                                    )
                                    Text(
                                        text = "• ${trans.shiftDate}",
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }

                                Text(
                                    text = "${if (trans.type == "INCOME") "+" else "-"}${trans.amount.toInt()} ر.ي",
                                    fontWeight = FontWeight.Bold,
                                    color = if (trans.type == "INCOME") DarkGreen else if (trans.expenseCategory == "SHOP") TextLight else DarkRed,
                                    fontSize = 14.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = trans.description,
                                fontSize = 13.sp,
                                color = TextLight,
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
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                    Text("•", fontSize = 11.sp, color = TextMuted)
                                    Text(
                                        text = if (trans.isCleared) "مسوى نقداً ✓" else "⌛ معلق",
                                        fontSize = 11.sp,
                                        color = if (trans.isCleared) DarkGreen else AccentYellow,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconButton(
                                        onClick = { showEditDialog = trans },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = NeonBlue, modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = { showDeleteDialog = trans },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = DarkRed, modifier = Modifier.size(16.dp))
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
            containerColor = CardSlate,
            title = { Text("تعديل العملية المالية", color = TextLight, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { editType = "INCOME" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = if (editType == "INCOME") DarkGreen else BorderDark)
                        ) {
                            Text("دخل", fontSize = 11.sp, color = Color.White)
                        }
                        Button(
                            onClick = { editType = "EXPENSE" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = if (editType == "EXPENSE") DarkRed else BorderDark)
                        ) {
                            Text("خرج", fontSize = 11.sp, color = Color.White)
                        }
                    }

                    if (editType == "EXPENSE") {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { editCat = "SHOP" },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = if (editCat == "SHOP") NeonTeal.copy(alpha = 0.3f) else BorderDark)
                            ) { Text("محل", fontSize = 10.sp, color = Color.White) }
                            Button(
                                onClick = { editCat = "PERSONAL" },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = if (editCat == "PERSONAL") DarkRed.copy(alpha = 0.3f) else BorderDark)
                            ) { Text("شخصي", fontSize = 10.sp, color = Color.White) }
                        }
                    }

                    OutlinedTextField(
                        value = editAmount,
                        onValueChange = { editAmount = it },
                        label = { Text("المبلغ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight)
                    )

                    OutlinedTextField(
                        value = editDesc,
                        onValueChange = { editDesc = it },
                        label = { Text("البيان") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight)
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
                    colors = ButtonDefaults.buttonColors(containerColor = NeonTeal, contentColor = Color.White)
                ) { Text("تحديث الحفظ") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = null }) { Text("إلغاء", color = TextMuted) }
            }
        )
    }

    // Modal Manager CRUD dialog: DELETE confirmation
    if (showDeleteDialog != null) {
        val trans = showDeleteDialog!!
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            containerColor = CardSlate,
            title = { Text("تأكيد الحذف النهائي؟", color = TextLight, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = { Text("هل أنت متأكد من رغبتك في حذف العملية المالية من سجل الموظف ${trans.employeeName} بقيمة (${trans.amount.toInt()} ر.ي)؟ لا يمكن استرجاع هذا السجل قسرياً.", color = TextMuted, fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.managerDeleteTransaction(trans.id)
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkRed, contentColor = Color.White)
                ) { Text("نعم، احذف") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text("إلغاء", color = TextMuted) }
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
            containerColor = CardSlate,
            title = { Text("قيد عملية يدوية اختيارية", color = TextLight, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("اختر الموظف المستهدف:", fontSize = 11.sp, color = TextMuted)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        employeesOnly.forEach { em ->
                            FilterChip(
                                selected = trEmpId == em.id,
                                onClick = { trEmpId = em.id },
                                label = { Text(em.name, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonTeal.copy(alpha = 0.3f),
                                    selectedLabelColor = NeonTeal
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { trType = "INCOME" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = if (trType == "INCOME") DarkGreen else BorderDark)
                        ) { Text("دخل", fontSize = 11.sp) }
                        Button(
                            onClick = { trType = "EXPENSE" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = if (trType == "EXPENSE") DarkRed else BorderDark)
                        ) { Text("خرج", fontSize = 11.sp) }
                    }

                    if (trType == "EXPENSE") {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { trCat = "SHOP" },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = if (trCat == "SHOP") NeonTeal.copy(alpha = 0.3f) else BorderDark)
                            ) { Text("مقتنيات محل", fontSize = 10.sp) }
                            Button(
                                onClick = { trCat = "PERSONAL" },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = if (trCat == "PERSONAL") DarkRed.copy(alpha = 0.3f) else BorderDark)
                            ) { Text("شخصي", fontSize = 10.sp) }
                        }
                    }

                    OutlinedTextField(
                        value = trAmount,
                        onValueChange = { trAmount = it },
                        label = { Text("المبلغ (ريال يمني)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight)
                    )

                    OutlinedTextField(
                        value = trDesc,
                        onValueChange = { trDesc = it },
                        label = { Text("البيان والتعليق") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight)
                    )

                    OutlinedTextField(
                        value = trDate,
                        onValueChange = { trDate = it },
                        label = { Text("تاريخ العملية YYYY-MM-DD") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight)
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
                    colors = ButtonDefaults.buttonColors(containerColor = NeonTeal, contentColor = Color.White)
                ) { Text("إدراج قسري") }
            },
            dismissButton = {
                TextButton(onClick = { showAddManualDialog = false }) { Text("إلغاء", color = TextMuted) }
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
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextLight
            )

            Button(
                onClick = { showAddEmployee = true },
                colors = ButtonDefaults.buttonColors(containerColor = NeonTeal, contentColor = Color.White)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("أضف موظف", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // List Employees Accounts
        employees.forEach { emp ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderDark, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = CardSlate)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(emp.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextLight)
                            Text("هاتف: ${emp.phone}", fontSize = 12.sp, color = TextMuted)
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(onClick = { editEmployee = emp }) {
                                Icon(Icons.Default.ManageAccounts, contentDescription = "تعديل", tint = NeonTeal)
                            }
                            IconButton(onClick = { viewModel.managerDeleteEmployee(emp.id) }) {
                                Icon(Icons.Default.DeleteForever, contentDescription = "حذف", tint = DarkRed)
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
                            Text("الراتب الدوامي الأساسي اليومي", fontSize = 10.sp, color = TextMuted)
                            Text("${emp.baseSalary.toInt()} ر.ي", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextLight)
                        }
                        Column {
                            Text("نسبة العقد (من دخل اليوم)", fontSize = 10.sp, color = TextMuted)
                            Text("${emp.commissionPercent.toInt()}% نسبة", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NeonTeal)
                        }
                        Column {
                            Text("رمز المرور السري الخاص", fontSize = 10.sp, color = TextMuted)
                            Text(emp.password, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AccentYellow)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Virtual Coordinate and Boundary config (supports Shop geofence restriction)
        Text(
            text = "تثبيت وتحديد الموقع الجغرافي المعتمد للمحل:",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextLight
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderDark, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = CardSlate)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val currentLocName by viewModel.shopLocationName.collectAsState()

                Icon(Icons.Default.LocationOn, contentDescription = null, tint = NeonTeal, modifier = Modifier.size(36.dp))

                Text("تحديد النطاق لصالون الحلاقة لفرض قيود تسجيل الحضور والغياب:", fontSize = 12.sp, color = TextMuted)

                var inputLocName by remember { mutableStateOf(currentLocName) }

                OutlinedTextField(
                    value = inputLocName,
                    onValueChange = { inputLocName = it },
                    label = { Text("وصف احداثية موقع الصالون الجغرافي") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonTeal,
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { viewModel.updateShopLocation(inputLocName) },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonTeal, contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("حفظ وتحديث ومحاكاة النطاق الجغرافي للصالون", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = "سيقوم التطبيق تلقائياً بالتحقق من مطابقة جهاز الموظف مع هذا النظاق لمنع أي محاولة لتسجيل الدخول من خارج أوقات وموقع العمل المسجل.",
                    fontSize = 10.sp,
                    color = TextMuted,
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
            containerColor = CardSlate,
            title = { Text("إنشاء حساب موظف جديد", color = TextLight, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
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
                    colors = ButtonDefaults.buttonColors(containerColor = NeonTeal, contentColor = Color.White)
                ) { Text("إنشاء وتفعيل") }
            },
            dismissButton = {
                TextButton(onClick = { showAddEmployee = false }) { Text("إلغاء", color = TextMuted) }
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
            containerColor = CardSlate,
            title = { Text("تعديل حساب موظف", color = TextLight, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
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
                        Text("الحساب نشط ويسمح له بالعمل", color = TextMuted, fontSize = 12.sp)
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
                    colors = ButtonDefaults.buttonColors(containerColor = NeonTeal, contentColor = Color.White)
                ) { Text("حفظ التغييرات") }
            },
            dismissButton = {
                TextButton(onClick = { editEmployee = null }) { Text("إلغاء", color = TextMuted) }
            }
        )
    }
}
