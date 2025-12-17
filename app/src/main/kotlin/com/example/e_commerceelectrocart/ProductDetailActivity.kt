package com.example.e_commerceelectrocart

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.e_commerceelectrocart.ui.theme.EcommerceElectrocartTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
class ProductDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val productName = intent.getStringExtra("PRODUCT_NAME") ?: ""
        val productPrice = intent.getIntExtra("PRODUCT_PRICE", 0)
        val productOriginalPrice = intent.getIntExtra("PRODUCT_ORIGINAL_PRICE", 0)
        val productImage = intent.getIntExtra("PRODUCT_IMAGE", 0)
        val productDescription = intent.getStringExtra("PRODUCT_DESCRIPTION") ?: ""

        val product = Product(productName, productImage, productPrice, productOriginalPrice, productDescription)

        setContent {
            EcommerceElectrocartTheme {
                ProductDetailScreen(product)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(product: Product) {
    val context = LocalContext.current
    val activity = (LocalContext.current as? ComponentActivity)
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Product Details") },
                navigationIcon = {
                    IconButton(onClick = { activity?.finish() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Handle Search */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = { 
                        val intent = Intent(context, DashboardActivity::class.java)
                        intent.putExtra("START_DESTINATION", "CART")
                        context.startActivity(intent)
                     }) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = "Cart")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(containerColor = Color.White) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { showBottomSheet = true },
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107))
                    ) {
                        Text("Buy Now", color = Color.Black)
                    }
                    Button(
                        onClick = { 
                            CartRepository.add(product)
                            Toast.makeText(context, "Added to Cart", Toast.LENGTH_SHORT).show() 
                        },
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57224))
                    ) {
                        Text("Add to Cart")
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().background(Color(0xFFF5F5F5))) {
            item { ProductImage(product = product) }
            item { ProductInfo(product = product) }
        }
        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState
            ) {
                BuyNowBottomSheet(product) { 
                     scope.launch {
                        sheetState.hide()
                    }.invokeOnCompletion {
                        if (!sheetState.isVisible) {
                            showBottomSheet = false
                        }
                        val intent = Intent(context, CheckoutActivity::class.java)
                        intent.putExtra("PRODUCT_NAME", product.name)
                        intent.putExtra("PRODUCT_PRICE", product.price)
                        intent.putExtra("PRODUCT_IMAGE", product.image)
                        intent.putExtra("PRODUCT_DESCRIPTION", product.description)
                        context.startActivity(intent)
                    }
                }
            }
        }
    }
}

@Composable
fun ProductImage(product: Product) {
    Image(
        painter = painterResource(id = product.image),
        contentDescription = product.name,
        modifier = Modifier.fillMaxWidth().height(300.dp),
        contentScale = ContentScale.Crop
    )
}

@Composable
fun ProductInfo(product: Product) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = product.name, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Rs. ${product.price}", color = Color(0xFFF57224), fontWeight = FontWeight.Bold, fontSize = 28.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Rs. ${product.originalPrice}",
                textDecoration = TextDecoration.LineThrough,
                fontSize = 18.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.width(8.dp))
            if (product.discount > 0) {
                Text("-${product.discount}%", fontSize = 18.sp, color = Color.Red)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = product.description, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text("100% Authentic", fontWeight = FontWeight.Bold, color = Color.DarkGray)
        Spacer(modifier = Modifier.height(4.dp))
        Text("14 Days Free Returns", color = Color.DarkGray)
    }
}

@Composable
fun BuyNowBottomSheet(product: Product, onBuyNow: () -> Unit) {
    var quantity by remember { mutableStateOf(1) }
    Column(modifier = Modifier.padding(16.dp)) {
        Row {
            Image(painter = painterResource(id = product.image), contentDescription = null, modifier = Modifier.size(80.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("Rs. ${product.price}", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("Stock: 10", color = Color.Gray, fontSize = 12.sp) // Dummy stock
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Divider()
        Spacer(modifier = Modifier.height(16.dp))
        Text("Color Family", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = { /*TODO*/ }) { Text("Black") } // Dummy color
        Spacer(modifier = Modifier.height(16.dp))
        Divider()
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Quantity", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            QuantitySelector(quantity, onQuantityChange = { quantity = it })
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onBuyNow,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107))
        ) {
            Text("Buy Now", color = Color.Black)
        }
    }
}
