package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun onboardingToAccountSetupToComposeAndDarkModeFlow() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("onboarding_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("onboarding_start_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("add_account_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("account_email_input").performTextInput("uzivatel@seznam.cz")
        composeRule.onNodeWithTag("account_password_input").performTextInput("hesloAplikace123")
        composeRule.onNodeWithTag("save_account_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("unified_inbox_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("fab_compose").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("compose_email_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("compose_to_input").performTextInput("prijemce@posteo.de")
        composeRule.onNodeWithTag("compose_subject_input").performTextInput("Test z Kurýra")
        composeRule.onNodeWithTag("save_draft_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("nav_settings").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("theme_mode_DARK").performClick()
        composeRule.waitForIdle()
    }
}
