package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ShopViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: ShopViewModel,
    onLoginSuccess: () -> Unit
) {
    val isUserInShopRange by viewModel.isUserInShopRange.collectAsState()
    val shopLocationName by viewModel.shopLocationName.collectAsState()

    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberObsidian)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // Decorative neon backdrop glow
        Box(
            modifier = Modifier
                .size(300.dp)
                .background(Brush.radialGradient(listOf(NeonTeal.copy(alpha = 0.08f), Color.Transparent)))
                .align(Alignment.TopCenter)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon
            Icon(
                imageVector = Icons.Default.Storefront,
                contentDescription = "المحل المالي",
                tint = NeonTeal,
                modifier = Modifier
                    .size(64.dp)
                    .padding(bottom = 8.dp)
            )

            // Header Arabic Text
            Text(
                text = "حسابات المحل المالي",
                fontSize = 26.sp,
                color = TextLight,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "نظام الموظفين والواردات اليومية المترابطة",
                fontSize = 14.sp,
                color = TextMuted,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            // Geofencing compliance validator card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .border(1.dp, if (isUserInShopRange) NeonTeal.copy(alpha = 0.3f) else DarkRed.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = CardSlate)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "مقياس التحقق الجغرافي (GPS)",
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                            Text(
                                text = if (isUserInShopRange) "✓ داخل النطاق الجغرافي للمحل" else "☠ خارج نطاق المحل الجغرافي!",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isUserInShopRange) NeonTeal else DarkRed
                            )
                        }
                        Switch(
                            checked = isUserInShopRange,
                            onCheckedChange = { viewModel.setLocationConstraint(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NeonTeal,
                                checkedTrackColor = NeonTeal.copy(alpha = 0.4f),
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = Color.DarkGray
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "الموقع المعتمد: $shopLocationName",
                        fontSize = 11.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Input Fields Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderDark, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = CardSlate)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "تسجيل الدخول",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextLight,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    // Phone textfield
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("رقم الهاتف (المنشأ من المدير)") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = NeonTeal) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonTeal,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Password textfield
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("كلمة المرور") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = NeonTeal) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = TextMuted
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonTeal,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Error text alert
                    AnimatedVisibility(visible = errorMessage != null) {
                        errorMessage?.let { msg ->
                            Text(
                                text = msg,
                                color = DarkRed,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }

                    // Log in button
                    Button(
                        onClick = {
                            if (phone.isBlank() || password.isBlank()) {
                                errorMessage = "الرجاء تعبئة جميع الحقول أولاً."
                                return@Button
                            }
                            isLoading = true
                            focusManager.clearFocus()
                            viewModel.login(
                                phone = phone.trim(),
                                password = password,
                                onSuccess = {
                                    isLoading = false
                                    errorMessage = null
                                    onLoginSuccess()
                                },
                                onError = { error ->
                                    isLoading = false
                                    errorMessage = error
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonTeal, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text(
                                text = "دخول آمن للمساعد المالي",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Preset Test Accounts Section
            Text(
                text = "🔑 اضغط للتجربة الفورية لقيم قصص المستخدمين:",
                fontSize = 11.sp,
                color = TextMuted,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Demo accounts clickable chips
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Ahmed & Ali
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                phone = "771111111"
                                password = "123"
                                errorMessage = null
                            },
                        colors = CardDefaults.cardColors(containerColor = CardSlate),
                        border = BorderStroke(1.dp, NeonTeal.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("أحمد (موظف)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonTeal)
                            Text("771111111", fontSize = 10.sp, color = TextMuted)
                        }
                    }
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                phone = "772222222"
                                password = "123"
                                errorMessage = null
                            },
                        colors = CardDefaults.cardColors(containerColor = CardSlate),
                        border = BorderStroke(1.dp, NeonTeal.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("علي (موظف)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonTeal)
                            Text("772222222", fontSize = 10.sp, color = TextMuted)
                        }
                    }
                }

                // Osama & Jalal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                phone = "773333333"
                                password = "123"
                                errorMessage = null
                            },
                        colors = CardDefaults.cardColors(containerColor = CardSlate),
                        border = BorderStroke(1.dp, NeonTeal.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("أسامة (موظف)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonTeal)
                            Text("773333333", fontSize = 10.sp, color = TextMuted)
                        }
                    }
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                phone = "770000000"
                                password = "admin"
                                errorMessage = null
                            },
                        colors = CardDefaults.cardColors(containerColor = CardSlate),
                        border = BorderStroke(1.dp, NeonBlue.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("جلال (المدير)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonBlue)
                            Text("770000000", fontSize = 10.sp, color = TextMuted)
                        }
                    }
                }
            }
        }
    }
}
