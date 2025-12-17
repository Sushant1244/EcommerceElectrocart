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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.e_commerceelectrocart.ui.theme.EcommerceElectrocartTheme

class CheckoutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // For now, we'll work with a single product. We can enhance this later to handle a full cart.
        val productName = intent.getStringExtra("PRODUCT_NAME") ?: "No Product"
        val productPrice = intent.getIntExtra("PRODUCT_PRICE", 0)
        val productImage = intent.getIntExtra("PRODUCT_IMAGE", R.drawable.cat_phone) 

        val product = Product(productName, productImage, productPrice, productPrice) 

        setContent {
            EcommerceElectrocartTheme {
                CheckoutScreen(product = product)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(product: Product) {
    val context = LocalContext.current
    val activity = (LocalContext.current as? ComponentActivity)
    val shippingFee = 83 // Dummy data

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Checkout") },
                navigationIcon = {
                    IconButton(onClick = { activity?.finish() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            val total = product.price + shippingFee
            BottomAppBar(containerColor = Color.White) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Total:", fontSize = 16.sp, color = Color.Gray)
                        Text("Rs. $total", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF57224))
                    }
                    Button(
                        onClick = { context.startActivity(Intent(context, PaymentActivity::class.java)) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57224))
                    ) {
                        Text("Proceed to Pay")
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
        ) {
            item { AddressSection() }
            item { ItemSection(product) }
            item { OrderSummarySection(product, shippingFee) }
        }
    }
}

@Composable
fun AddressSection() {
    val context = LocalContext.current
    // Using dummy data for address
    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.LocationOn, contentDescription = "Location", tint = Color(0xFFF57224))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Delivery Address", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = { context.startActivity(Intent(context, AddressActivity::class.java)) }) {
                Text("Add/Change")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Sushant", fontWeight = FontWeight.Bold)
                Text("15-82-0101, Dillibazar Pipalbot, Kathmandu")
                Text("Bagmati Province, 9701605257")
            }
        }
    }
}

@Composable
fun ItemSection(product: Product) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text("Nep Hot", fontWeight = FontWeight.Bold, fontSize = 16.sp) // Dummy Vendor
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = product.image),
                contentDescription = product.name,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, maxLines = 2)
                Text("No Brand, Color Family: Black", fontSize = 12.sp, color = Color.Gray) // Dummy details
            }
            Text("Rs. ${product.price}", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun OrderSummarySection(product: Product, shippingFee: Int) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Order Summary", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Merchandise Subtotal")
                    Text("Rs. ${product.price}")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Shipping Fee Subtotal")
                    Text("Rs. $shippingFee")
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Total Payment", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Rs. ${product.price + shippingFee}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFFF57224))
                }
            }
        }
    }
}
