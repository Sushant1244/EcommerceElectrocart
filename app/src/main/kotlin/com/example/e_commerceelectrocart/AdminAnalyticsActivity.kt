package com.example.e_commerceelectrocart

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.e_commerceelectrocart.ui.theme.EcommerceElectrocartTheme

data class Sale(val day: String, val amount: Float)
data class TopProduct(val name: String, val unitsSold: Int)

class AdminAnalyticsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Dummy Data
        val salesData = listOf(
            Sale("Mon", 1200f),
            Sale("Tue", 1800f),
            Sale("Wed", 1500f),
            Sale("Thu", 2200f),
            Sale("Fri", 3000f),
            Sale("Sat", 2500f),
            Sale("Sun", 2800f)
        )
        val topProducts = listOf(
            TopProduct("Smart Watch", 150),
            TopProduct("Headphones", 120),
            TopProduct("Laptop", 80),
            TopProduct("Phone", 200)
        )

        setContent {
            EcommerceElectrocartTheme {
                AdminAnalyticsScreen(salesData, topProducts)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAnalyticsScreen(sales: List<Sale>, topProducts: List<TopProduct>) {
    val activity = (androidx.compose.ui.platform.LocalContext.current as? ComponentActivity)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sales Analytics") },
                navigationIcon = {
                    IconButton(onClick = { activity?.finish() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).padding(16.dp)) {
            item {
                Text("Weekly Sales", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                SalesGraph(sales)
                Spacer(modifier = Modifier.height(32.dp))
            }
            item {
                Text("Top Selling Products", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
            }
            items(topProducts.sortedByDescending { it.unitsSold }) {
                TopProductItem(it)
            }
        }
    }
}

@Composable
fun SalesGraph(sales: List<Sale>) {
    val maxSale = sales.maxOfOrNull { it.amount } ?: 0f
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            sales.forEach { sale ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .height((sale.amount / maxSale * 200).dp) // Max height of 200dp
                            .width(30.dp)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Text(sale.day, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun TopProductItem(product: TopProduct) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(product.name, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
            Text("${product.unitsSold} units sold", color = Color.Gray)
        }
    }
}