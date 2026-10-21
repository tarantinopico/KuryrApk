package com.example

import com.example.data.local.AccountEntity
import com.example.data.local.ContactEntity
import com.example.data.local.EmailMessageEntity
import com.example.data.local.NotificationRuleEntity
import com.example.data.model.DigestFrequency
import com.example.data.model.FolderType
import com.example.data.model.MailProtocol
import com.example.data.model.SecurityMode
import com.example.data.model.SwipeAction
import com.example.data.preferences.UserPreferences
import com.example.data.repository.ContactRepository
import com.example.data.repository.MailRepository
import com.example.data.security.InMemoryPasswordVault
import com.example.network.ImapSmtpPop3Client
import com.example.network.JmapClient
import com.example.notifications.NotificationDecisionType
import com.example.notifications.NotificationRuleEngine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RepositoryAndNotificationUnitTest {

    @Test
    fun `MailRepository creates account with encrypted password and default folders and executes swipe actions`() = runTest {
        val fakeDao = FakeKuryrDao()
        val vault = InMemoryPasswordVault()
        val repository = MailRepository(
            dao = fakeDao,
            passwordVault = vault,
            imapSmtpPop3Client = ImapSmtpPop3Client(),
            jmapClient = JmapClient()
        )

        val accountId = repository.addAccountWithFolders(
            account = AccountEntity(
                accountName = "Seznam Osobní",
                displayName = "Jan Kurýr",
                emailAddress = "jan@seznam.cz",
                username = "jan@seznam.cz",
                providerId = "seznam",
                protocol = MailProtocol.IMAP,
                incomingHost = "imap.seznam.cz",
                incomingPort = 993,
                incomingSecurity = SecurityMode.IMPLICIT_TLS,
                smtpHost = "smtp.seznam.cz",
                smtpPort = 465,
                smtpSecurity = SecurityMode.IMPLICIT_TLS
            ),
            password = "superTajneAppHeslo123"
        )

        assertEquals("superTajneAppHeslo123", repository.getAccountPassword(accountId))
        val folders = fakeDao.getFoldersByAccountOnce(accountId)
        assertEquals(6, folders.size)
        val inbox = folders.first { it.type == FolderType.INBOX }
        val archive = folders.first { it.type == FolderType.ARCHIVE }

        val msgId = fakeDao.insertMessage(
            EmailMessageEntity(
                accountId = accountId,
                folderId = inbox.id,
                remoteUid = "uid-1",
                messageIdHeader = "<1@seznam.cz>",
                threadId = "<1@seznam.cz>",
                subject = "Testovací zpráva",
                fromName = "Test",
                fromEmail = "test@seznam.cz",
                toRecipients = "jan@seznam.cz",
                snippet = "Ukázka",
                bodyPlain = "Plný text",
                receivedAt = 1000L,
                isRead = false
            )
        )

        val inserted = fakeDao.getMessageById(msgId)!!
        repository.executeSwipeAction(inserted, SwipeAction.TOGGLE_READ)
        assertTrue(fakeDao.getMessageById(msgId)!!.isRead)

        repository.executeSwipeAction(fakeDao.getMessageById(msgId)!!, SwipeAction.ARCHIVE)
        assertEquals(archive.id, fakeDao.getMessageById(msgId)!!.folderId)
    }

    @Test
    fun `ContactRepository saves contacts and filters by fulltext search`() = runTest {
        val fakeDao = FakeKuryrDao()
        val contactRepo = ContactRepository(fakeDao)

        contactRepo.saveContact(
            ContactEntity(
                displayName = "Petra Svobodová",
                email = "petra@fastmail.com",
                organization = "Redakce Praha",
                isVip = true
            )
        )
        contactRepo.saveContact(
            ContactEntity(
                displayName = "Martin Dvořák",
                email = "martin@posteo.de",
                organization = "Studio Brno",
                isVip = false
            )
        )

        val searchResults = contactRepo.searchContacts("Redakce").first()
        assertEquals(1, searchResults.size)
        assertEquals("petra@fastmail.com", searchResults.first().email)
    }

    @Test
    fun `NotificationRuleEngine handles quiet hours, VIP bypass, custom rules and digest formatting`() {
        val fakeDao = FakeKuryrDao()
        val engine = NotificationRuleEngine(fakeDao)

        val quietPrefs = UserPreferences(
            quietHoursEnabled = true,
            quietHoursStartHour = 22,
            quietHoursEndHour = 7,
            digestFrequency = DigestFrequency.DAILY
        )

        val invoiceRule = NotificationRuleEntity(
            id = 10L,
            name = "Faktury s přílohou",
            isEnabled = true,
            subjectContains = "Faktura",
            requiresAttachment = true,
            bypassQuietHours = true,
            highPriority = true
        )

        val regularMessageAtNight = EmailMessageEntity(
            id = 1L,
            accountId = 1L,
            folderId = 1L,
            remoteUid = "u1",
            messageIdHeader = "<m1>",
            threadId = "<m1>",
            subject = "Běžný newsletter",
            fromName = "Zpravodaj",
            fromEmail = "info@example.com",
            toRecipients = "me@seznam.cz",
            snippet = "Novinky",
            bodyPlain = "Obsah",
            receivedAt = 2000L,
            isRead = false,
            isVipSender = false,
            hasAttachments = false
        )

        // Během tichých hodin (23:00) se běžná zpráva zařadí do fronty denního souhrnu
        val evalRegular = engine.evaluateMessage(
            message = regularMessageAtNight,
            enabledRules = listOf(invoiceRule),
            preferences = quietPrefs,
            currentHourOfDay = 23
        )
        assertEquals(NotificationDecisionType.QUEUE_FOR_DIGEST, evalRegular.decision)

        // Zpráva odpovídající prioritnímu pravidlu s přílohou obejde tiché hodiny
        val invoiceMessageAtNight = regularMessageAtNight.copy(
            id = 2L,
            subject = "Faktura za říjen 2026",
            hasAttachments = true
        )
        val evalInvoice = engine.evaluateMessage(
            message = invoiceMessageAtNight,
            enabledRules = listOf(invoiceRule),
            preferences = quietPrefs,
            currentHourOfDay = 23
        )
        assertEquals(NotificationDecisionType.NOTIFY_HIGH_PRIORITY, evalInvoice.decision)
        assertNotNull(evalInvoice.matchedRule)

        // Kontrola formátování souhrnu
        val summary = engine.formatDigestSummary(
            items = listOf(
                com.example.data.local.DigestQueueEntity(
                    id = 1L,
                    messageId = 1L,
                    accountId = 1L,
                    sender = "Zpravodaj",
                    subject = "Běžný newsletter",
                    receivedAt = 2000L
                )
            ),
            frequency = DigestFrequency.DAILY
        )
        assertTrue(summary.contains("Denní souhrn (1 zpráv)"))
        assertTrue(summary.contains("Zpravodaj: Běžný newsletter"))
        assertFalse(engine.isWithinQuietHours(true, 22, 7, 12))
        assertTrue(engine.isWithinQuietHours(true, 22, 7, 2))
    }
}
