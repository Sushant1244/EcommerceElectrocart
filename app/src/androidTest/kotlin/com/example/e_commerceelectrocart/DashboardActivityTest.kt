package com.example.e_commerceelectrocart

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DashboardActivityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<DashboardActivity>()

    @Test
    fun dashboard_displaysSmartWatchProduct() {
        // Wait for the Dashboard to load and check if one of our products is visible
        composeTestRule.onNodeWithText("Smart Watch").assertExists()
    }

    @Test
    fun dashboard_displaysFlashSaleHeader() {
        // Verify the Flash Sale section is present
        composeTestRule.onNodeWithText("Flash Sale").assertExists()
    }

    @Test
    fun search_filtersProducts() {
        // Type into search and verify filtered result appears
        composeTestRule.onNodeWithText("Search products, categories...").performTextInput("Laptop")
        composeTestRule.onNodeWithText("Laptop").assertExists()
    }

    @Test
    fun addToCart_andOpenCart_showsItem() {
        // Tap first Add button, open Cart tab and verify cart contains item
        composeTestRule.onNodeWithText("Add").performClick()
        // Open Cart via bottom navigation
        composeTestRule.onNodeWithText("Cart").performClick()
        composeTestRule.onNodeWithText("My Cart").assertExists()
        // Ensure at least one product name is shown in cart
        composeTestRule.onNodeWithText("Smart Watch").assertExists()
    }
}
