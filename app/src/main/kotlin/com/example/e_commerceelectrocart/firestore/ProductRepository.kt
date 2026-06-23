package com.example.e_commerceelectrocart.firestore

import com.example.e_commerceelectrocart.CartRepository
import com.example.e_commerceelectrocart.Models
import com.example.e_commerceelectrocart.Product
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import androidx.compose.runtime.mutableStateListOf

/**
 * Simple Firestore-backed product repository with a local fallback.
 * - Exposes `products` as a mutable state list for Compose UI binding.
 * - Starts with local `Models.productList` and listens for remote updates.
 */
object ProductRepository {
    val products = mutableStateListOf<Product>().apply {
        // Seed with the local static list so UI/tests remain deterministic
        addAll(com.example.e_commerceelectrocart.productList)
    }

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private var listener: ListenerRegistration? = null

    fun startListening() {
        // Avoid multiple listeners
        if (listener != null) return

        listener = db.collection("products")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    // Keep the local list on error
                    return@addSnapshotListener
                }
                val fetched = mutableListOf<Product>()
                snapshots?.documents?.forEach { doc ->
                    try {
                        val p = doc.toObject(com.example.e_commerceelectrocart.firestore.Product::class.java)
                        if (p != null) {
                            // Map Firestore product to UI Product model. Use placeholder image if none.
                            val uiProduct = Product(
                                name = p.name.ifBlank { "Product" },
                                image = com.example.e_commerceelectrocart.R.drawable.product_watch,
                                price = p.price.toInt(),
                                originalPrice = p.originalPrice.toInt(),
                                description = p.description
                            )
                            fetched.add(uiProduct)
                        }
                    } catch (e: Exception) {
                        // ignore malformed document
                    }
                }

                if (fetched.isNotEmpty()) {
                    products.clear()
                    products.addAll(fetched)
                }
            }
    }

    fun stopListening() {
        listener?.remove()
        listener = null
    }
}
