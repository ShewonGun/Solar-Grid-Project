/* ============================================================================
 * File        : SmartGridNavHost.kt
 * Purpose     : Navigation graph for the SmartGrid mobile client. Decides the
 *               start destination from the SQLite-cached session, routes a
 *               fresh login to the home screen that matches the user's role,
 *               and drives the prosumer bottom navigation bar.
 * Author      : SmartGrid Mobile Team
 * Created     : 2026-09-17
 * ==========================================================================*/
package com.example.smartgrid_mobile.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.smartgrid_mobile.core.AppViewModelFactory
import com.example.smartgrid_mobile.core.ServiceLocator
import com.example.smartgrid_mobile.data.remote.Roles
import com.example.smartgrid_mobile.ui.auth.LoginDestination
import com.example.smartgrid_mobile.ui.auth.LoginScreen
import com.example.smartgrid_mobile.ui.auth.LoginViewModel
import com.example.smartgrid_mobile.ui.auth.RegisterScreen
import com.example.smartgrid_mobile.ui.auth.RegisterViewModel
import com.example.smartgrid_mobile.ui.booking.BookSlotScreen
import com.example.smartgrid_mobile.ui.booking.BookingAction
import com.example.smartgrid_mobile.ui.booking.BookingSummaryScreen
import com.example.smartgrid_mobile.ui.booking.BookingSummaryViewModel
import com.example.smartgrid_mobile.ui.booking.BookingViewModel
import com.example.smartgrid_mobile.ui.booking.MyBookingsScreen
import com.example.smartgrid_mobile.ui.booking.MyBookingsViewModel
import com.example.smartgrid_mobile.ui.map.NodesMapScreen
import com.example.smartgrid_mobile.ui.map.NodesMapViewModel
import com.example.smartgrid_mobile.ui.operator.OperatorHomeScreen
import com.example.smartgrid_mobile.ui.operator.OperatorViewModel
import com.example.smartgrid_mobile.ui.operator.ScanQrScreen
import com.example.smartgrid_mobile.ui.operator.VerifiedTransferScreen
import com.example.smartgrid_mobile.ui.prosumer.ChangePasswordScreen
import com.example.smartgrid_mobile.ui.prosumer.EditProfileScreen
import com.example.smartgrid_mobile.ui.prosumer.ProsumerHomeScreen
import com.example.smartgrid_mobile.ui.prosumer.ProsumerViewModel
import com.example.smartgrid_mobile.ui.qr.TransactionQrScreen
import com.example.smartgrid_mobile.ui.qr.TransactionQrViewModel

/** Route names for every destination in the graph. */
object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val PROSUMER_GRAPH = "prosumer"
    const val PROSUMER_HOME = "prosumer/home"
    const val PROSUMER_PROFILE = "prosumer/profile"
    const val PROSUMER_PASSWORD = "prosumer/password"
    const val PROSUMER_BOOK_SLOT = "prosumer/book-slot"
    const val PROSUMER_BOOKINGS = "prosumer/bookings"
    const val PROSUMER_QR = "prosumer/qr"
    const val PROSUMER_NODES_MAP = "prosumer/nodes-map"
    const val PROSUMER_BOOKING_SUMMARY = "prosumer/booking-summary"

    /** Arguments for the summary screen: which booking, and what was done to it. */
    const val ARG_ACTION = "action"

    /** Route for the summary shown after a booking is created, changed or cancelled. */
    fun bookingSummary(reservationId: String, action: BookingAction): String =
        "$PROSUMER_BOOKING_SUMMARY/$reservationId?$ARG_ACTION=${action.name}"
    const val OPERATOR_GRAPH = "operator"
    const val OPERATOR_HOME = "operator/home"
    const val OPERATOR_SCAN = "operator/scan"
    const val OPERATOR_RESULT = "operator/result"

    /** Optional argument naming the booking the QR screen should open on. */
    const val ARG_RESERVATION_ID = "reservationId"

    /** Route for the QR screen, optionally pointed at one booking. */
    fun prosumerQr(reservationId: String? = null): String =
        if (reservationId.isNullOrBlank()) PROSUMER_QR else "$PROSUMER_QR?$ARG_RESERVATION_ID=$reservationId"
}

@Composable
fun SmartGridNavHost(navController: NavHostController = rememberNavController()) {
    val repository = ServiceLocator.authRepository
    val session by repository.session.collectAsStateWithLifecycle()

    // One-off notice handed from the registration screen to the login screen.
    var loginNotice by remember { mutableStateOf<String?>(null) }

    // Start where the stored session says, so a returning user skips the login form.
    val startDestination = remember {
        when (repository.session.value?.user?.role) {
            null -> Routes.LOGIN
            Roles.OPERATOR, Roles.BACKOFFICE -> Routes.OPERATOR_GRAPH
            else -> Routes.PROSUMER_GRAPH
        }
    }

    // Signing out (or a cleared token) always returns the app to the login screen.
    LaunchedEffect(session) {
        if (session == null && navController.currentDestination?.route != Routes.LOGIN) {
            navController.navigate(Routes.LOGIN) {
                popUpTo(navController.graph.id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    // The route on screen right now, used to light up the matching bottom tab.
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route

    // Switches tabs without stacking them, so back always returns to Home rather
    // than walking the tab history. State is deliberately not saved: the slot and
    // booking lists reload from the API on every visit instead of going stale.
    val selectTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(Routes.PROSUMER_HOME)
            launchSingleTop = true
        }
    }

    // Handed to every tab destination so they all render the same bar.
    val prosumerBottomBar: @Composable () -> Unit = {
        ProsumerBottomBar(currentRoute = currentRoute, onSelect = selectTab)
    }

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Routes.LOGIN) {
            val viewModel: LoginViewModel = viewModel(factory = AppViewModelFactory)

            // Surfaces the "account created" notice once the user lands back here.
            LaunchedEffect(loginNotice) {
                loginNotice?.let {
                    viewModel.showInfo(it)
                    loginNotice = null
                }
            }

            LoginScreen(
                viewModel = viewModel,
                onLoggedIn = { destination ->
                    val route = when (destination) {
                        LoginDestination.OPERATOR -> Routes.OPERATOR_GRAPH
                        LoginDestination.PROSUMER -> Routes.PROSUMER_GRAPH
                    }
                    navController.navigate(route) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onCreateAccount = { navController.navigate(Routes.REGISTER) }
            )
        }

        composable(Routes.REGISTER) {
            val viewModel: RegisterViewModel = viewModel(factory = AppViewModelFactory)
            RegisterScreen(
                viewModel = viewModel,
                onRegistered = { nic ->
                    loginNotice = "Account $nic created. It stays pending until a " +
                        "backoffice officer activates it, then you can sign in."
                    navController.popBackStack(Routes.LOGIN, inclusive = false)
                },
                onBack = { navController.popBackStack() }
            )
        }

        navigation(startDestination = Routes.PROSUMER_HOME, route = Routes.PROSUMER_GRAPH) {

            composable(Routes.PROSUMER_HOME) { entry ->
                val viewModel = prosumerViewModel(navController, entry)
                ProsumerHomeScreen(
                    viewModel = viewModel,
                    bottomBar = prosumerBottomBar,
                    onEditProfile = {
                        viewModel.clearMessages()
                        selectTab(Routes.PROSUMER_PROFILE)
                    },
                    onMyBookings = {
                        viewModel.clearMessages()
                        selectTab(Routes.PROSUMER_BOOKINGS)
                    },
                    onBookSlot = {
                        viewModel.clearMessages()
                        selectTab(Routes.PROSUMER_BOOK_SLOT)
                    },
                    onTransactionQr = {
                        viewModel.clearMessages()
                        navController.navigate(Routes.prosumerQr())
                    },
                    onNearbyNodes = {
                        viewModel.clearMessages()
                        navController.navigate(Routes.PROSUMER_NODES_MAP)
                    },
                    onChangePassword = {
                        viewModel.clearMessages()
                        navController.navigate(Routes.PROSUMER_PASSWORD)
                    }
                )
            }

            composable(Routes.PROSUMER_PROFILE) { entry ->
                EditProfileScreen(
                    viewModel = prosumerViewModel(navController, entry),
                    bottomBar = prosumerBottomBar,
                    // A saved profile drops the user back on the Home tab.
                    onSaved = { selectTab(Routes.PROSUMER_HOME) }
                )
            }

            composable(Routes.PROSUMER_BOOK_SLOT) {
                // Booking has its own view model, scoped to this destination, so
                // leaving the screen drops the slot list it loaded.
                val bookingViewModel: BookingViewModel = viewModel(factory = AppViewModelFactory)
                BookSlotScreen(
                    viewModel = bookingViewModel,
                    bottomBar = prosumerBottomBar,
                    onBooked = { reservationId ->
                        navController.navigate(
                            Routes.bookingSummary(reservationId, BookingAction.CREATED)
                        )
                    }
                )
            }

            composable(Routes.PROSUMER_BOOKINGS) {
                // Scoped to this destination, so the lists reload on each visit.
                val bookingsViewModel: MyBookingsViewModel =
                    viewModel(factory = AppViewModelFactory)
                MyBookingsScreen(
                    viewModel = bookingsViewModel,
                    bottomBar = prosumerBottomBar,
                    onShowQr = { reservationId ->
                        navController.navigate(Routes.prosumerQr(reservationId))
                    },
                    onActionSummary = { reservationId, action ->
                        navController.navigate(
                            Routes.bookingSummary(reservationId, BookingAction.valueOf(action))
                        )
                    }
                )
            }

            composable(
                route = "${Routes.PROSUMER_QR}?${Routes.ARG_RESERVATION_ID}={${Routes.ARG_RESERVATION_ID}}",
                arguments = listOf(
                    navArgument(Routes.ARG_RESERVATION_ID) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { entry ->
                // Scoped to this destination so the approved list reloads on each visit.
                val qrViewModel: TransactionQrViewModel = viewModel(factory = AppViewModelFactory)
                TransactionQrScreen(
                    viewModel = qrViewModel,
                    reservationId = entry.arguments?.getString(Routes.ARG_RESERVATION_ID),
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "${Routes.PROSUMER_BOOKING_SUMMARY}/{${Routes.ARG_RESERVATION_ID}}" +
                    "?${Routes.ARG_ACTION}={${Routes.ARG_ACTION}}",
                arguments = listOf(
                    navArgument(Routes.ARG_RESERVATION_ID) { type = NavType.StringType },
                    navArgument(Routes.ARG_ACTION) {
                        type = NavType.StringType
                        defaultValue = BookingAction.CREATED.name
                    }
                )
            ) { entry ->
                val summaryViewModel: BookingSummaryViewModel =
                    viewModel(factory = AppViewModelFactory)
                val reservationId = entry.arguments?.getString(Routes.ARG_RESERVATION_ID).orEmpty()
                val action = entry.arguments?.getString(Routes.ARG_ACTION)
                    ?.let { runCatching { BookingAction.valueOf(it) }.getOrNull() }
                    ?: BookingAction.CREATED

                BookingSummaryScreen(
                    viewModel = summaryViewModel,
                    reservationId = reservationId,
                    action = action,
                    onViewBookings = { selectTab(Routes.PROSUMER_BOOKINGS) },
                    onShowQr = { id -> navController.navigate(Routes.prosumerQr(id)) },
                    onDone = { selectTab(Routes.PROSUMER_HOME) }
                )
            }

            composable(Routes.PROSUMER_NODES_MAP) {
                // Scoped to this destination so the node list is re-read on each visit.
                val mapViewModel: NodesMapViewModel = viewModel(factory = AppViewModelFactory)
                NodesMapScreen(
                    viewModel = mapViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.PROSUMER_PASSWORD) { entry ->
                ChangePasswordScreen(
                    viewModel = prosumerViewModel(navController, entry),
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }
        }

        navigation(startDestination = Routes.OPERATOR_HOME, route = Routes.OPERATOR_GRAPH) {

            composable(Routes.OPERATOR_HOME) { entry ->
                // Starting a new job here clears whatever the last scan left behind.
                val operatorViewModel = operatorViewModel(navController, entry)
                OperatorHomeScreen(
                    user = session?.user,
                    onScanQr = {
                        operatorViewModel.reset()
                        navController.navigate(Routes.OPERATOR_SCAN)
                    },
                    onSignOut = { repository.logout() }
                )
            }

            composable(Routes.OPERATOR_SCAN) { entry ->
                ScanQrScreen(
                    viewModel = operatorViewModel(navController, entry),
                    onVerified = {
                        navController.navigate(Routes.OPERATOR_RESULT) {
                            // The camera has done its job; back goes to the home screen.
                            popUpTo(Routes.OPERATOR_SCAN) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.OPERATOR_RESULT) { entry ->
                val operatorViewModel = operatorViewModel(navController, entry)
                VerifiedTransferScreen(
                    viewModel = operatorViewModel,
                    onScanAnother = {
                        operatorViewModel.reset()
                        navController.navigate(Routes.OPERATOR_SCAN) {
                            popUpTo(Routes.OPERATOR_RESULT) { inclusive = true }
                        }
                    },
                    onDone = {
                        operatorViewModel.reset()
                        navController.popBackStack(Routes.OPERATOR_HOME, inclusive = false)
                    }
                )
            }
        }
    }
}

/**
 * Resolves the OperatorViewModel against the operator nav graph, so the home,
 * scanner and result screens all share one scanned job.
 */
@Composable
private fun operatorViewModel(
    navController: NavHostController,
    entry: androidx.navigation.NavBackStackEntry
): OperatorViewModel {
    val parentEntry = remember(entry) { navController.getBackStackEntry(Routes.OPERATOR_GRAPH) }
    return viewModel(parentEntry, factory = AppViewModelFactory)
}

/**
 * Resolves the ProsumerViewModel against the prosumer nav graph, so the three
 * prosumer screens share one instance and it is cleared on sign-out.
 */
@Composable
private fun prosumerViewModel(
    navController: NavHostController,
    entry: androidx.navigation.NavBackStackEntry
): ProsumerViewModel {
    val parentEntry = remember(entry) { navController.getBackStackEntry(Routes.PROSUMER_GRAPH) }
    return viewModel(parentEntry, factory = AppViewModelFactory)
}
