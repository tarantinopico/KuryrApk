package com.example

import com.example.network.EmailHeaderParser
import com.example.network.ImapSmtpPop3Client
import com.example.network.JmapClient
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NetworkAndHeaderUnitTest {

    @Test
    fun `parse RFC 5322 folded headers and RFC 2047 UTF-8 encoded words`() {
        val rawHeaders = """
            From: =?UTF-8?B?SmFuIE5vdsO9?= <jan.novy@seznam.cz>
            To: "Klara Dvorakova" <klara@posteo.de>
            Subject: =?UTF-8?Q?Pozv=C3=A1nka_na_sch=C5=AFzku?=
             =?UTF-8?Q?_Kur=C3=BDr?=
            Message-ID: <msg-9001@seznam.cz>
            References: <thread-root-01@seznam.cz> <msg-9000@seznam.cz>
            Authentication-Results: mx.posteo.de; spf=pass; dkim=pass
            Date: Tue, 20 Oct 2026 14:30:00 +0200
        """.trimIndent()

        val parsed = EmailHeaderParser.parseHeaders(rawHeaders)
        assertEquals("Jan Nový", parsed.fromName)
        assertEquals("jan.novy@seznam.cz", parsed.fromEmail)
        assertEquals("Pozvánka na schůzku Kurýr", parsed.subject)
        assertEquals("<msg-9001@seznam.cz>", parsed.messageId)
        assertEquals("<thread-root-01@seznam.cz>", parsed.threadId)
        assertEquals("PASS", parsed.spfStatus)
        assertEquals("PASS", parsed.dkimStatus)
    }

    @Test
    fun `parse IMAP LIST and FETCH untagged server responses`() {
        val client = ImapSmtpPop3Client()
        val listLines = listOf(
            "* LIST (\\HasNoChildren \\Inbox) \"/\" \"INBOX\"",
            "* LIST (\\HasNoChildren \\Sent) \"/\" \"Sent\"",
            "A02 OK LIST completed"
        )
        val folders = client.parseImapListResponse(listLines)
        assertEquals(2, folders.size)
        assertEquals("INBOX", folders[0].serverPath)
        assertTrue(folders[0].flags.contains("\\Inbox"))

        val fetchLines = listOf(
            "* 1 FETCH (UID 501 FLAGS (\\Seen \\Flagged) BODY[] {120}",
            "From: Eva <eva@fastmail.com>",
            "Subject: Projekt Kuryr",
            "Message-ID: <501@fastmail.com>",
            "",
            "Obsah prvni zpravy v tele.",
            ")",
            "A04 OK FETCH completed"
        )
        val messages = client.parseImapFetchResponse(fetchLines)
        assertEquals(1, messages.size)
        assertEquals("501", messages[0].uid)
        assertTrue(messages[0].isRead)
        assertTrue(messages[0].isStarred)
        assertEquals("Obsah prvni zpravy v tele.", messages[0].bodyPlain)
    }

    @Test
    fun `JMAP client discovers session and fetches emails from MockWebServer`() = runTest {
        val server = MockWebServer()
        server.start()
        try {
            val apiUrl = server.url("/jmap/api/").toString()
            val sessionJson = """
                {
                  "username": "uzivatel@sternamail.org",
                  "apiUrl": "$apiUrl",
                  "primaryAccounts": {
                    "urn:ietf:params:jmap:mail": "acc-sterna-1"
                  }
                }
            """.trimIndent()
            server.enqueue(MockResponse().setResponseCode(200).setBody(sessionJson))

            val emailResponseJson = """
                {
                  "methodResponses": [
                    ["Email/query", {"accountId": "acc-sterna-1", "ids": ["m101"]}, "q0"],
                    ["Email/get", {
                      "accountId": "acc-sterna-1",
                      "list": [
                        {
                          "id": "m101",
                          "threadId": "t101",
                          "subject": "Zpráva přes JMAP protokol",
                          "from": [{"name": "Podpora Sterna", "email": "info@sternamail.org"}],
                          "to": [{"email": "uzivatel@sternamail.org"}],
                          "preview": "Ukázka rychlého JMAP přenosu",
                          "receivedAt": "2026-10-21T08:15:00Z",
                          "keywords": {"${'$'}seen": true, "${'$'}flagged": true},
                          "hasAttachment": false,
                          "bodyValues": {
                            "p1": {"value": "Plný text JMAP e-mailu z mock serveru."}
                          }
                        }
                      ]
                    }, "g0"]
                  ]
                }
            """.trimIndent()
            server.enqueue(MockResponse().setResponseCode(200).setBody(emailResponseJson))

            val jmapClient = JmapClient()
            val sessionUrl = server.url("/.well-known/jmap").toString()
            val sessionResult = jmapClient.fetchSession(sessionUrl, "uzivatel@sternamail.org", "tajneHeslo")
            assertTrue(sessionResult.isSuccess)
            val session = sessionResult.getOrThrow()
            assertEquals("acc-sterna-1", session.mailAccountId)

            val emailsResult = jmapClient.fetchRecentEmails(session, "uzivatel@sternamail.org", "tajneHeslo")
            assertTrue(emailsResult.isSuccess)
            val emails = emailsResult.getOrThrow()
            assertEquals(1, emails.size)
            assertEquals("Zpráva přes JMAP protokol", emails[0].subject)
            assertEquals("Podpora Sterna", emails[0].fromName)
            assertTrue(emails[0].isSeen)
            assertTrue(emails[0].isFlagged)
        } finally {
            server.shutdown()
        }
    }
}
