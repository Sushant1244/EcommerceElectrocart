package com.example.e_commerceelectrocart

import org.junit.Assert.assertEquals
import org.junit.Test

class ProductTest {

    @Test
    fun product_discount_calculation_isCorrect() {
        val product = Product(
            name = "Test Product",
            image = 0,
            price = 80,
            originalPrice = 100
        )
        // (100 - 80) / 100 * 100 = 20%
        assertEquals(20, product.discount)
    }

    @Test
    fun cartRepository_add_incrementsQuantity() {
        CartRepository.clear()
        val product = Product("Item", 0, 10, 10)
        
        CartRepository.add(product)
        assertEquals(1, CartRepository.cartItems.size)
        assertEquals(1, CartRepository.cartItems[0].quantity)
        
        CartRepository.add(product)
        assertEquals(1, CartRepository.cartItems.size)
        assertEquals(2, CartRepository.cartItems[0].quantity)
    }
}
