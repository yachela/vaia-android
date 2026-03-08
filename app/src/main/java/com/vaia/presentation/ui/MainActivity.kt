package com.vaia.presentation.ui

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.vaia.R
import com.vaia.VaiaApplication
import com.vaia.presentation.navigation.Activities
import com.vaia.presentation.navigation.Calendar
import com.vaia.presentation.navigation.Expenses
import com.vaia.presentation.navigation.Home
import com.vaia.presentation.navigation.Login
import com.vaia.presentation.navigation.Organizer
import com.vaia.presentation.navigation.Profile
import com.vaia.presentation.navigation.Register
import com.vaia.presentation.navigation.Roadmap
import com.vaia.presentation.navigation.Trips
import com.vaia.presentation.navigation.TripChecklist
import com.vaia.presentation.navigation.TripDocuments
import com.vaia.presentation.ui.activities.ActivitiesScreen
import com.vaia.presentation.ui.auth.LoginScreen
import com.vaia.presentation.ui.auth.RegisterScreen
import com.vaia.presentation.ui.calendar.CalendarScreen
import com.vaia.presentation.ui.documents.DocumentChecklistScreen
import com.vaia.presentation.ui.expenses.ExpensesScreen
import com.vaia.presentation.ui.home.HomeScreen
import com.vaia.presentation.ui.organizer.OrganizerScreen
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
import com.vaia.presentation.viewmodel.MapViewModel
import com.vaia.presentation.viewmodel.MapViewModelFactory
import com.vaia.presentation.viewmodel.TripsViewModel
import com.vaia.presentation.viewmodel.TripsViewModelFactory
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.settingsDataStore by preferencesDataStore(name = "settings_prefs")
private val darkThemeEnabledKey = booleanPreferencesKey("dark_theme_enabled")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("VAIA", "MainActivity onCreate called")
        setContent {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val isDarkTheme by context.settingsDataStore.data
                .map { preferences -> preferences[darkThemeEnabledKey] ?: false }
                .collectAsState(initial = false)
            VaiaTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VaiaApp(
                        isDarkTheme = isDarkTheme,
                        onThemeChange = { isEnabled ->
                            scope.launch {
                                context.settingsDataStore.edit { settings ->
                                    settings[darkThemeEnabledKey] = isEnabled
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun VaiaApp(
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit
) {
    val navController = rememberNavController()
    val application = androidx.compose.ui.platform.LocalContext.current.applicationContext as VaiaApplication
    val appContainer = application.appContainer

    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(appContainer.authRepository)
    )
    val tripsViewModel: TripsViewModel = viewModel(
        factory = TripsViewModelFactory(
            appContainer.tripRepository,
            appContainer.authRepository,
            appContainer.activityRepository
        )
    )
    val mapViewModel: MapViewModel = viewModel(
        factory = MapViewModelFactory(
            application = androidx.compose.ui.platform.LocalContext.current.applicationContext as android.app.Application,
            activityRepository = appContainer.activityRepository
        )
    )

    fun navigateToLogin() {
        navController.navigate(Login) {
            popUpTo<Home> { inclusive = true }
            launchSingleTop = true
        }
    }

    val startDestination = if (authViewModel.isLoggedIn()) Home else Login

    NavHost(navController = navController, startDestination = startDestination) {

        composable<Login> {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Home) {
                        popUpTo<Login> { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Register) },
                viewModel = authViewModel
            )
        }

        composable<Register> {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Home) {
                        popUpTo<Register> { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.navigate(Login) },
                viewModel = authViewModel
            )
        }

        composable<Home> {
            LaunchedEffect(Unit) { if (!authViewModel.isLoggedIn()) navigateToLogin() }
            HomeScreen(
                onNavigateToActivities = { tripId -> navController.navigate(Activities(tripId)) },
                onNavigateTrips = { navController.navigate(Trips) { launchSingleTop = true } },
                onNavigateProfile = { navController.navigate(Profile) { launchSingleTop = true } },
                onNavigateCalendar = { navController.navigate(Calendar) { launchSingleTop = true } },
                onNavigateOrganizer = { navController.navigate(Organizer) { launchSingleTop = true } },
                viewModel = tripsViewModel
            )
        }

        composable<Trips> {
            LaunchedEffect(Unit) { if (!authViewModel.isLoggedIn()) navigateToLogin() }
            TripsScreen(
                onNavigateToActivities = { tripId -> navController.navigate(Activities(tripId)) },
                onNavigateHome = {
                    navController.navigate(Home) {
                        popUpTo<Home> { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onNavigateTrips = { navController.navigate(Trips) { launchSingleTop = true } },
                onNavigateProfile = { navController.navigate(Profile) { launchSingleTop = true } },
                onNavigateCalendar = { navController.navigate(Calendar) { launchSingleTop = true } },
                onNavigateOrganizer = { navController.navigate(Organizer) { launchSingleTop = true } },
                viewModel = tripsViewModel
            )
        }

        composable<Calendar> {
            LaunchedEffect(Unit) { if (!authViewModel.isLoggedIn()) navigateToLogin() }
            CalendarScreen(
                appContainer = appContainer,
                onNavigateHome = { navController.navigate(Home) { launchSingleTop = true } },
                onNavigateTrips = { navController.navigate(Trips) { launchSingleTop = true } },
                onNavigateProfile = { navController.navigate(Profile) { launchSingleTop = true } },
                onNavigateOrganizer = { navController.navigate(Organizer) { launchSingleTop = true } },
            )
        }

        composable<Activities> { backStackEntry ->
            LaunchedEffect(Unit) { if (!authViewModel.isLoggedIn()) navigateToLogin() }
            val route: Activities = backStackEntry.toRoute()
            val activitiesViewModel: ActivitiesViewModel = viewModel(
                factory = ActivitiesViewModelFactory(appContainer.activityRepository, appContainer.tripRepository, route.tripId, appContainer.reminderScheduler)
            )
            ActivitiesScreen(
                tripId = route.tripId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToExpenses = { navController.navigate(Expenses(route.tripId)) },
                onNavigateToRoadmap = { navController.navigate(Roadmap(route.tripId)) },
                onNavigateToDocuments = { navController.navigate(TripDocuments(route.tripId)) },
                onNavigateHome = { navController.navigate(Home) { launchSingleTop = true } },
                onNavigateTrips = { navController.navigate(Trips) { launchSingleTop = true } },
                onNavigateProfile = { navController.navigate(Profile) { launchSingleTop = true } },
                onNavigateCalendar = { navController.navigate(Calendar) { launchSingleTop = true } },
                onNavigateOrganizer = { navController.navigate(Organizer) { launchSingleTop = true } },
                viewModel = activitiesViewModel
            )
        }

        composable<Roadmap> { backStackEntry ->
            LaunchedEffect(Unit) { if (!authViewModel.isLoggedIn()) navigateToLogin() }
            val route: Roadmap = backStackEntry.toRoute()
            val activitiesViewModel: ActivitiesViewModel = viewModel(
                factory = ActivitiesViewModelFactory(appContainer.activityRepository, appContainer.tripRepository, route.tripId, appContainer.reminderScheduler)
            )
            RoadmapScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateHome = { navController.navigate(Home) { launchSingleTop = true } },
                onNavigateTrips = { navController.navigate(Trips) { launchSingleTop = true } },
                onNavigateProfile = { navController.navigate(Profile) { launchSingleTop = true } },
                onNavigateCalendar = { navController.navigate(Calendar) { launchSingleTop = true } },
                onNavigateOrganizer = { navController.navigate(Organizer) { launchSingleTop = true } },
                viewModel = activitiesViewModel
            )
        }

        composable<Expenses> { backStackEntry ->
            LaunchedEffect(Unit) { if (!authViewModel.isLoggedIn()) navigateToLogin() }
            val route: Expenses = backStackEntry.toRoute()
            val expensesViewModel: ExpensesViewModel = viewModel(
                factory = ExpensesViewModelFactory(appContainer.expenseRepository, route.tripId)
            )
            ExpensesScreen(
                tripId = route.tripId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateHome = { navController.navigate(Home) { launchSingleTop = true } },
                onNavigateTrips = { navController.navigate(Trips) { launchSingleTop = true } },
                onNavigateProfile = { navController.navigate(Profile) { launchSingleTop = true } },
                onNavigateCalendar = { navController.navigate(Calendar) { launchSingleTop = true } },
                onNavigateOrganizer = { navController.navigate(Organizer) { launchSingleTop = true } },
                viewModel = expensesViewModel
            )
        }

        composable<TripDocuments> { backStackEntry ->
            LaunchedEffect(Unit) { if (!authViewModel.isLoggedIn()) navigateToLogin() }
            val route: TripDocuments = backStackEntry.toRoute()
            val tripTitle = stringResource(R.string.trip_documents_title)
            DocumentChecklistScreen(
                tripId = route.tripId,
                tripTitle = tripTitle,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<TripChecklist> { backStackEntry ->
            LaunchedEffect(Unit) { if (!authViewModel.isLoggedIn()) navigateToLogin() }
            val route: TripChecklist = backStackEntry.toRoute()
            DocumentChecklistScreen(
                tripId = route.tripId,
                tripTitle = route.tripTitle,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<Profile> {
            LaunchedEffect(Unit) { if (!authViewModel.isLoggedIn()) navigateToLogin() }
            ProfileScreen(
                onNavigateHome = { navController.navigate(Home) { launchSingleTop = true } },
                onNavigateTrips = { navController.navigate(Trips) { launchSingleTop = true } },
                onNavigateProfile = { navController.navigate(Profile) { launchSingleTop = true } },
                onNavigateCalendar = { navController.navigate(Calendar) { launchSingleTop = true } },
                onNavigateOrganizer = { navController.navigate(Organizer) { launchSingleTop = true } },
                onLogout = {
                    authViewModel.logout()
                    navigateToLogin()
                },
                isDarkTheme = isDarkTheme,
                onThemeChange = onThemeChange,
                viewModel = authViewModel
            )
        }

        composable<Organizer> {
            LaunchedEffect(Unit) { if (!authViewModel.isLoggedIn()) navigateToLogin() }
            OrganizerScreen(
                onNavigateHome = { navController.navigate(Home) { launchSingleTop = true } },
                onNavigateTrips = { navController.navigate(Trips) { launchSingleTop = true } },
                onNavigateCalendar = { navController.navigate(Calendar) { launchSingleTop = true } },
                onNavigateProfile = { navController.navigate(Profile) { launchSingleTop = true } },
                tripsViewModel = tripsViewModel,
                mapViewModel = mapViewModel
            )
        }
    }
}
