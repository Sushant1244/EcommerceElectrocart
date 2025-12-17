package com.example.e_commerceelectrocart

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.e_commerceelectrocart.ui.theme.EcommerceElectrocartTheme

class AdminDashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EcommerceElectrocartTheme {
                AdminDashboardScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen() {
    val context = LocalContext.current
    val activity = (LocalContext.current as? ComponentActivity)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Panel") },
                navigationIcon = {
                    IconButton(onClick = { activity?.finish() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Welcome, Admin!", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(32.dp))
            AdminButton(text = "Manage Products") { context.startActivity(Intent(context, ManageProductsActivity::class.java)) }
            Spacer(modifier = Modifier.height(16.dp))
            AdminButton(text = "View Orders") { context.startActivity(Intent(context, ViewOrdersActivity::class.java)) }
            Spacer(modifier = Modifier.height(16.dp))
            AdminButton(text = "User Management") { context.startActivity(Intent(context, UserManagementActivity::class.java)) }
            Spacer(modifier = Modifier.height(16.dp))
            AdminButton(text = "Sales Analytics") { context.startActivity(Intent(context, AdminAnalyticsActivity::class.java)) }
        }
    }
}

@Composable
fun AdminButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(50.dp)
    ) {
        Text(text, fontSize = 16.sp)
    }
}
