package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.data.local.AppDatabase
import com.example.data.model.UserEntity
import com.example.data.repository.ShopRepository
import com.example.ui.ShopViewModel
import com.example.ui.ShopViewModelFactory
import com.example.ui.screens.EmployeeDashboard
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.ManagerDashboard
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: ShopViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize state-access dependencies
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = ShopRepository(
            userDao = database.userDao(),
            transactionDao = database.transactionDao(),
            settlementDao = database.settlementDao()
        )
        val factory = ShopViewModelFactory(application, repository)
        viewModel = ViewModelProvider(this, factory)[ShopViewModel::class.java]

        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Login) }

                // Safe wrapper scaffold handling edge to edge properly
                Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
                    when (val screen = currentScreen) {
                        is AppScreen.Login -> {
                            LoginScreen(
                                viewModel = viewModel,
                                onLoginSuccess = {
                                    val loggedInUser = viewModel.currentUser.value
                                    if (loggedInUser != null) {
                                        if (loggedInUser.role == "MANAGER") {
                                            currentScreen = AppScreen.Manager(loggedInUser)
                                        } else {
                                            currentScreen = AppScreen.Employee(loggedInUser)
                                        }
                                    }
                                }
                            )
                        }
                        is AppScreen.Employee -> {
                            EmployeeDashboard(
                                viewModel = viewModel,
                                user = screen.user,
                                onLogout = {
                                    viewModel.logout()
                                    currentScreen = AppScreen.Login
                                }
                            )
                        }
                        is AppScreen.Manager -> {
                            ManagerDashboard(
                                viewModel = viewModel,
                                currentUser = screen.user,
                                onLogout = {
                                    viewModel.logout()
                                    currentScreen = AppScreen.Login
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

sealed class AppScreen {
    object Login : AppScreen()
    data class Employee(val user: UserEntity) : AppScreen()
    data class Manager(val user: UserEntity) : AppScreen()
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    androidx.compose.material3.Text(text = "Hello $name!", modifier = modifier)
}
