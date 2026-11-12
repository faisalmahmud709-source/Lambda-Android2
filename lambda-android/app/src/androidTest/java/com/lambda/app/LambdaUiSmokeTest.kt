package com.lambda.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import org.junit.Rule
import org.junit.Test

class LambdaUiSmokeTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun homeOpensAndShowsLambdaBrand() {
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Lambda").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Lambda").assertIsDisplayed()
        compose.onNodeWithText("Planner").assertIsDisplayed()
    }
}
