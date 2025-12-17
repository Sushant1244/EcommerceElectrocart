package com.example.e_commerceelectrocart

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.e_commerceelectrocart.ui.theme.EcommerceElectrocartTheme

class AddressActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EcommerceElectrocartTheme {
                AddressScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressScreen() {
    val context = LocalContext.current
    val activity = (LocalContext.current as? ComponentActivity)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Addresses") },
                navigationIcon = {
                    IconButton(onClick = { activity?.finish() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { context.startActivity(Intent(context, AddEditAddressActivity::class.java)) }) {
                Icon(Icons.Default.Add, contentDescription = "Add Address")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            // Dummy Address
            item {
                AddressItem("Sushant", "15-82-0101, Dillibazar Pipalbot, Kathmandu", "Bagmati Province, 9701605257", isDefault = true)
            }
        }
    }
}

@Composable
fun AddressItem(name: String, address: String, details: String, isDefault: Boolean) {
    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (isDefault) {
                    Text("[Default]", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                }
            }
            Text(address, fontSize = 14.sp)
            Text(details, fontSize = 14.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                TextButton(onClick = { /*TODO*/ }) {
                    Text("Edit")
                }
                TextButton(onClick = { /*TODO*/ }) {
                    Text("Remove")
                }
            }
        }
    }
}
