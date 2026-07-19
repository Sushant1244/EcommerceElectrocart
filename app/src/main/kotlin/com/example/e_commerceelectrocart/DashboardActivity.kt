package com.example.e_commerceelectrocart

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.e_commerceelectrocart.ui.theme.EcommerceElectrocartTheme
import com.facebook.login.LoginManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import com.example.e_commerceelectrocart.firestore.ProductRepository

data class QuickLink(val name: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@AndroidEntryPoint
class DashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Start listening to Firestore products so UI stays live
        ProductRepository.startListening()
        val startDestination = intent.getStringExtra("START_DESTINATION") ?: "HOME"
        setContent {
            EcommerceElectrocartTheme {
                MainScreen(startDestination)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ProductRepository.stopListening()
    }
}

val quickLinks = listOf(
    QuickLink("Gems", Icons.Default.Star),
    QuickLink("Sale Live", Icons.Default.LocalOffer),
    QuickLink("Choice", Icons.Default.CheckCircle),
    QuickLink("Freebie", Icons.Default.Redeem),
    QuickLink("Free Delivery", Icons.Default.LocalShipping),
)


@Composable
fun MainScreen(startDestination: String = "HOME") {
    val initialSelection = when (startDestination) {
        "CART" -> 2
        "MESSAGES" -> 1
        else -> 0
    }
    var selectedItem by remember { mutableStateOf(initialSelection) }
    var searchText by remember { mutableStateOf("") }
    val items = listOf("Home", "Messages", "Cart", "Account")
    val icons = listOf(Icons.Default.Home, Icons.Default.MailOutline, Icons.Default.ShoppingCart, Icons.Default.Person)

    Scaffold(
        topBar = {
            if (selectedItem != 2) { // Hide top bar on Cart screen
                DashboardTopBar(searchText = searchText, onSearchChange = { searchText = it }, onCartClick = { selectedItem = 2 })
            }
        },
        bottomBar = {
            NavigationBar {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(icons[index], contentDescription = item) },
                        label = { Text(item) },
                        selected = selectedItem == index,
                        onClick = { selectedItem = index }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedItem) {
                0 -> HomeScreen(searchText)
                1 -> MessagesScreen()
                2 -> CartScreen(CartRepository.cartItems)
                3 -> AccountScreen()
            }
        }
    }
}

@Composable
fun HomeScreen(searchQuery: String = "") {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F5F9))
    ) {
        item { HomeHeroSection() }
        item { QuickLinksSection() }
        item { CategoriesSection() }
        item { FlashSaleSection(searchQuery) }
        item { ProductGrid(searchQuery) }
    }
}

@Composable
fun HomeHeroSection() {
    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Discover premium deals",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF102A43)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Explore curated offers and shop the best products with faster delivery.",
                    fontSize = 14.sp,
                    color = Color(0xFF52606D),
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { /* TODO: Navigate to deals */ },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F9D58))
                ) {
                    Text("Shop Now", color = Color.White)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Card(
                modifier = Modifier.size(110.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF7FF))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = R.drawable.offer_banner),
                        contentDescription = "Hero Offer",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
fun QuickLinksSection() {
    val context = LocalContext.current
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(quickLinks) { link ->
            Card(
                modifier = Modifier
                    .size(width = 130.dp, height = 110.dp)
                    .clickable {
                        val intent = when (link.name) {
                            "Gems" -> Intent(context, GemsActivity::class.java)
                            "Sale Live" -> Intent(context, SaleLiveActivity::class.java)
                            "Choice" -> Intent(context, ChoiceActivity::class.java)
                            "Freebie" -> Intent(context, FreebieActivity::class.java)
                            "Free Delivery" -> Intent(context, FreeDeliveryActivity::class.java)
                            else -> null
                        }
                        intent?.let { context.startActivity(it) }
                    },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF4F7FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(link.icon, contentDescription = link.name, tint = Color(0xFF0F9D58))
                    }
                    Text(link.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF102A43))
                }
            }
        }
    }
}

@Composable
fun CategoriesSection() {
    val categories = listOf("Electronics", "Fashion", "Home", "Beauty", "Toys")
    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text("Top Categories", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF102A43))
        Spacer(modifier = Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(categories) { cat ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 3.dp,
                    modifier = Modifier.clickable { /* TODO: Filter by category */ }
                ) {
                    Text(
                        cat,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF334E68)
                    )
                }
            }
        }
    }
}

@Composable
fun FlashSaleSection(searchQuery: String = "") {
    val source = com.example.e_commerceelectrocart.firestore.ProductRepository.products
    val filtered = if (searchQuery.isBlank()) source.shuffled() else source.filter { it.name.contains(searchQuery, true) }

    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
        SectionHeader(title = "Flash Sale", actionText = "View All") { /* TODO */ }
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(filtered.take(5)) { product ->
                ProductCard(product)
            }
        }
    }
}

@Composable
fun ProductGrid(searchQuery: String = "") {
    val source = com.example.e_commerceelectrocart.firestore.ProductRepository.products
    val filtered = if (searchQuery.isBlank()) source else source.filter { it.name.contains(searchQuery, true) }

    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
        SectionHeader(title = "Recommended for you", actionText = "See More") { /* TODO */ }
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(filtered) { product ->
                ProductCard(product)
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, actionText: String, onActionClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF102A43))
        Spacer(modifier = Modifier.weight(1f))
        Text(
            actionText,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF0F9D58),
            modifier = Modifier.clickable(onClick = onActionClick)
        )
    }
}


/* ---------------- TOP BAR ---------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardTopBar(searchText: String, onSearchChange: (String) -> Unit, onCartClick: () -> Unit) {
    TopAppBar(
        title = {
            OutlinedTextField(
                value = searchText,
                onValueChange = { onSearchChange(it) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
                placeholder = { Text("Search products, categories...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color(0xFFF0F3F7),
                    unfocusedContainerColor = Color(0xFFF0F3F7)
                )
            )
        },
        actions = {
            IconButton(onClick = onCartClick) {
                Icon(Icons.Default.ShoppingCart, contentDescription = "Cart")
            }
            IconButton(onClick = { /* TODO: Notifications */ }) {
                Icon(Icons.Default.Notifications, contentDescription = "Notifications")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF9FAFC)),
        modifier = Modifier.fillMaxWidth()
    )
}


/* ---------------- SCREENS ---------------- */

@Composable
fun MessagesScreen() {
    // Dummy conversation list
    val conversations = listOf(
        "Sumit Shah" to "Great, I will be there.",
        "John Doe" to "See you tomorrow!"
    )

    LazyColumn(modifier = Modifier.padding(16.dp)) {
        items(conversations) { (name, lastMessage) ->
            Row(modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.LightGray)) // Placeholder for profile pic
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(name, fontWeight = FontWeight.Bold)
                    Text(lastMessage, color = Color.Gray, fontSize = 14.sp)
                }
            }
            HorizontalDivider()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(cartItems: MutableList<CartItem>) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Cart") },
                actions = {
                    IconButton(onClick = { /* Handle delete */ }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            )
        },
        bottomBar = {
            CheckoutBar(cartItems, false) { 
                val intent = Intent(context, CheckoutActivity::class.java)
                 if(cartItems.isNotEmpty()){
                    intent.putExtra("PRODUCT_NAME", cartItems[0].product.name)
                    intent.putExtra("PRODUCT_PRICE", cartItems[0].product.price)
                    intent.putExtra("PRODUCT_IMAGE", cartItems[0].product.image)
                 }
                 context.startActivity(intent)
            }
        }
    ) { padding ->
        if (cartItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Your cart is empty", style = MaterialTheme.typography.headlineMedium)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(Color(0xFFF5F5F5))
            ) {
                item {
                    VendorHeader()
                }
                items(cartItems) { item ->
                    var isChecked by remember { mutableStateOf(false) }
                    CartListItem(item, isChecked) { 
                        isChecked = !isChecked
                    } 
                }
            }
        }
    }
}

@Composable
fun VendorHeader() {
    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        var isChecked by remember { mutableStateOf(false) }
        Checkbox(checked = isChecked, onCheckedChange = { isChecked = it })
        Text("Everyday Online Pvt.Ltd", fontWeight = FontWeight.Bold)
        Icon(Icons.Default.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(14.dp))
    }
}


@Composable
fun CartListItem(item: CartItem, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = isChecked, onCheckedChange = onCheckedChange)
        Image(
            painter = painterResource(id = item.product.image),
            contentDescription = item.product.name,
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.product.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 2)
            Text("Rs. ${item.product.price}", color = Color.Gray, fontSize = 14.sp)
             Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Rs. ${item.product.originalPrice}",
                        textDecoration = TextDecoration.LineThrough,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (item.product.discount > 0) {
                        Text("-${item.product.discount}%", fontSize = 12.sp, color = Color.Red)
                    }
                }
        }
        Spacer(modifier = Modifier.width(16.dp))
        var quantity by remember { mutableStateOf(item.quantity) }
        QuantitySelector(quantity) { 
            quantity = it
            if(it > 0) {
                val index = CartRepository.cartItems.indexOf(item)
                if (index != -1) {
                    CartRepository.cartItems[index] = item.copy(quantity = it)
                }
            } else {
                CartRepository.remove(item)
            }
        }
    }
}

@Composable
fun CheckoutBar(cartItems: List<CartItem>, isAllChecked: Boolean, onCheckout: () -> Unit) {
    val totalPrice = cartItems.sumOf { it.product.price * it.quantity } 
    var allChecked by remember { mutableStateOf(isAllChecked) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = allChecked, onCheckedChange = { allChecked = it })
            Text("All")
            Spacer(modifier = Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text("Subtotal: Rs. $totalPrice", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Shipping Fee: Rs. 143", fontSize = 12.sp, color = Color.Gray) 
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(onClick = onCheckout, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57224))) {
                Text("Check Out(${cartItems.size})")
            }
        }
    }
}


@Composable
fun AccountScreen() {
    var user by remember { mutableStateOf<User?>(null) }
    val firebaseUser = FirebaseAuth.getInstance().currentUser
    var showLogoutDialog by remember { mutableStateOf(false) }

    DisposableEffect(firebaseUser) {
        if (firebaseUser == null) {
            onDispose { }
        } else {
            val firestore = FirebaseFirestore.getInstance()
            val docRef = firestore.collection("Users").document(firebaseUser.uid)
            val registration = docRef.addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                try {
                    val name = snapshot?.getString("name") ?: firebaseUser.displayName ?: "User"
                    val email = snapshot?.getString("email") ?: firebaseUser.email ?: ""
                    user = User(firebaseUser.uid, name, email)
                } catch (e: Exception) {
                    // ignore
                }
            }

            onDispose {
                try {
                    registration.remove()
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
    }

    val context = LocalContext.current
    val activity = (context as? ComponentActivity)

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        user?.let { currentUser ->
            // Profile Section
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(80.dp).clip(CircleShape).background(Color.LightGray)) 
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(currentUser.name, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                    Text(currentUser.email, color = Color.Gray, fontSize = 16.sp)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider()
        } ?: Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
             CircularProgressIndicator()
        }

        // Menu Items
        ProfileMenuItem(text = "My Orders", icon = Icons.Default.ListAlt) { context.startActivity(Intent(context, MyOrdersActivity::class.java)) }
        ProfileMenuItem(text = "Shipping Addresses", icon = Icons.Default.LocationOn) { context.startActivity(Intent(context, AddressActivity::class.java)) }
        ProfileMenuItem(text = "Settings", icon = Icons.Default.Settings) { context.startActivity(Intent(context, SettingsActivity::class.java)) }
        Spacer(modifier = Modifier.weight(1f))

        // Logout Button (with confirmation)
        Button(
            onClick = { showLogoutDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57224)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Logout")
        }

        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("Confirm Logout") },
                text = { Text("Are you sure you want to logout?") },
                confirmButton = {
                    TextButton(onClick = {
                        showLogoutDialog = false
                        try {
                            FirebaseAuth.getInstance().signOut()
                            LoginManager.getInstance().logOut()
                        } catch (e: Exception) {
                            try { FirebaseAuth.getInstance().signOut() } catch (ignored: Exception) {}
                        }
                        CartRepository.clear()
                        val intent = Intent(context, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        context.startActivity(intent)
                        activity?.finish()
                    }) { Text("Logout") }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
fun ProfileMenuItem(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).clickable(onClick = onClick)) {
        Icon(icon, contentDescription = null, tint = Color.Gray)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text, fontSize = 18.sp)
        Spacer(modifier = Modifier.weight(1f))
        Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = Color.Gray)
    }
}
