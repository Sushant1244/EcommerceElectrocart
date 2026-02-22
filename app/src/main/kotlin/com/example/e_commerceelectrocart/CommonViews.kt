package com.example.e_commerceelectrocart

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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

/**
 * A shared product card component used across multiple screens.
 */
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
