package com.example.e_commerceelectrocart.firestore

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date
import java.util.UUID

/**
 * Firestore data models for the e-commerce application.
 */

/**
 * User profile data model
 */
data class UserProfile(
    @DocumentId
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String? = null,
    val photoUrl: String? = null,
    val role: String = "user", // admin, user, vendor
    val isActive: Boolean = true,
    @ServerTimestamp
    val createdAt: Date? = null,
    @ServerTimestamp
    val lastLogin: Date? = null,
    val address: Address? = null,
    val wishlist: List<String> = emptyList(), // List of product IDs
    val fcmTokens: List<String> = emptyList() // For push notifications
)

/**
 * Address data model
 */
data class Address(
    @DocumentId
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val name: String = "",
    val phone: String = "",
    val addressLine1: String = "",
    val addressLine2: String? = null,
    val city: String = "",
    val state: String = "",
    val postalCode: String = "",
    val country: String = "India",
    val isDefault: Boolean = false,
    val addressType: String = "home" // home, work, other
)

/**
 * Product category data model
 */
data class Category(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val description: String? = null,
    val imageUrl: String? = null,
    val parentId: String? = null,
    val isActive: Boolean = true,
    val sortOrder: Int = 0,
    val productCount: Int = 0
)

/**
 * Product data model
 */
data class Product(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val originalPrice: Double = 0.0,
    val discount: Int = 0,
    val categoryId: String = "",
    val categoryName: String = "",
    val imageUrls: List<String> = emptyList(),
    val thumbnailUrl: String? = null,
    val stock: Int = 0,
    val isAvailable: Boolean = true,
    val vendorId: String? = null,
    val vendorName: String? = null,
    val rating: Double = 0.0,
    val reviewCount: Int = 0,
    val tags: List<String> = emptyList(),
    val specifications: Map<String, String> = emptyMap(),
    @ServerTimestamp
    val createdAt: Date? = null,
    @ServerTimestamp
    val updatedAt: Date? = null,
    val isFeatured: Boolean = false,
    val isFlashSale: Boolean = false,
    val flashSaleEndTime: Date? = null
) {
    val hasDiscount: Boolean
        get() = discount > 0

    val isInStock: Boolean
        get() = stock > 0 && isAvailable

    val formattedPrice: String
        get() = "₹${String.format("%.0f", price)}"

    val formattedOriginalPrice: String
        get() = "₹${String.format("%.0f", originalPrice)}"
}

/**
 * Cart item data model
 */
data class CartItem(
    @DocumentId
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val productId: String = "",
    val productName: String = "",
    val productImageUrl: String? = null,
    val price: Double = 0.0,
    val quantity: Int = 1,
    @ServerTimestamp
    val addedAt: Date? = null
) {
    val totalPrice: Double
        get() = price * quantity
}

/**
 * Order status enum
 */
enum class OrderStatus {
    PENDING,
    CONFIRMED,
    PROCESSING,
    SHIPPED,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED,
    REFUNDED
}

/**
 * Payment method enum
 */
enum class PaymentMethod {
    COD,           // Cash on Delivery
    CREDIT_CARD,
    DEBIT_CARD,
    UPI,
    NET_BANKING,
    WALLET
}

/**
 * Order data model
 */
data class Order(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val orderNumber: String = "",
    val items: List<OrderItem> = emptyList(),
    val subtotal: Double = 0.0,
    val shippingCost: Double = 0.0,
    val tax: Double = 0.0,
    val discount: Double = 0.0,
    val total: Double = 0.0,
    val status: OrderStatus = OrderStatus.PENDING,
    val paymentMethod: PaymentMethod = PaymentMethod.COD,
    val paymentStatus: String = "pending", // pending, paid, failed
    val shippingAddress: Address? = null,
    val billingAddress: Address? = null,
    val notes: String? = null,
    @ServerTimestamp
    val createdAt: Date? = null,
    @ServerTimestamp
    val updatedAt: Date? = null,
    val estimatedDelivery: Date? = null,
    val deliveredAt: Date? = null,
    val trackingNumber: String? = null,
    val couponCode: String? = null
)

/**
 * Order item data model
 */
data class OrderItem(
    val productId: String = "",
    val productName: String = "",
    val productImageUrl: String? = null,
    val price: Double = 0.0,
    val quantity: Int = 1,
    val totalPrice: Double = 0.0
)

/**
 * Review data model
 */
data class Review(
    @DocumentId
    val id: String = "",
    val productId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhotoUrl: String? = null,
    val rating: Int = 0, // 1-5
    val title: String? = null,
    val comment: String = "",
    val isVerifiedPurchase: Boolean = false,
    @ServerTimestamp
    val createdAt: Date? = null,
    val helpfulCount: Int = 0,
    val images: List<String> = emptyList()
)

/**
 * Notification data model
 */
data class AppNotification(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "general", // order, product, promotion, system
    val imageUrl: String? = null,
    val actionUrl: String? = null,
    val isRead: Boolean = false,
    @ServerTimestamp
    val createdAt: Date? = null
)

/**
 * Banner/Carousel data model
 */
data class Banner(
    @DocumentId
    val id: String = "",
    val title: String = "",
    val description: String? = null,
    val imageUrl: String = "",
    val actionUrl: String? = null,
    val actionType: String = "none", // none, product, category, url
    val isActive: Boolean = true,
    val sortOrder: Int = 0,
    @ServerTimestamp
    val createdAt: Date? = null
)

/**
 * Coupon/Promo code data model
 */
data class Coupon(
    @DocumentId
    val id: String = "",
    val code: String = "",
    val description: String? = null,
    val discountType: String = "percentage", // percentage, fixed
    val discountValue: Double = 0.0,
    val minimumOrderAmount: Double = 0.0,
    val maximumDiscount: Double? = null,
    val usageLimit: Int? = null,
    val usedCount: Int = 0,
    val validFrom: Date? = null,
    val validUntil: Date? = null,
    val isActive: Boolean = true,
    val applicableCategories: List<String> = emptyList(),
    val applicableProducts: List<String> = emptyList()
)

/**
 * Chat/Message data model
 */
data class ChatMessage(
    @DocumentId
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderPhotoUrl: String? = null,
    val receiverId: String = "",
    val message: String = "",
    val messageType: String = "text", // text, image, order
    val imageUrl: String? = null,
    val isRead: Boolean = false,
    @ServerTimestamp
    val createdAt: Date? = null
)

/**
 * Chat conversation data model
 */
data class ChatConversation(
    @DocumentId
    val id: String = "",
    val participants: List<String> = emptyList(),
    val lastMessage: String? = null,
    val lastMessageSenderId: String? = null,
    @ServerTimestamp
    val lastMessageTime: Date? = null,
    val unreadCount: Map<String, Int> = emptyMap() // userId to count
)

/**
 * Analytics event data model
 */
data class AnalyticsEvent(
    @DocumentId
    val id: String = "",
    val eventName: String = "",
    val userId: String? = null,
    val userSessionId: String = "",
    val parameters: Map<String, Any> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)
