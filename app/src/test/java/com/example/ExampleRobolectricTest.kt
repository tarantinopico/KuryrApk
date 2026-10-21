package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.preferences.AppPreferencesRepository
import com.example.data.repository.ContactRepository
import com.example.data.repository.MailRepository
import com.example.data.security.InMemoryPasswordVault
import com.example.network.ImapSmtpPop3Client
import com.example.network.JmapClient
import com.example.notifications.NotificationRuleEngine
import com.example.ui.KuryrViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createTestViewModel(): KuryrViewModel {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val fakeDao = FakeKuryrDao()
        val vault = InMemoryPasswordVault()
        val mailRepo = MailRepository(
            dao = fakeDao,
            passwordVault = vault,
            imapSmtpPop3Client = ImapSmtpPop3Client(),
            jmapClient = JmapClient()
        )
        val contactRepo = ContactRepository(fakeDao)
        val prefsRepo = AppPreferencesRepository(context)
        val ruleEngine = NotificationRuleEngine(fakeDao)
        return KuryrViewModel(
            appContext = context,
            mailRepository = mailRepo,
            contactRepository = contactRepo,
            preferencesRepository = prefsRepo,
            notificationRuleEngine = ruleEngine
        )
    }

    @Test
    fun `onboarding flow and adding account navigates to unified inbox`() {
        val viewModel = createTestViewModel()
        composeTestRule.setContent {
            KuryrApp(viewModel = viewModel)
        }
        composeTestRule.waitForIdle()

        // 1. Ověření zobrazení Onboarding obrazovky
        composeTestRule.onNodeWithTag("onboarding_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("onboarding_start_button").performClick()
        composeTestRule.waitForIdle()

        // 2. Ověření průvodce přidáním účtu
        composeTestRule.onNodeWithTag("add_account_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("account_email_input").performTextInput("jan.novak@seznam.cz")
        composeTestRule.onNodeWithTag("account_password_input").performTextInput("tajneHeslo123")
        composeTestRule.onNodeWithTag("save_account_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // 3. Po uložení účtu se zobrazí Unified Inbox
        composeTestRule.onNodeWithTag("unified_inbox_screen").assertIsDisplayed()
    }

    @Test
    fun `compose email flow and dark mode toggle work properly`() {
        val viewModel = createTestViewModel()
        composeTestRule.setContent {
            KuryrApp(viewModel = viewModel)
        }
        composeTestRule.waitForIdle()

        // Přidáme účet, abychom byli v Unified Inboxu
        composeTestRule.onNodeWithTag("onboarding_start_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("account_email_input").performTextInput("redaktor@posteo.de")
        composeTestRule.onNodeWithTag("account_password_input").performTextInput("posteoAppPass")
        composeTestRule.onNodeWithTag("save_account_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Otevřeme editor nové zprávy (Compose)
        composeTestRule.onNodeWithTag("fab_compose").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("compose_email_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("compose_to_input").performTextInput("kolega@seznam.cz")
        composeTestRule.onNodeWithTag("compose_subject_input").performTextInput("Zpráva z testu Kurýr")
        composeTestRule.onNodeWithTag("compose_body_input").performTextInput("Text e-mailu pro kontrolu odeslání.")
        composeTestRule.onNodeWithTag("save_draft_button").performClick()
        composeTestRule.waitForIdle()

        // Přejdeme do Nastavení a přepneme tmavý / AMOLED režim
        composeTestRule.onNodeWithTag("nav_settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("theme_mode_DARK").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("theme_mode_AMOLED").performClick()
        composeTestRule.waitForIdle()
    }
}
