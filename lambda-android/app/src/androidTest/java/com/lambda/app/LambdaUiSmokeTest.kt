package com.lambda.app

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LambdaUiSmokeTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun homeOpensAndShowsNavigation() {
        compose.waitUntil(8_000) { compose.onAllNodesWithText("Planner").fetchSemanticsNodes().isNotEmpty() }
        assertTrue(compose.onAllNodesWithText("Lambda").fetchSemanticsNodes().isNotEmpty())
    }
}
