package com.vaia.presentation.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vaia.presentation.ui.common.AppQuickBar
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppQuickBarTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun quickBar_clickOrganizer_invokesCallback() {
        var organizerTapped = false

        composeRule.setContent {
            AppQuickBar(
                currentRoute = "home",
                onHome = {},
                onTrips = {},
                onProfile = {},
                onCalendar = {},
                onMap = { organizerTapped = true }
            )
        }

        composeRule.onNodeWithContentDescription("Organizador").assertIsDisplayed().performClick()
        assertTrue(organizerTapped)
    }
}
