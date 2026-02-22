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
}
