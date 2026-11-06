package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.model.Invoice
import com.example.data.repository.StoreFirestoreRepository
import com.example.ui.components.SettingsDialog
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.InvoiceReceiptDialog
import com.example.ui.screens.InvoicesCustomersScreen
import com.example.ui.screens.PosScreen
import com.example.ui.screens.signOutUser
import com.example.ui.theme.TajerProTheme
import com.example.ui.viewmodel.StoreViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TajerProTheme {
                AppNavigation()
            }
        }
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = { /* AuthStateListener updates currentUser automatically */ }
        )
    } else {
        TajerProMainApp(currentUserId = user.uid)
    }
}

enum class MainDestination(
    val routeId: String,
    val label: String,
    val icon: ImageVector
) {
    DASHBOARD("dashboard", "الرئيسية", Icons.Default.Dashboard),
    POS("pos", "نقطة البيع", Icons.Default.PointOfSale),
    INVENTORY("inventory", "المخزون", Icons.Default.Inventory2),
    INVOICES_CUSTOMERS("invoices_customers", "الفواتير والعملاء", Icons.AutoMirrored.Filled.ReceiptLong)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TajerProMainApp(
    currentUserId: String,
    viewModel: StoreViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                StoreViewModel(StoreFirestoreRepository(db), currentUserId)
            }
        }
    )
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val productsUi by viewModel.productsState.collectAsStateWithLifecycle()
    val invoicesUi by viewModel.invoicesState.collectAsStateWithLifecycle()
    val customersUi by viewModel.customersState.collectAsStateWithLifecycle()
    val paymentsUi by viewModel.paymentsState.collectAsStateWithLifecycle()
    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    val metrics by viewModel.dashboardMetrics.collectAsStateWithLifecycle()
    val selectedPeriod by viewModel.selectedPeriod.collectAsStateWithLifecycle()
    val cartState by viewModel.cartState.collectAsStateWithLifecycle()
    val lastCreatedInvoice by viewModel.lastCreatedInvoice.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    val products = (productsUi as? UiState.Success)?.data ?: emptyList()
    val invoices = (invoicesUi as? UiState.Success)?.data ?: emptyList()
    val customers = (customersUi as? UiState.Success)?.data ?: emptyList()
    val payments = (paymentsUi as? UiState.Success)?.data ?: emptyList()

    var currentDestination by remember { mutableStateOf(MainDestination.DASHBOARD) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var selectedInvoiceForReceipt by remember { mutableStateOf<Invoice?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    // BackHandler on secondary screens returns to Dashboard
    if (currentDestination != MainDestination.DASHBOARD) {
        BackHandler {
            currentDestination = MainDestination.DASHBOARD
        }
    }

    val lowStockCount = products.count { it.isLowStock || it.isOutOfStock }
    val cartUnitCount = cartState.items.sumOf { it.quantity }.toInt()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Row {
                            Text(
                                text = settings.storeName.ifBlank { "تاجر برو" },
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = settings.currencySymbol.ifBlank { "$" },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showSettingsDialog = true },
                            modifier = Modifier.testTag("open_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "إعدادات المحل والعملة"
                            )
                        }
                        IconButton(
                            onClick = {
                                signOutUser(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onSignOutComplete = {},
                                    scope = scope
                                )
                            },
                            modifier = Modifier.testTag("sign_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "تسجيل الخروج"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar {
                        MainDestination.entries.forEach { dest ->
                            NavigationBarItem(
                                selected = currentDestination == dest,
                                onClick = { currentDestination = dest },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (dest == MainDestination.POS && cartUnitCount > 0) {
                                                Badge { Text(cartUnitCount.toString()) }
                                            } else if (dest == MainDestination.INVENTORY && lowStockCount > 0) {
                                                Badge { Text(lowStockCount.toString()) }
                                            }
                                        }
                                    ) {
                                        Icon(imageVector = dest.icon, contentDescription = dest.label)
                                    }
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag("nav_${dest.routeId}")
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
                if (isWideScreen) {
                    NavigationRail {
                        MainDestination.entries.forEach { dest ->
                            NavigationRailItem(
                                selected = currentDestination == dest,
                                onClick = { currentDestination = dest },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (dest == MainDestination.POS && cartUnitCount > 0) {
                                                Badge { Text(cartUnitCount.toString()) }
                                            } else if (dest == MainDestination.INVENTORY && lowStockCount > 0) {
                                                Badge { Text(lowStockCount.toString()) }
                                            }
                                        }
                                    ) {
                                        Icon(imageVector = dest.icon, contentDescription = dest.label)
                                    }
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag("rail_${dest.routeId}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    when (currentDestination) {
                        MainDestination.DASHBOARD -> {
                            DashboardScreen(
                                metrics = metrics,
                                selectedPeriod = selectedPeriod,
                                onSelectPeriod = viewModel::setSelectedPeriod,
                                products = products,
                                recentInvoices = invoices,
                                settings = settings,
                                onNavigateToPos = { currentDestination = MainDestination.POS },
                                onNavigateToInventory = { currentDestination = MainDestination.INVENTORY },
                                onNavigateToInvoices = { currentDestination = MainDestination.INVOICES_CUSTOMERS },
                                onQuickRestock = { prod -> viewModel.restockProduct(prod, 10L) },
                                onSelectInvoiceForReceipt = { inv -> selectedInvoiceForReceipt = inv },
                                onSeedStarterData = viewModel::seedStarterData
                            )
                        }
                        MainDestination.POS -> {
                            PosScreen(
                                products = products,
                                customers = customers,
                                cartState = cartState,
                                settings = settings,
                                onAddToCart = viewModel::addToCart,
                                onScanBarcode = viewModel::scanBarcodeToCart,
                                onUpdateCartQty = viewModel::updateCartItemQuantity,
                                onRemoveFromCart = viewModel::removeFromCart,
                                onClearCart = viewModel::clearCart,
                                onSelectCustomer = viewModel::selectCustomerForCart,
                                onQuickAddCustomer = { name, phone ->
                                    viewModel.saveCustomer(
                                        name = name,
                                        phone = phone,
                                        address = "",
                                        notes = ""
                                    )
                                },
                                onUpdateDiscount = viewModel::updateCartDiscount,
                                onUpdateTaxRate = viewModel::updateCartTaxRate,
                                onUpdatePaymentMethod = viewModel::updateCartPaymentMethod,
                                onUpdatePaidAmount = viewModel::updateCartPaidAmount,
                                onUpdateNotes = viewModel::updateCartNotes,
                                onSubmitCheckout = viewModel::submitInvoiceCheckout
                            )
                        }
                        MainDestination.INVENTORY -> {
                            InventoryScreen(
                                products = products,
                                settings = settings,
                                onSaveProduct = viewModel::saveProduct,
                                onRestockProduct = viewModel::restockProduct,
                                onDeleteProduct = viewModel::deleteProduct
                            )
                        }
                        MainDestination.INVOICES_CUSTOMERS -> {
                            InvoicesCustomersScreen(
                                invoices = invoices,
                                customers = customers,
                                payments = payments,
                                settings = settings,
                                onSelectInvoiceForReceipt = { inv -> selectedInvoiceForReceipt = inv },
                                onSaveCustomer = viewModel::saveCustomer,
                                onRecordPayment = viewModel::recordCustomerDebtPayment
                            )
                        }
                    }
                }
            }
        }
    }

    // Show Receipt Dialog when a new invoice is created or an existing invoice is clicked
    val activeReceiptInvoice = lastCreatedInvoice ?: selectedInvoiceForReceipt
    if (activeReceiptInvoice != null) {
        InvoiceReceiptDialog(
            invoice = activeReceiptInvoice,
            settings = settings,
            onDismiss = {
                viewModel.clearLastCreatedInvoice()
                selectedInvoiceForReceipt = null
            }
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            currentSettings = settings,
            onDismiss = { showSettingsDialog = false },
            onSave = { name, phone, addr, curr, tax, footer ->
                viewModel.saveStoreSettings(name, phone, addr, curr, tax, footer)
                showSettingsDialog = false
            }
        )
    }
}
