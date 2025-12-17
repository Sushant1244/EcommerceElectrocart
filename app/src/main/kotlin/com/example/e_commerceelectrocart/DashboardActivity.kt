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

data class QuickLink(val name: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

class DashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val startDestination = intent.getStringExtra("START_DESTINATION") ?: "HOME"
        setContent {
            EcommerceElectrocartTheme {
                MainScreen(startDestination)
            }
        }
    }
}

val productList = listOf(
    Product("Smart Watch", R.drawable.product_watch, 199, 249, "A great smart watch with a lot of features."),
    Product("Headphones", R.drawable.product_headphone, 249, 299, "High-quality headphones with noise cancellation."),
    Product("Laptop", R.drawable.cat_laptop, 1299, 1499, "A powerful laptop for all your needs."),
    Product("Phone", R.drawable.cat_phone, 899, 999, "A smartphone with a great camera."),
    Product("Smart Watch 2", R.drawable.product_watch, 299, 349, "The latest smart watch with a new design."),
)

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
        else -> 0
    }
    var selectedItem by remember { mutableStateOf(initialSelection) }
    val items = listOf("Home", "Messages", "Cart", "Account")
    val icons = listOf(Icons.Default.Home, Icons.Default.MailOutline, Icons.Default.ShoppingCart, Icons.Default.Person)

    Scaffold(
        topBar = { 
            if(selectedItem != 2) { // Hide top bar on Cart screen
                DashboardTopBar(onCartClick = { selectedItem = 2 }) 
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
                0 -> HomeScreen()
                1 -> MessagesScreen()
                2 -> CartScreen(CartRepository.cartItems)
                3 -> AccountScreen()
            }
        }
    }
}

@Composable
fun HomeScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        item { QuickLinksSection() }
        item { WelcomeBanner() }
        item { MegaDealsBanner() }
        item { FlashSaleSection() }
    }
}


/* ---------------- SECTIONS ---------------- */

@Composable
fun QuickLinksSection() {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        items(quickLinks) { link ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 8.dp).clickable { /* Handle Quick Link Click */ }
            ) {
                Icon(link.icon, contentDescription = link.name, tint = Color(0xFFF57224))
                Text(link.name, fontSize = 10.sp, fontWeight = FontWeight.Medium, modifier = Modifier.widthIn(max = 60.dp))
            }
        }
    }
}


@Composable
fun WelcomeBanner() {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clickable { Toast.makeText(context, "Welcome Banner Clicked", Toast.LENGTH_SHORT).show() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0E1))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Welcome: 15% OFF + Free Delivery", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    Column {
                        Text("Up to Rs. 120", color = Color(0xFFF57224), fontWeight = FontWeight.Bold)
                        Text("New User Exclusive", fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Rs. 200", color = Color(0xFFF57224), fontWeight = FontWeight.Bold)
                        Text("Save on Delivery", fontSize = 11.sp)
                    }
                }
            }
            Button(
                onClick = { Toast.makeText(context, "Collect All Clicked", Toast.LENGTH_SHORT).show() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57224))
            ) {
                Text("Collect All")
            }
        }
    }
}

@Composable
fun MegaDealsBanner() {
    val context = LocalContext.current
    Image(
        painter = painterResource(id = R.drawable.offer_banner),
        contentDescription = "Mega Deals",
        contentScale = ContentScale.FillWidth,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { Toast.makeText(context, "Mega Deals Banner Clicked", Toast.LENGTH_SHORT).show() }
    )
}

@Composable
fun FlashSaleSection() {
    Column(modifier = Modifier.padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Flash Sale", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(16.dp))
            Row {
                Card(shape = RoundedCornerShape(4.dp), colors = CardDefaults.cardColors(containerColor = Color.Black)) {
                    Text("01", color = Color.White, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                }
                Text(" : ", fontWeight = FontWeight.Bold)
                Card(shape = RoundedCornerShape(4.dp), colors = CardDefaults.cardColors(containerColor = Color.Black)) {
                    Text("13", color = Color.White, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                }
                Text(" : ", fontWeight = FontWeight.Bold)
                Card(shape = RoundedCornerShape(4.dp), colors = CardDefaults.cardColors(containerColor = Color.Black)) {
                    Text("15", color = Color.White, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Text("SHOP MORE >", fontSize = 12.sp, color = Color(0xFFF57224), fontWeight = FontWeight.Bold, modifier = Modifier.clickable { /* Handle Shop More Click */ })
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow {
            items(productList.shuffled()) { product ->
                ProductCard(product)
            }
        }
    }
}


/* ---------------- TOP BAR ---------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardTopBar(onCartClick: () -> Unit) {
    var searchText by remember { mutableStateOf("") }
    TopAppBar(
        title = {
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
                placeholder = { Text("Search...") },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    containerColor = Color(0xFFF5F5F5)
                )
            )
        },
        actions = {
            IconButton(onClick = onCartClick) {
                Icon(Icons.Default.ShoppingCart, contentDescription = "Cart")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)

    )
}

/* ---------------- PRODUCT CARD ---------------- */

@Composable
fun ProductCard(product: Product) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .width(150.dp)
            .padding(end = 12.dp)
            .clickable { 
                val intent = Intent(context, ProductDetailActivity::class.java)
                intent.putExtra("PRODUCT_NAME", product.name)
                intent.putExtra("PRODUCT_PRICE", product.price)
                intent.putExtra("PRODUCT_ORIGINAL_PRICE", product.originalPrice)
                intent.putExtra("PRODUCT_IMAGE", product.image)
                intent.putExtra("PRODUCT_DESCRIPTION", product.description)
                context.startActivity(intent)
             },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            Image(
                painter = painterResource(id = product.image),
                contentDescription = "Product",
                modifier = Modifier.height(120.dp).fillMaxWidth(),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(8.dp)) {
                Text(text = product.name, fontWeight = FontWeight.Normal, fontSize = 14.sp, maxLines = 2)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Rs. ${product.price}", color = Color(0xFFF57224), fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Rs. ${product.originalPrice}",
                        textDecoration = TextDecoration.LineThrough,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (product.discount > 0) {
                        Text("-${product.discount}%", fontSize = 12.sp, color = Color.Red)
                    }
                }
            }
        }
    }
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
            Divider()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(cartItems: MutableList<CartItem>) {
    val context = LocalContext.current
    var allChecked by remember { mutableStateOf(false) }

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
            CheckoutBar(cartItems, allChecked) { 
                val intent = Intent(context, CheckoutActivity::class.java)
                 // In a real app, you'd pass all selected items
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
                // Group by a dummy vendor for now
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
    val totalPrice = cartItems.filter { true }.sumOf { it.product.price * it.quantity } // simplified logic
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
                Text("Shipping Fee: Rs. 143", fontSize = 12.sp, color = Color.Gray) // Dummy value
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

    DisposableEffect(firebaseUser) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val name = snapshot.child("name").getValue(String::class.java)
                val email = snapshot.child("email").getValue(String::class.java)
                if (name != null && email != null && firebaseUser != null) {
                    user = User(firebaseUser.uid, name, email)
                }
            }
            override fun onCancelled(error: DatabaseError) { /* Handle error */ }
        }
        firebaseUser?.uid?.let {
            FirebaseDatabase.getInstance().getReference("Users").child(it).addValueEventListener(listener)
        }
        onDispose {
            firebaseUser?.uid?.let {
                FirebaseDatabase.getInstance().getReference("Users").child(it).removeEventListener(listener)
            }
        }
    }

    val context = LocalContext.current
    val activity = (LocalContext.current as? ComponentActivity)

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        val currentUser = user
        if (currentUser != null) {
            // Profile Section
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(80.dp).clip(CircleShape).background(Color.LightGray)) // Placeholder for profile pic
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(currentUser.name, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                    Text(currentUser.email, color = Color.Gray, fontSize = 16.sp)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            Divider()
        } else {
             CircularProgressIndicator()
        }

        // Menu Items
        ProfileMenuItem(text = "My Orders", icon = Icons.Default.ListAlt) { context.startActivity(Intent(context, MyOrdersActivity::class.java)) }
        ProfileMenuItem(text = "Shipping Addresses", icon = Icons.Default.LocationOn) { context.startActivity(Intent(context, AddressActivity::class.java)) }
        ProfileMenuItem(text = "Settings", icon = Icons.Default.Settings) { context.startActivity(Intent(context, SettingsActivity::class.java)) }
        Spacer(modifier = Modifier.weight(1f))

        // Logout Button
        Button(
            onClick = {
                FirebaseAuth.getInstance().signOut()
                LoginManager.getInstance().logOut()
                CartRepository.clear()
                val intent = Intent(context, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                context.startActivity(intent)
                activity?.finish()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57224)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Logout")
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

