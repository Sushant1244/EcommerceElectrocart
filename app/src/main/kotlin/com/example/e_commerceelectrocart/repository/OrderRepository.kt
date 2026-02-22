package com.example.e_commerceelectrocart.repository

import android.util.Log
import com.example.e_commerceelectrocart.firebase.FirebaseManager
import com.example.e_commerceelectrocart.firestore.Address
import com.example.e_commerceelectrocart.firestore.CartItem
import com.example.e_commerceelectrocart.firestore.Order
import com.example.e_commerceelectrocart.firestore.OrderItem
import com.example.e_commerceelectrocart.firestore.OrderStatus
import com.example.e_commerceelectrocart.firestore.PaymentMethod
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Order-related Firestore operations
 */
@Singleton
class OrderRepository @Inject constructor(
    private val firebaseManager: FirebaseManager
) {
    companion object {
        private const val TAG = "OrderRepository"
    }

    private val ordersCollection = firebaseManager.db.collection(FirebaseManager.COLLECTION_ORDERS)
    private val cartsCollection = firebaseManager.db.collection("carts")

    /**
     * Get user's orders with real-time updates
     */
    fun getUserOrders(userId: String): Flow<List<Order>> = callbackFlow {
        val listener = ordersCollection
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error fetching user orders", error)
                    close(error)
                    return@addSnapshotListener
                }

                val orders = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Order::class.java)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing order: ${doc.id}", e)
                        null
                    }
                } ?: emptyList()

                trySend(orders)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get all orders (for admin)
     */
    fun getAllOrders(): Flow<List<Order>> = callbackFlow {
        val listener = ordersCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error fetching all orders", error)
                    close(error)
                    return@addSnapshotListener
                }

                val orders = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Order::class.java)
                } ?: emptyList()

                trySend(orders)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get order by ID
     */
    suspend fun getOrderById(orderId: String): Order? {
        return try {
            val doc = ordersCollection.document(orderId).get().await()
            doc.toObject(Order::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting order: $orderId", e)
            null
        }
    }

    /**
     * Create new order from cart
     */
    suspend fun createOrder(
        userId: String,
        items: List<CartItem>,
        shippingAddress: Address,
        billingAddress: Address?,
        paymentMethod: PaymentMethod,
        couponCode: String? = null,
        notes: String? = null
    ): String? {
        return try {
            // Calculate totals
            val subtotal = items.sumOf { it.totalPrice }
            val shippingCost = if (subtotal >= 499) 0.0 else 49.0 // Free shipping above 499
            val tax = subtotal * 0.18 // 18% GST
            val discount = 0.0 // Apply coupon discount if applicable
            val total = subtotal + shippingCost + tax - discount

            // Generate order number
            val orderNumber = generateOrderNumber()

            // Convert cart items to order items
            val orderItems = items.map { cartItem ->
                OrderItem(
                    productId = cartItem.productId,
                    productName = cartItem.productName,
                    productImageUrl = cartItem.productImageUrl,
                    price = cartItem.price,
                    quantity = cartItem.quantity,
                    totalPrice = cartItem.totalPrice
                )
            }

            val order = Order(
                id = "",
                userId = userId,
                orderNumber = orderNumber,
                items = orderItems,
                subtotal = subtotal,
                shippingCost = shippingCost,
                tax = tax,
                discount = discount,
                total = total,
                status = OrderStatus.PENDING,
                paymentMethod = paymentMethod,
                paymentStatus = if (paymentMethod == PaymentMethod.COD) "pending" else "pending",
                shippingAddress = shippingAddress,
                billingAddress = billingAddress ?: shippingAddress,
                notes = notes,
                couponCode = couponCode
            )

            val docRef = ordersCollection.add(order).await()

            // Clear user's cart after order
            clearUserCart(userId)

            // Log analytics
            firebaseManager.logEvent("order_created", mapOf(
                "order_id" to docRef.id,
                "order_total" to total,
                "item_count" to items.size
            ))

            // Call Cloud Function to send order confirmation
            sendOrderConfirmation(userId, docRef.id, orderNumber, total)

            docRef.id
        } catch (e: Exception) {
            Log.e(TAG, "Error creating order", e)
            null
        }
    }

    /**
     * Update order status (for admin)
     */
    suspend fun updateOrderStatus(orderId: String, status: OrderStatus): Boolean {
        return try {
            val updates = mutableMapOf<String, Any>(
                "status" to status.name,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            if (status == OrderStatus.DELIVERED) {
                updates["deliveredAt"] = FieldValue.serverTimestamp()
            }

            ordersCollection.document(orderId).update(updates).await()

            // Log analytics
            firebaseManager.logEvent("order_status_updated", mapOf(
                "order_id" to orderId,
                "new_status" to status.name
            ))

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating order status: $orderId", e)
            false
        }
    }

    /**
     * Update payment status
     */
    suspend fun updatePaymentStatus(orderId: String, paymentStatus: String): Boolean {
        return try {
            ordersCollection.document(orderId)
                .update(mapOf(
                    "paymentStatus" to paymentStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                ))
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating payment status: $orderId", e)
            false
        }
    }

    /**
     * Cancel order
     */
    suspend fun cancelOrder(orderId: String, reason: String? = null): Boolean {
        return try {
            ordersCollection.document(orderId)
                .update(mapOf(
                    "status" to OrderStatus.CANCELLED.name,
                    "cancellationReason" to reason,
                    "updatedAt" to FieldValue.serverTimestamp()
                ))
                .await()

            firebaseManager.logEvent("order_cancelled", mapOf("order_id" to orderId))
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling order: $orderId", e)
            false
        }
    }

    /**
     * Get user's cart items
     */
    fun getUserCart(userId: String): Flow<List<CartItem>> = callbackFlow {
        val listener = cartsCollection
            .document(userId)
            .collection("items")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error fetching cart", error)
                    close(error)
                    return@addSnapshotListener
                }

                val items = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(CartItem::class.java)
                } ?: emptyList()

                trySend(items)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Add item to cart
     */
    suspend fun addToCart(userId: String, cartItem: CartItem): Boolean {
        return try {
            // Check if item already exists in cart
            val existingItem = cartsCollection.document(userId)
                .collection("items")
                .whereEqualTo("productId", cartItem.productId)
                .get()
                .await()

            if (!existingItem.isEmpty) {
                // Update quantity
                val existingDoc = existingItem.documents.first()
                val newQuantity = existingDoc.getLong("quantity")?.toInt()?.plus(cartItem.quantity) ?: cartItem.quantity
                existingDoc.reference.update("quantity", newQuantity, "totalPrice", cartItem.price * newQuantity).await()
            } else {
                // Add new item
                cartsCollection.document(userId)
                    .collection("items")
                    .add(cartItem)
                    .await()
            }

            firebaseManager.logEvent("add_to_cart", mapOf(
                "product_id" to cartItem.productId,
                "quantity" to cartItem.quantity
            ))

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error adding to cart", e)
            false
        }
    }

    /**
     * Update cart item quantity
     */
    suspend fun updateCartItemQuantity(userId: String, itemId: String, quantity: Int): Boolean {
        return try {
            if (quantity <= 0) {
                // Remove item
                cartsCollection.document(userId)
                    .collection("items")
                    .document(itemId)
                    .delete()
                    .await()
            } else {
                // Update quantity
                val doc = cartsCollection.document(userId)
                    .collection("items")
                    .document(itemId)
                    .get()
                    .await()
                
                val price = doc.getDouble("price") ?: 0.0
                cartsCollection.document(userId)
                    .collection("items")
                    .document(itemId)
                    .update(mapOf(
                        "quantity" to quantity,
                        "totalPrice" to price * quantity
                    ))
                    .await()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating cart item", e)
            false
        }
    }

    /**
     * Remove item from cart
     */
    suspend fun removeFromCart(userId: String, itemId: String): Boolean {
        return try {
            cartsCollection.document(userId)
                .collection("items")
                .document(itemId)
                .delete()
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error removing from cart", e)
            false
        }
    }

    /**
     * Clear user's entire cart
     */
    suspend fun clearUserCart(userId: String): Boolean {
        return try {
            val cartItems = cartsCollection.document(userId)
                .collection("items")
                .get()
                .await()

            for (doc in cartItems.documents) {
                doc.reference.delete().await()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing cart", e)
            false
        }
    }

    /**
     * Get order statistics (for admin)
     */
    suspend fun getOrderStats(): Map<String, Any> {
        return try {
            val totalOrders = ordersCollection.get().await().size()
            val pendingOrders = ordersCollection.whereEqualTo("status", OrderStatus.PENDING.name).get().await().size()
            val deliveredOrders = ordersCollection.whereEqualTo("status", OrderStatus.DELIVERED.name).get().await().size()

            mapOf(
                "totalOrders" to totalOrders,
                "pendingOrders" to pendingOrders,
                "deliveredOrders" to deliveredOrders
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting order stats", e)
            emptyMap()
        }
    }

    /**
     * Generate unique order number
     */
    private fun generateOrderNumber(): String {
        val timestamp = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())
        val random = (1000..9999).random()
        return "ORD-$timestamp-$random"
    }

    /**
     * Send order confirmation via Cloud Function
     */
    private suspend fun sendOrderConfirmation(userId: String, orderId: String, orderNumber: String, total: Double) {
        try {
            firebaseManager.callFunction("sendOrderConfirmation", mapOf(
                "userId" to userId,
                "orderId" to orderId,
                "orderNumber" to orderNumber,
                "total" to total
            ))
        } catch (e: Exception) {
            Log.e(TAG, "Error sending order confirmation", e)
        }
    }
}
