package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Poll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.data.model.UiState
import com.example.data.remote.attemptAutoSignIn
import com.example.data.remote.authStateFlow
import com.example.data.remote.signOutUser
import com.example.data.repository.RewardRepository
import com.example.data.security.FraudGuard
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.EarnScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ReferScreen
import com.example.ui.screens.SurveysScreen
import com.example.ui.screens.WalletScreen
import com.example.ui.screens.WebsiteScreen
import com.example.ui.theme.DangerContainerDark
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.EmeraldContainerDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldBright
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.viewmodel.RewardViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

const val ROUTE_HOME = "home"
const val ROUTE_EARN = "earn"
const val ROUTE_SURVEYS = "surveys"
const val ROUTE_WALLET = "wallet"
const val ROUTE_REFER = "refer"
const val ROUTE_WEBSITE = "website"
const val ROUTE_ADMIN = "admin"

private data class NavDestinationItem(
    val route: String,
    val labelResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    var autoSignInAttempted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!autoSignInAttempted && auth.currentUser == null) {
            autoSignInAttempted = true
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {},
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            val viewModel: RewardViewModel = viewModel(
                key = user.uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY]) {
                            "APPLICATION_KEY missing from CreationExtras"
                        }
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId)
                        RewardViewModel(
                            repository = RewardRepository(db, Firebase.auth),
                            fraudGuard = FraudGuard(app),
                            currentUserId = user.uid,
                            userDisplayName = user.displayName ?: "RewardPulse User",
                            userEmail = user.email ?: "user@rewardpulse.app",
                            userPhotoUrl = user.photoUrl?.toString()
                        )
                    }
                }
            )
            MainRewardShell(
                viewModel = viewModel,
                onSignOut = {
                    signOutUser(
                        credentialManager = credentialManager,
                        scope = scope
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainRewardShell(
    viewModel: RewardViewModel,
    onSignOut: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_HOME

    val profileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val logsState by viewModel.earningLogsState.collectAsStateWithLifecycle()
    val withdrawalsState by viewModel.withdrawalsState.collectAsStateWithLifecycle()
    val referralsState by viewModel.referralsState.collectAsStateWithLifecycle()
    val userFraudEventsState by viewModel.fraudEventsState.collectAsStateWithLifecycle()
    val fraudAssessment by viewModel.fraudAssessment.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val bannerMessage by viewModel.bannerMessage.collectAsStateWithLifecycle()

    val adminUsersState by viewModel.adminUsersState.collectAsStateWithLifecycle()
    val adminWithdrawalsState by viewModel.adminWithdrawalsState.collectAsStateWithLifecycle()
    val adminLogsState by viewModel.adminEarningLogsState.collectAsStateWithLifecycle()
    val adminFraudState by viewModel.adminFraudEventsState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(bannerMessage) {
        val msg = bannerMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg.second)
            viewModel.clearBanner()
        }
    }

    val bottomDestinations = remember {
        listOf(
            NavDestinationItem(ROUTE_HOME, R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home),
            NavDestinationItem(ROUTE_EARN, R.string.nav_earn, Icons.Filled.Bolt, Icons.Outlined.Bolt),
            NavDestinationItem(ROUTE_SURVEYS, R.string.nav_surveys, Icons.Filled.Poll, Icons.Outlined.Poll),
            NavDestinationItem(ROUTE_WALLET, R.string.nav_wallet, Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
            NavDestinationItem(ROUTE_REFER, R.string.nav_refer, Icons.Filled.GroupAdd, Icons.Outlined.GroupAdd)
        )
    }

    val railDestinations = remember(viewModel.isAdmin) {
        buildList {
            addAll(bottomDestinations)
            if (viewModel.isAdmin) {
                add(NavDestinationItem(ROUTE_ADMIN, R.string.nav_admin, Icons.Filled.AdminPanelSettings, Icons.Outlined.AdminPanelSettings))
            }
        }
    }

    val navigateTo: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val profile = (profileState as? UiState.Success)?.data
    val logs = (logsState as? UiState.Success)?.data.orEmpty()
    val withdrawals = (withdrawalsState as? UiState.Success)?.data.orEmpty()
    val referrals = (referralsState as? UiState.Success)?.data.orEmpty()
    val userFraudEvents = (userFraudEventsState as? UiState.Success)?.data.orEmpty()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = ObsidianBg,
            contentWindowInsets = WindowInsets.navigationBars,
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    val isErr = bannerMessage?.first == true
                    Snackbar(
                        snackbarData = data,
                        containerColor = if (isErr) DangerContainerDark else ObsidianCard,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = ObsidianSurface,
                        titleContentColor = Color.White
                    ),
                    navigationIcon = {
                        if ((currentRoute == ROUTE_ADMIN && !useRail) || currentRoute == ROUTE_WEBSITE) {
                            IconButton(
                                onClick = { navigateTo(ROUTE_HOME) },
                                modifier = Modifier.testTag("subscreen_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back to Home",
                                    tint = Color.White
                                )
                            }
                        }
                    },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = when (currentRoute) {
                                    ROUTE_ADMIN -> "Admin Console"
                                    ROUTE_WEBSITE -> "Official Website"
                                    else -> stringResource(id = R.string.app_name)
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (profile != null) {
                                Surface(
                                    color = EmeraldContainerDark,
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = "%,d pts".format(profile.pointsBalance),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = EmeraldBright,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        if (viewModel.isAdmin && currentRoute != ROUTE_ADMIN) {
                            IconButton(
                                onClick = { navigateTo(ROUTE_ADMIN) },
                                modifier = Modifier.testTag("topbar_admin_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AdminPanelSettings,
                                    contentDescription = "Open Admin Console",
                                    tint = GoldBright
                                )
                            }
                        }
                        IconButton(
                            onClick = onSignOut,
                            modifier = Modifier.testTag("sign_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Sign out",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = ObsidianSurface,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        bottomDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            val label = stringResource(id = dest.labelResId)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label, maxLines = 1) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = EmeraldBright,
                                    selectedTextColor = EmeraldBright,
                                    indicatorColor = EmeraldContainerDark
                                ),
                                modifier = Modifier.testTag("nav_${dest.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = ObsidianSurface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        railDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            val label = stringResource(id = dest.labelResId)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag("rail_${dest.route}")
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 840.dp)
                    ) {
                        when (val state = profileState) {
                            is UiState.Loading -> {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(color = EmeraldPrimary)
                                    Spacer(Modifier.size(12.dp))
                                    Text(
                                        text = "Syncing secure RewardPulse wallet...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            is UiState.Error -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = state.message,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            is UiState.Success -> {
                                val loadedProfile = state.data
                                if (loadedProfile == null) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        CircularProgressIndicator(color = EmeraldPrimary)
                                        Spacer(Modifier.size(12.dp))
                                        Text(
                                            text = "Initializing reward ledger...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    NavHost(
                                        navController = navController,
                                        startDestination = ROUTE_HOME,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        composable(ROUTE_HOME) {
                                            HomeScreen(
                                                profile = loadedProfile,
                                                todayKey = viewModel.currentDateKey(),
                                                isAdmin = viewModel.isAdmin,
                                                fraudAssessment = fraudAssessment,
                                                fraudEvents = userFraudEvents,
                                                onRefreshSecurity = viewModel::refreshSecurityScan,
                                                onNavigateToEarn = { navigateTo(ROUTE_EARN) },
                                                onNavigateToSurveys = { navigateTo(ROUTE_SURVEYS) },
                                                onNavigateToWallet = { navigateTo(ROUTE_WALLET) },
                                                onNavigateToRefer = { navigateTo(ROUTE_REFER) },
                                                onNavigateToWebsite = { navigateTo(ROUTE_WEBSITE) },
                                                onNavigateToAdmin = { navigateTo(ROUTE_ADMIN) },
                                                onCopiedNotice = { msg -> viewModel.showInfo(msg) },
                                                onSignOut = onSignOut
                                            )
                                        }
                                        composable(ROUTE_WEBSITE) {
                                            WebsiteScreen(
                                                onBack = { navigateTo(ROUTE_HOME) },
                                                showBackBar = false
                                            )
                                        }
                                        composable(ROUTE_EARN) {
                                            EarnScreen(
                                                profile = loadedProfile,
                                                todayKey = viewModel.currentDateKey(),
                                                logs = logs,
                                                fraudAssessment = fraudAssessment,
                                                isBusy = isBusy,
                                                nowElapsedMs = viewModel::nowElapsedMs,
                                                onAwardOfferTicket = { offer, adSec, started ->
                                                    viewModel.awardOfferTicket(loadedProfile, offer, adSec, started)
                                                },
                                                onClaimTicketOffer = { offer ->
                                                    viewModel.claimOfferWithTickets(loadedProfile, offer)
                                                },
                                                onCompleteAd = { ad, started ->
                                                    viewModel.completeAdWatch(loadedProfile, ad, started)
                                                },
                                                onNavigateToWallet = { navigateTo(ROUTE_WALLET) }
                                            )
                                        }
                                        composable(ROUTE_SURVEYS) {
                                            SurveysScreen(
                                                profile = loadedProfile,
                                                isBusy = isBusy,
                                                nowElapsedMs = viewModel::nowElapsedMs,
                                                onCompleteSurvey = { survey, started ->
                                                    viewModel.completeSurvey(loadedProfile, survey, started)
                                                }
                                            )
                                        }
                                        composable(ROUTE_WALLET) {
                                            WalletScreen(
                                                profile = loadedProfile,
                                                withdrawals = withdrawals,
                                                fraudAssessment = fraudAssessment,
                                                isBusy = isBusy,
                                                onRefreshSecurity = viewModel::refreshSecurityScan,
                                                onSubmitWithdrawal = { method, pts, upi, paytm, acct, ifsc, holder, mail ->
                                                    viewModel.submitWithdrawal(
                                                        profile = loadedProfile,
                                                        method = method,
                                                        pointsToRedeem = pts,
                                                        upiId = upi,
                                                        paytmNumber = paytm,
                                                        bankAccountNumber = acct,
                                                        ifscCode = ifsc,
                                                        accountHolderName = holder,
                                                        redeemEmail = mail
                                                    )
                                                },
                                                onCopiedNotice = { msg ->
                                                    viewModel.showInfo(msg)
                                                }
                                            )
                                        }
                                        composable(ROUTE_REFER) {
                                            ReferScreen(
                                                profile = loadedProfile,
                                                referrals = referrals,
                                                isBusy = isBusy,
                                                onSyncPendingReferrals = viewModel::syncPendingReferrals,
                                                onApplyReferralCode = { code ->
                                                    viewModel.applyReferralCode(loadedProfile, code)
                                                },
                                                onCopiedNotice = { msg ->
                                                    viewModel.showInfo(msg)
                                                }
                                            )
                                        }
                                        if (viewModel.isAdmin) {
                                            composable(ROUTE_ADMIN) {
                                                AdminScreen(
                                                    users = (adminUsersState as? UiState.Success)?.data.orEmpty(),
                                                    withdrawals = (adminWithdrawalsState as? UiState.Success)?.data.orEmpty(),
                                                    earningLogs = (adminLogsState as? UiState.Success)?.data.orEmpty(),
                                                    fraudEvents = (adminFraudState as? UiState.Success)?.data.orEmpty(),
                                                    isBusy = isBusy,
                                                    onApproveWithdrawal = viewModel::approveWithdrawal,
                                                    onRejectWithdrawal = viewModel::rejectWithdrawal,
                                                    onSetUserStatus = viewModel::setUserAccountStatus,
                                                    onResolveFraudEvent = viewModel::resolveFraudEvent
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
        }
    }
}
