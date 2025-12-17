package com.example.e_commerceelectrocart

import androidx.compose.runtime.mutableStateListOf

/**
 * Shared data models for the application.
 */

data class Product(
    val name: String,
    val image: Int,
    val price: Int,
    val originalPrice: Int,
    val description: String = ""
) {
    val discount: Int
        get() = if (originalPrice > 0) ((originalPrice.toDouble() - price) / originalPrice * 100).toInt() else 0
}

data class CartItem(val product: Product, var quantity: Int)

data class User(val uid: String, val name: String, val email: String)

data class Order(val orderId: String, val date: String, val total: Double, val status: String, val customerName: String = "")

/**
 * A repository for managing the shopping cart.
 */
object CartRepository {
    val cartItems = mutableStateListOf<CartItem>()

    fun add(product: Product) {
        val existingItem = cartItems.find { it.product.name == product.name }
        if (existingItem != null) {
            existingItem.quantity++
        } else {
            cartItems.add(CartItem(product, 1))
        }
    }

    fun remove(item: CartItem) {
        cartItems.remove(item)
    }

    fun clear() {
        cartItems.clear()
    }
}
