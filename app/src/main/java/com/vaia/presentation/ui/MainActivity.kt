package com.vaia.presentation.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vaia.VaiaApplication
import com.vaia.presentation.ui.activities.ActivitiesScreen
import com.vaia.presentation.ui.auth.LoginScreen
import com.vaia.presentation.ui.auth.RegisterScreen
import com.vaia.presentation.ui.documents.DocumentsScreen
import com.vaia.presentation.ui.documents.DocumentChecklistScreen
import com.vaia.presentation.ui.expenses.ExpensesScreen
import com.vaia.presentation.ui.profile.ProfileScreen
import com.vaia.presentation.ui.roadmap.RoadmapScreen
import com.vaia.presentation.ui.theme.VaiaTheme
import com.vaia.presentation.ui.trips.TripsScreen
import com.vaia.presentation.viewmodel.ActivitiesViewModel
import com.vaia.presentation.viewmodel.ActivitiesViewModelFactory
import com.vaia.presentation.viewmodel.AuthViewModel
import com.vaia.presentation.viewmodel.AuthViewModelFactory
import com.vaia.presentation.viewmodel.ExpensesViewModel
import com.vaia.presentation.viewmodel.ExpensesViewModelFactory
import com.vaia.presentation.viewmodel.TripsViewModel
import com.vaia.presentation.viewmodel.TripsViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("VAIA", "MainActivity onCreate called")
        setContent {
            VaiaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VaiaApp()
                }
            }
        }
    }
}

@Composable
fun VaiaApp() {
    val navController = rememberNavController()
    val application = androidx.compose.ui.platform.LocalContext.current.applicationContext as VaiaApplication
    val appContainer = application.appContainer

    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(appContainer.authRepository)
    )
    val tripsViewModel: TripsViewModel = viewModel(
        factory = TripsViewModelFactory(appContainer.tripRepository, appContainer.authRepository)
    )
    val startDestination = if (authViewModel.isLoggedIn()) "home" else "login"

    fun navigateToLogin() {
        navController.navigate("login") {
            popUpTo("home") { inclusive = true }
            launchSingleTop = true
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate("register")
                },
                viewModel = authViewModel
            )
        }
        composable("register") {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate("home") {
                        popUpTo("register") { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate("login")
                },
                viewModel = authViewModel
            )
        }
        composable("home") {
            LaunchedEffect(Unit) {
                if (!authViewModel.isLoggedIn()) navigateToLogin()
            }
            TripsScreen(
                onLogout = {
                    navigateToLogin()
                },
                onNavigateToActivities = { tripId ->
                    navController.navigate("activities/$tripId")
                },
                onNavigateHome = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateTrips = {
                    navController.navigate("home") {
                        launchSingleTop = true
                    }
                },
                onNavigateProfile = {
                    navController.navigate("profile") {
                        launchSingleTop = true
                    }
                },
                viewModel = tripsViewModel
            )
        }
        composable("activities/{tripId}") { backStackEntry ->
            LaunchedEffect(Unit) {
                if (!authViewModel.isLoggedIn()) navigateToLogin()
            }
            val tripId = backStackEntry.arguments?.getString("tripId") ?: ""
            val activitiesViewModel: ActivitiesViewModel = viewModel(
                factory = ActivitiesViewModelFactory(appContainer.activityRepository, tripId)
            )
            ActivitiesScreen(
                tripId = tripId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToExpenses = {
                    navController.navigate("expenses/$tripId")
                },
                onNavigateToRoadmap = {
                    navController.navigate("roadmap/$tripId")
                },
                onNavigateToDocuments = {
                    navController.navigate("documents/$tripId")
                },
                onNavigateHome = {
                    navController.navigate("home") { launchSingleTop = true }
                },
                onNavigateTrips = {
                    navController.navigate("home") { launchSingleTop = true }
                },
                onNavigateProfile = {
                    navController.navigate("profile") { launchSingleTop = true }
                },
                viewModel = activitiesViewModel
            )
        }
        composable("roadmap/{tripId}") { backStackEntry ->
            LaunchedEffect(Unit) {
                if (!authViewModel.isLoggedIn()) navigateToLogin()
            }
            val tripId = backStackEntry.arguments?.getString("tripId") ?: ""
            val activitiesViewModel: ActivitiesViewModel = viewModel(
                factory = ActivitiesViewModelFactory(appContainer.activityRepository, tripId)
            )
            RoadmapScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateHome = {
                    navController.navigate("home") { launchSingleTop = true }
                },
                onNavigateTrips = {
                    navController.navigate("home") { launchSingleTop = true }
                },
                onNavigateProfile = {
                    navController.navigate("profile") { launchSingleTop = true }
                },
                viewModel = activitiesViewModel
            )
        }
        composable("expenses/{tripId}") { backStackEntry ->
            LaunchedEffect(Unit) {
                if (!authViewModel.isLoggedIn()) navigateToLogin()
            }
            val tripId = backStackEntry.arguments?.getString("tripId") ?: ""
            val expensesViewModel: ExpensesViewModel = viewModel(
                factory = ExpensesViewModelFactory(appContainer.expenseRepository, tripId)
            )
            ExpensesScreen(
                tripId = tripId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateHome = {
                    navController.navigate("home") { launchSingleTop = true }
                },
                onNavigateTrips = {
                    navController.navigate("home") { launchSingleTop = true }
                },
                onNavigateProfile = {
                    navController.navigate("profile") { launchSingleTop = true }
                },
                viewModel = expensesViewModel
            )
        }
        composable("documents/{tripId}") { backStackEntry ->
            LaunchedEffect(Unit) {
                if (!authViewModel.isLoggedIn()) navigateToLogin()
            }
            val tripId = backStackEntry.arguments?.getString("tripId") ?: ""
            val tripTitle = "Trip Documents"
            DocumentChecklistScreen(
                tripId = tripId,
                tripTitle = tripTitle,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("checklist/{tripId}/{tripTitle}") { backStackEntry ->
            LaunchedEffect(Unit) {
                if (!authViewModel.isLoggedIn()) navigateToLogin()
            }
            val tripId = backStackEntry.arguments?.getString("tripId") ?: ""
            val tripTitle = backStackEntry.arguments?.getString("tripTitle") ?: ""
            DocumentChecklistScreen(
                tripId = tripId,
                tripTitle = tripTitle,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("profile") {
            LaunchedEffect(Unit) {
                if (!authViewModel.isLoggedIn()) navigateToLogin()
            }
            ProfileScreen(
                onNavigateHome = {
                    navController.navigate("home") { launchSingleTop = true }
                },
                onNavigateTrips = {
                    navController.navigate("home") { launchSingleTop = true }
                },
                onNavigateProfile = {
                    navController.navigate("profile") { launchSingleTop = true }
                },
                viewModel = authViewModel
            )
        }
    }
}
