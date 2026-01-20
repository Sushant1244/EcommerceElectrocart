package com.example.e_commerceelectrocart

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

/**
 * ViewModel for managing the shopping cart.
 */
class CartViewModel : ViewModel() {
    private val _cartItems = mutableStateListOf<CartItem>()
    val cartItems: List<CartItem> = _cartItems

    fun addProduct(product: Product) {
        val existingItem = _cartItems.find { it.product.name == product.name }
        if (existingItem != null) {
            val index = _cartItems.indexOf(existingItem)
            _cartItems[index] = existingItem.copy(quantity = existingItem.quantity + 1)
        } else {
            _cartItems.add(CartItem(product, 1))
        }
    }

    fun removeItem(item: CartItem) {
        _cartItems.remove(item)
    }

    fun updateQuantity(item: CartItem, newQuantity: Int) {
        if (newQuantity > 0) {
            val index = _cartItems.indexOf(item)
            if (index != -1) {
                _cartItems[index] = item.copy(quantity = newQuantity)
            }
        } else {
            removeItem(item)
        }
    }

    fun clearCart() {
        _cartItems.clear()
    }

    fun getTotalPrice(): Int {
        return _cartItems.sumOf { it.product.price * it.quantity }
    }
}

/**
 * ViewModel for managing user profile data.
 */
class UserViewModel : ViewModel() {
    private val _user = mutableStateOf<User?>(null)
    val user: State<User?> = _user

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().getReference("Users")

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        val firebaseUser = auth.currentUser
        firebaseUser?.uid?.let { uid ->
            db.child(uid).addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val name = snapshot.child("name").getValue(String::class.java) ?: ""
                    val email = snapshot.child("email").getValue(String::class.java) ?: ""
                    _user.value = User(uid, name, email)
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }

    fun logout() {
        auth.signOut()
        _user.value = null
    }
}

/**
 * ViewModel for managing the product list.
 */
class ProductViewModel : ViewModel() {
    private val _products = mutableStateOf<List<Product>>(emptyList())
    val products: State<List<Product>> = _products

    init {
        // In a real app, load from Firebase
        _products.value = listOf(
            Product("Smart Watch", R.drawable.product_watch, 199, 249, "A great smart watch with a lot of features."),
            Product("Headphones", R.drawable.product_headphone, 249, 299, "High-quality headphones with noise cancellation."),
            Product("Laptop", R.drawable.cat_laptop, 1299, 1499, "A powerful laptop for all your needs."),
            Product("Phone", R.drawable.cat_phone, 899, 999, "A smartphone with a great camera."),
            Product("Smart Watch 2", R.drawable.product_watch, 299, 349, "The latest smart watch with a new design."),
        )
    }
}
