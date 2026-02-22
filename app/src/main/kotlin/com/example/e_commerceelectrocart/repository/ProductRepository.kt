package com.example.e_commerceelectrocart.repository

import android.net.Uri
import android.util.Log
import com.example.e_commerceelectrocart.firebase.FirebaseManager
import com.example.e_commerceelectrocart.firestore.Category
import com.example.e_commerceelectrocart.firestore.Product
import com.example.e_commerceelectrocart.firestore.Review
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Product-related Firestore operations
 */
@Singleton
class ProductRepository @Inject constructor(
    private val firebaseManager: FirebaseManager
) {
    companion object {
        private const val TAG = "ProductRepository"
    }

    private val productsCollection = firebaseManager.db.collection(FirebaseManager.COLLECTION_PRODUCTS)
    private val categoriesCollection = firebaseManager.db.collection(FirebaseManager.COLLECTION_CATEGORIES)

    /**
     * Get all products as a Flow with real-time updates
     */
    fun getAllProducts(): Flow<List<Product>> = callbackFlow {
        val listener = productsCollection
            .whereEqualTo("isAvailable", true)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error fetching products", error)
                    close(error)
                    return@addSnapshotListener
                }

                val products = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Product::class.java)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing product: ${doc.id}", e)
                        null
                    }
                } ?: emptyList()

                trySend(products)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get featured products
     */
    fun getFeaturedProducts(): Flow<List<Product>> = callbackFlow {
        val listener = productsCollection
            .whereEqualTo("isFeatured", true)
            .whereEqualTo("isAvailable", true)
            .limit(10)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error fetching featured products", error)
                    close(error)
                    return@addSnapshotListener
                }

                val products = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Product::class.java)
                } ?: emptyList()

                trySend(products)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get flash sale products
     */
    fun getFlashSaleProducts(): Flow<List<Product>> = callbackFlow {
        val listener = productsCollection
            .whereEqualTo("isFlashSale", true)
            .whereEqualTo("isAvailable", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error fetching flash sale products", error)
                    close(error)
                    return@addSnapshotListener
                }

                val products = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Product::class.java)
                } ?: emptyList()

                trySend(products)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get products by category
     */
    fun getProductsByCategory(categoryId: String): Flow<List<Product>> = callbackFlow {
        val listener = productsCollection
            .whereEqualTo("categoryId", categoryId)
            .whereEqualTo("isAvailable", true)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error fetching products by category", error)
                    close(error)
                    return@addSnapshotListener
                }

                val products = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Product::class.java)
                } ?: emptyList()

                trySend(products)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get single product by ID
     */
    suspend fun getProductById(productId: String): Product? {
        return try {
            val doc = productsCollection.document(productId).get().await()
            if (doc.exists()) {
                doc.toObject(Product::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting product: $productId", e)
            null
        }
    }

    /**
     * Search products
     */
    fun searchProducts(query: String): Flow<List<Product>> = callbackFlow {
        // Firestore doesn't support full-text search, use simple approach
        val listener = productsCollection
            .whereEqualTo("isAvailable", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error searching products", error)
                    close(error)
                    return@addSnapshotListener
                }

                val products = snapshot?.documents
                    ?.mapNotNull { doc -> doc.toObject(Product::class.java) }
                    ?.filter { product ->
                        product.name.contains(query, ignoreCase = true) ||
                        product.description.contains(query, ignoreCase = true) ||
                        product.tags.any { it.contains(query, ignoreCase = true) }
                    } ?: emptyList()

                trySend(products)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Add new product
     */
    suspend fun addProduct(product: Product, imageUris: List<Uri> = emptyList()): String? {
        return try {
            // Upload images first if provided
            val imageUrls = if (imageUris.isNotEmpty()) {
                imageUris.mapNotNull { uri ->
                    try {
                        firebaseManager.uploadImage(firebaseManager.productsStorageRef, uri)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error uploading product image", e)
                        null
                    }
                }
            } else {
                emptyList()
            }

            val productWithImages = product.copy(
                imageUrls = imageUrls,
                thumbnailUrl = imageUrls.firstOrNull()
            )

            val docRef = productsCollection.add(productWithImages).await()
            firebaseManager.logEvent("product_created", mapOf("product_id" to docRef.id))
            docRef.id
        } catch (e: Exception) {
            Log.e(TAG, "Error adding product", e)
            null
        }
    }

    /**
     * Update product
     */
    suspend fun updateProduct(productId: String, updates: Map<String, Any?>): Boolean {
        return try {
            productsCollection.document(productId)
                .update(updates + mapOf("updatedAt" to FieldValue.serverTimestamp()))
                .await()
            firebaseManager.logEvent("product_updated", mapOf("product_id" to productId))
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating product: $productId", e)
            false
        }
    }

    /**
     * Delete product
     */
    suspend fun deleteProduct(productId: String): Boolean {
        return try {
            val product = getProductById(productId)
            product?.imageUrls?.forEach { url ->
                firebaseManager.deleteImage(url)
            }
            
            productsCollection.document(productId).delete().await()
            firebaseManager.logEvent("product_deleted", mapOf("product_id" to productId))
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting product: $productId", e)
            false
        }
    }

    /**
     * Get all categories
     */
    fun getAllCategories(): Flow<List<Category>> = callbackFlow {
        val listener = categoriesCollection
            .whereEqualTo("isActive", true)
            .orderBy("sortOrder")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error fetching categories", error)
                    close(error)
                    return@addSnapshotListener
                }

                val categories = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Category::class.java)
                } ?: emptyList()

                trySend(categories)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get category by ID
     */
    suspend fun getCategoryById(categoryId: String): Category? {
        return try {
            val doc = categoriesCollection.document(categoryId).get().await()
            doc.toObject(Category::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting category: $categoryId", e)
            null
        }
    }

    /**
     * Add new category
     */
    suspend fun addCategory(category: Category, imageUri: Uri? = null): String? {
        return try {
            val imageUrl = imageUri?.let {
                firebaseManager.uploadImage(firebaseManager.bannersStorageRef, it)
            }

            val categoryWithImage = category.copy(imageUrl = imageUrl)
            val docRef = categoriesCollection.add(categoryWithImage).await()
            docRef.id
        } catch (e: Exception) {
            Log.e(TAG, "Error adding category", e)
            null
        }
    }

    /**
     * Update category
     */
    suspend fun updateCategory(categoryId: String, updates: Map<String, Any?>): Boolean {
        return try {
            categoriesCollection.document(categoryId).update(updates).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating category: $categoryId", e)
            false
        }
    }

    /**
     * Delete category
     */
    suspend fun deleteCategory(categoryId: String): Boolean {
        return try {
            categoriesCollection.document(categoryId).delete().await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting category: $categoryId", e)
            false
        }
    }

    /**
     * Get product reviews
     */
    fun getProductReviews(productId: String): Flow<List<Review>> = callbackFlow {
        val listener = firebaseManager.db.collection("reviews")
            .whereEqualTo("productId", productId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error fetching reviews", error)
                    close(error)
                    return@addSnapshotListener
                }

                val reviews = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Review::class.java)
                } ?: emptyList()

                trySend(reviews)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Add product review
     */
    suspend fun addReview(review: Review): String? {
        return try {
            val docRef = firebaseManager.db.collection("reviews").add(review).await()
            
            // Update product rating
            val reviews = getProductReviews(review.productId)
            // Calculate new average rating
            updateProductRating(review.productId)
            
            docRef.id
        } catch (e: Exception) {
            Log.e(TAG, "Error adding review", e)
            null
        }
    }

    /**
     * Update product rating based on reviews
     */
    private suspend fun updateProductRating(productId: String) {
        try {
            val snapshot = firebaseManager.db.collection("reviews")
                .whereEqualTo("productId", productId)
                .get()
                .await()

            val reviews = snapshot.documents.mapNotNull { it.toObject(Review::class.java) }
            if (reviews.isNotEmpty()) {
                val avgRating = reviews.map { it.rating }.average()
                productsCollection.document(productId)
                    .update(mapOf(
                        "rating" to avgRating,
                        "reviewCount" to reviews.size
                    ))
                    .await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating product rating", e)
        }
    }

    /**
     * Get products with pagination
     */
    suspend fun getProductsPaginated(
        limit: Int = 20,
        lastDocument: com.google.firebase.firestore.DocumentSnapshot? = null
    ): List<Product> {
        return try {
            var query = productsCollection
                .whereEqualTo("isAvailable", true)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())

            lastDocument?.let {
                query = query.startAfter(it)
            }

            val snapshot = query.get().await()
            snapshot.documents.mapNotNull { it.toObject(Product::class.java) }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting paginated products", e)
            emptyList()
        }
    }
}
