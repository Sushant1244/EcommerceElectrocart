package com.example.e_commerceelectrocart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A shared composable for selecting a quantity.
 */
@Composable
fun QuantitySelector(quantity: Int, onQuantityChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = { onQuantityChange(quantity - 1) },
            modifier = Modifier.size(28.dp).clip(CircleShape).background(Color(0xFFF5F5F5))
        ) {
            Icon(Icons.Default.Remove, contentDescription = "Remove", modifier = Modifier.size(16.dp))
        }
        Text(text = "$quantity", modifier = Modifier.padding(horizontal = 12.dp), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        IconButton(
            onClick = { onQuantityChange(quantity + 1) },
            modifier = Modifier.size(28.dp).clip(CircleShape).background(Color(0xFFF5F5F5))
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
        }
    }
}
