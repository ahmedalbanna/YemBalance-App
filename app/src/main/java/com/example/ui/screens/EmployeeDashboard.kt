package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import com.example.data.model.TransactionEntity
import com.example.data.model.UserEntity
import com.example.ui.ShopViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeDashboard(
    viewModel: ShopViewModel,
    user: UserEntity,
    onLogout: () -> Unit
) {
    val allTransactions by viewModel.allTransactions.collectAsState()
    val syncProgress by viewModel.syncProgress.collectAsState()
    val syncStatusText by viewModel.syncStatusText.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf("ALL") } // ALL, INCOME, SHOP_EXP, PERSONAL_EXP

    // Filter transactions logged by this employee today
    val todayStr = viewModel.getTodayDateString()
    val myTransactionsToday = remember(allTransactions, todayStr, user.id) {
        allTransactions.filter {
            it.employeeId == user.id && it.shiftDate == todayStr
        }
    }

    // Calculations based on user stories
    val totalIncome = remember(myTransactionsToday) {
        myTransactionsToday.filter { it.type == "INCOME" }.sumOf { it.amount }
    }
    val totalShopExpense = remember(myTransactionsToday) {
        myTransactionsToday.filter { it.type == "EXPENSE" && it.expenseCategory == "SHOP" }.sumOf { it.amount }
    }
    val totalPersonalExpense = remember(myTransactionsToday) {
        myTransactionsToday.filter { it.type == "EXPENSE" && it.expenseCategory == "PERSONAL" }.sumOf { it.amount }
    }

    // Formula: BaseSalary + (totalIncome * percent / 100) - totalPersonalExpense
    val commissionEarned = (totalIncome * user.commissionPercent) / 100.0
    val netSalaryEarned = user.baseSalary + commissionEarned - totalPersonalExpense

    // State to check if some transactions today are uncleared (needs settlement)
    val hasUnsettledTransactions = remember(myTransactionsToday) {
        myTransactionsToday.any { !it.isCleared }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "مرحباً، ${user.name}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextLight
                        )
                        Text(
                            text = "موظف صالون حلاقة • اليوم: $todayStr",
                            color = TextMuted,
                            fontSize = 12.sp
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = NeonTeal,
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة حركة مالية")
            }
        },
        containerColor = CyberObsidian
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {

            // Sync simulation status bar indicator
            AnimatedVisibility(visible = syncProgress != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .background(CardSlate, RoundedCornerShape(8.dp))
                        .border(1.dp, NeonTeal.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = syncStatusText ?: "جاري المزامنة...",
                        fontSize = 13.sp,
                        color = NeonTeal,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (syncProgress ?: 0) / 100f },
                        color = NeonTeal,
                        trackColor = Color.DarkGray,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Earnings Stats Panel Card (Exactly representing Ahmed's/Ali's/Osama's story metrics)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderDark, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = CardSlate)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ملخص الوردية اليومية",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextLight
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        if (hasUnsettledTransactions) AccentYellow else DarkGreen,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                            )
                            Text(
                                text = if (hasUnsettledTransactions) "بانتظار تصفية المدير" else "تم تصفية حسابك من جلال",
                                fontSize = 11.sp,
                                color = if (hasUnsettledTransactions) AccentYellow else DarkGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Calculations display
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Total Income
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text("إجمالي الدخل", fontSize = 11.sp, color = TextMuted)
                            Text("${totalIncome.toInt()} ر.ي", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DarkGreen)
                        }
                        // Shop Expense
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text("مصاريف محل (لا تخصم)", fontSize = 11.sp, color = TextMuted)
                            Text("${totalShopExpense.toInt()} ر.ي", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextLight)
                        }
                        // Personal Expense
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text("مسحوبات شخصية (تخصم)", fontSize = 11.sp, color = TextMuted)
                            Text("${totalPersonalExpense.toInt()} ر.ي", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DarkRed)
                        }
                    }

                    HorizontalDivider(thickness = 1.dp, color = BorderDark, modifier = Modifier.padding(vertical = 12.dp))

                    // Live Net Payout calculation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("صافي مستحقات راتبك لليوم:", fontSize = 12.sp, color = TextMuted)
                            Row {
                                Text(
                                    text = "الأساسي (${user.baseSalary.toInt()}) + عمولة ${user.commissionPercent.toInt()}% (${commissionEarned.toInt()})",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }
                        Text(
                            text = "${netSalaryEarned.toInt()} ر.ي",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = NeonTeal
                        )
                    }

                    // Simulated sync trigger buttons
                    if (syncProgress == null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.triggerSyncSimulation("WiFi") },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = BorderDark),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Wifi, contentDescription = null, tint = NeonTeal, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تزامن واي فاي", fontSize = 11.sp, color = textMutedHelper())
                            }
                            Button(
                                onClick = { viewModel.triggerSyncSimulation("Bluetooth") },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = BorderDark),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Bluetooth, contentDescription = null, tint = NeonTeal, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تزامن بلوتوث", fontSize = 11.sp, color = textMutedHelper())
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search Header & Type Switcher Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("بحث في الحركات المالية...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonTeal,
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = TextLight
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            // Category choice horizontal chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val filterOptions = listOf(
                    Triple("ALL", "الكل", Icons.Default.List),
                    Triple("INCOME", "الدخل (عمل)", Icons.Default.TrendingUp),
                    Triple("SHOP_EXP", "مصارف محل", Icons.Default.Store),
                    Triple("PERSONAL_EXP", "مصروف شخصي", Icons.Default.Person)
                )

                filterOptions.forEach { (type, label, icon) ->
                    val isSelected = filterType == type
                    FilterChip(
                        selected = isSelected,
                        onClick = { filterType = type },
                        label = { Text(label, fontSize = 11.sp) },
                        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonTeal.copy(alpha = 0.2f),
                            selectedLabelColor = NeonTeal,
                            containerColor = CardSlate,
                            labelColor = TextMuted,
                            iconColor = TextMuted
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (isSelected) NeonTeal else BorderDark,
                            selectedBorderColor = NeonTeal,
                            enabled = true,
                            selected = isSelected
                        )
                    )
                }
            }

            // Filtered results list
            val filteredTransactions = remember(myTransactionsToday, searchQuery, filterType) {
                myTransactionsToday.filter { item ->
                    // Search constraint
                    val matchesSearch = item.description.contains(searchQuery, ignoreCase = true)

                    // Type constraint
                    val matchesType = when (filterType) {
                        "INCOME" -> item.type == "INCOME"
                        "SHOP_EXP" -> item.type == "EXPENSE" && item.expenseCategory == "SHOP"
                        "PERSONAL_EXP" -> item.type == "EXPENSE" && item.expenseCategory == "PERSONAL"
                        else -> true
                    }

                    matchesSearch && matchesType
                }
            }

            Text(
                text = "سجل حركات اليوم (${filteredTransactions.size})",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextLight,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (filteredTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.EventNote, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("لا يوجد حركات مسجلة تطابق التصفية لليوم.", fontSize = 13.sp, color = TextMuted)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredTransactions, key = { it.id }) { trans ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, if (trans.isCleared) BorderDark else NeonTeal.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
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
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                color = when (trans.type) {
                                                    "INCOME" -> DarkGreen.copy(alpha = 0.1f)
                                                    else -> if (trans.expenseCategory == "SHOP") Color.White.copy(alpha = 0.05f) else DarkRed.copy(alpha = 0.1f)
                                                },
                                                shape = RoundedCornerShape(10.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when (trans.type) {
                                                "INCOME" -> Icons.Default.TrendingUp
                                                else -> if (trans.expenseCategory == "SHOP") Icons.Default.Store else Icons.Default.Person
                                            },
                                            contentDescription = null,
                                            tint = when (trans.type) {
                                                "INCOME" -> DarkGreen
                                                else -> if (trans.expenseCategory == "SHOP") TextLight else DarkRed
                                            },
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = trans.description.ifEmpty {
                                                if (trans.type == "INCOME") "دخل عمل يدوي" else "خرج مصروف"
                                            },
                                            color = TextLight,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = when {
                                                    trans.type == "INCOME" -> "أجر اليوم"
                                                    trans.expenseCategory == "SHOP" -> "مصاريف المحل"
                                                    else -> "غداء شخصي خصم"
                                                },
                                                fontSize = 11.sp,
                                                color = TextMuted
                                            )
                                            Text("•", fontSize = 11.sp, color = TextMuted)
                                            Text(
                                                text = SimpleDateFormat("HH:mm", Locale.US).format(Date(trans.timestamp)),
                                                fontSize = 11.sp,
                                                color = TextMuted
                                            )
                                        }
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${if (trans.type == "INCOME") "+" else "-"}${trans.amount.toInt()} ر.ي",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = if (trans.type == "INCOME") DarkGreen else if (trans.expenseCategory == "SHOP") TextLight else DarkRed
                                    )
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 4.dp)
                                            .background(
                                                color = if (trans.isCleared) DarkGreen.copy(alpha = 0.15f) else AccentYellow.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (trans.isCleared) "مسوى" else "معلق",
                                            fontSize = 9.sp,
                                            color = if (trans.isCleared) DarkGreen else AccentYellow,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal dialogue to create entries manually
    if (showAddDialog) {
        var entryType by remember { mutableStateOf("INCOME") } // INCOME, EXPENSE
        var expenseCat by remember { mutableStateOf("SHOP") } // SHOP, PERSONAL
        var amountStr by remember { mutableStateOf("") }
        var descStr by remember { mutableStateOf("") }
        var inputErr by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = CardSlate,
            title = {
                Text(
                    text = "تسجيل حركة مالية جديدة",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextLight,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("نوع الحركة المالية:", fontSize = 12.sp, color = TextMuted)

                    // Type Choice tabs (Income vs Expense)
                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { entryType = "INCOME" },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (entryType == "INCOME") DarkGreen else BorderDark,
                                contentColor = if (entryType == "INCOME") Color.White else TextMuted
                            )
                        ) {
                            Text("دخل (إيرادات الزبائن)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { entryType = "EXPENSE" },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (entryType == "EXPENSE") DarkRed else BorderDark,
                                contentColor = if (entryType == "EXPENSE") Color.White else TextMuted
                            )
                        ) {
                            Text("خرج (مصروفات صالون)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // If Expense: choose type (Shop vs Personal)
                    if (entryType == "EXPENSE") {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("ربط وتصنيف الخارج المالي:", fontSize = 12.sp, color = TextMuted)
                        Row(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { expenseCat = "SHOP" },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (expenseCat == "SHOP") NeonTeal.copy(alpha = 0.2f) else BorderDark,
                                    contentColor = if (expenseCat == "SHOP") NeonTeal else TextMuted
                                ),
                                shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
                            ) {
                                Text("للمحل (لا يخصم من راتبي)", fontSize = 10.sp)
                            }
                            Button(
                                onClick = { expenseCat = "PERSONAL" },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (expenseCat == "PERSONAL") NeonTeal.copy(alpha = 0.2f) else BorderDark,
                                    contentColor = if (expenseCat == "PERSONAL") NeonTeal else TextMuted
                                ),
                                shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
                            ) {
                                Text("شخصي ومصروف غداء (يخصم)", fontSize = 10.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("المبلغ بالريال اليمني") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonTeal,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = descStr,
                        onValueChange = { descStr = it },
                        label = { Text("البيان والوصف (اختياري)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonTeal,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    AnimatedVisibility(visible = inputErr != null) {
                        Text(
                            text = inputErr ?: "",
                            color = DarkRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = amountStr.toDoubleOrNull()
                        if (amount == null || amount <= 0) {
                            inputErr = "الرجاء كـتابة مبلغ صالح أكبر من صفر."
                            return@Button
                        }
                        viewModel.addTransaction(
                            type = entryType,
                            amount = amount,
                            description = descStr.ifEmpty { if (entryType == "INCOME") "دخل خدمة يدوية" else "شراء مستلزمات" },
                            expenseCategory = if (entryType == "EXPENSE") expenseCat else null
                        )
                        showAddDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonTeal, contentColor = Color.White)
                ) {
                    Text("حفظ الحركة وتوثيقها")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("إلغاء", color = TextMuted)
                }
            }
        )
    }
}

@Composable
private fun textMutedHelper(): Color = TextLight
