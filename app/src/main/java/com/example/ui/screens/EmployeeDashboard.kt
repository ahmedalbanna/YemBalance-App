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
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.ui.ArabicNumerals
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
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "موظف صالون حلاقة • اليوم: ${ArabicNumerals.convert(todayStr)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "تسجيل خروج", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة حركة مالية")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
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
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = syncStatusText ?: "جاري المزامنة...",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (syncProgress ?: 0) / 100f },
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Earnings Stats Panel Card (Exactly representing Ahmed's/Ali's/Osama's story metrics)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        if (hasUnsettledTransactions) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                            )
                            Text(
                                text = if (hasUnsettledTransactions) "بانتظار تصفية المدير" else "تم تصفية حسابك من جلال",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (hasUnsettledTransactions) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
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
                            Text("إجمالي الدخل", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(ArabicNumerals.formatCurrency(totalIncome), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                        }
                        // Shop Expense
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text("مصاريف محل (لا تخصم)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(ArabicNumerals.formatCurrency(totalShopExpense), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                        }
                        // Personal Expense
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text("مسحوبات شخصية (تخصم)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(ArabicNumerals.formatCurrency(totalPersonalExpense), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.error)
                        }
                    }

                    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(vertical = 12.dp))

                    // Live Net Payout calculation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("صافي مستحقات راتبك لليوم:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row {
                                Text(
                                    text = "الأساسي (${ArabicNumerals.convert(user.baseSalary.toInt().toString())}) + عمولة ${ArabicNumerals.convert(user.commissionPercent.toInt().toString())}% (${ArabicNumerals.convert(commissionEarned.toInt().toString())})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = ArabicNumerals.formatCurrency(netSalaryEarned),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                            color = MaterialTheme.colorScheme.primary
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
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.outlineVariant),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Wifi, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تزامن واي فاي", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Button(
                                onClick = { viewModel.triggerSyncSimulation("Bluetooth") },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.outlineVariant),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Bluetooth, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تزامن بلوتوث", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
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
                    placeholder = { Text("بحث في الحركات المالية...", style = MaterialTheme.typography.bodyMedium) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface
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
                    Triple("ALL", "الكل", Icons.AutoMirrored.Filled.List),
                    Triple("INCOME", "الدخل (عمل)", Icons.AutoMirrored.Filled.TrendingUp),
                    Triple("SHOP_EXP", "مصارف محل", Icons.Default.Store),
                    Triple("PERSONAL_EXP", "مصروف شخصي", Icons.Default.Person)
                )

                filterOptions.forEach { (type, label, icon) ->
                    val isSelected = filterType == type
                    FilterChip(
                        selected = isSelected,
                        onClick = { filterType = type },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary,
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            selectedBorderColor = MaterialTheme.colorScheme.primary,
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
                text = "سجل حركات اليوم (${ArabicNumerals.convert(filteredTransactions.size.toString())})",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
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
                        Icon(Icons.AutoMirrored.Filled.EventNote, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("لا يوجد حركات مسجلة تطابق التصفية لليوم.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                .border(1.dp, if (trans.isCleared) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
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
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                color = when (trans.type) {
                                                    "INCOME" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                                    else -> if (trans.expenseCategory == "SHOP") MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f) else MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                                                },
                                                shape = RoundedCornerShape(10.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when (trans.type) {
                                                "INCOME" -> Icons.AutoMirrored.Filled.TrendingUp
                                                else -> if (trans.expenseCategory == "SHOP") Icons.Default.Store else Icons.Default.Person
                                            },
                                            contentDescription = null,
                                            tint = when (trans.type) {
                                                "INCOME" -> MaterialTheme.colorScheme.primary
                                                else -> if (trans.expenseCategory == "SHOP") MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                                            },
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = trans.description.ifEmpty {
                                                if (trans.type == "INCOME") "دخل عمل يدوي" else "خرج مصروف"
                                            },
                                            color = MaterialTheme.colorScheme.onSurface,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
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
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text("•", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                text = ArabicNumerals.convert(SimpleDateFormat("HH:mm", Locale.US).format(Date(trans.timestamp))),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${if (trans.type == "INCOME") "+" else "-"}${ArabicNumerals.formatCurrency(trans.amount)}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (trans.type == "INCOME") MaterialTheme.colorScheme.primary else if (trans.expenseCategory == "SHOP") MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                                    )
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 4.dp)
                                            .background(
                                                color = if (trans.isCleared) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (trans.isCleared) "مسوى" else "معلق",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (trans.isCleared) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
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
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = "تسجيل حركة مالية جديدة",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("نوع الحركة المالية:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    // Type Choice tabs (Income vs Expense)
                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { entryType = "INCOME" },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (entryType == "INCOME") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                contentColor = if (entryType == "INCOME") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text("دخل (إيرادات الزبائن)", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                        }
                        Button(
                            onClick = { entryType = "EXPENSE" },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (entryType == "EXPENSE") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant,
                                contentColor = if (entryType == "EXPENSE") MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text("خرج (مصروفات صالون)", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                        }
                    }

                    // If Expense: choose type (Shop vs Personal)
                    if (entryType == "EXPENSE") {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("ربط وتصنيف الخارج المالي:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { expenseCat = "SHOP" },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (expenseCat == "SHOP") MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outlineVariant,
                                    contentColor = if (expenseCat == "SHOP") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
                            ) {
                                Text("للمحل (لا يخصم من راتبي)", style = MaterialTheme.typography.labelSmall)
                            }
                            Button(
                                onClick = { expenseCat = "PERSONAL" },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (expenseCat == "PERSONAL") MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outlineVariant,
                                    contentColor = if (expenseCat == "PERSONAL") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
                            ) {
                                Text("شخصي ومصروف غداء (يخصم)", style = MaterialTheme.typography.labelSmall)
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
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = descStr,
                        onValueChange = { descStr = it },
                        label = { Text("البيان والوصف (اختياري)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    AnimatedVisibility(visible = inputErr != null) {
                        Text(
                            text = inputErr ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("حفظ الحركة وتوثيقها")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("إلغاء", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}

