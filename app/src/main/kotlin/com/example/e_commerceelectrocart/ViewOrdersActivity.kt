package com.example.e_commerceelectrocart

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.e_commerceelectrocart.ui.theme.EcommerceElectrocartTheme

class ViewOrdersActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Dummy Data
        val orders = listOf(
            Order(orderId = "#1234", customerName = "Sumit Shah", date = "2024-07-28", total = 249.0, status = "Shipped"),
            Order(orderId = "#1235", customerName = "John Doe", date = "2024-07-27", total = 1299.0, status = "Processing"),
            Order(orderId = "#1236", customerName = "Jane Smith", date = "2024-07-26", total = 899.0, status = "Delivered")
        )
        setContent {
            EcommerceElectrocartTheme {
                ViewOrdersScreen(orders)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewOrdersScreen(orders: List<Order>) {
    val activity = (LocalContext.current as? ComponentActivity)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("View Orders") },
                navigationIcon = {
                    IconButton(onClick = { activity?.finish() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(orders) { order ->
                OrderItem(order)
            }
        }
    }
}

@Composable
fun OrderItem(order: Order) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(order.orderId, fontWeight = FontWeight.Bold)
            Text("Customer: ${order.customerName}")
            Text("Date: ${order.date}")
            Text("Status: ${order.status}", color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Total: Rs. ${order.total}", fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.End))
        }
    }
}
