package com.example.network

import com.example.data.local.AccountEntity
import com.example.data.model.SecurityMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

data class RemoteMailItem(
    val uid: String,
    val rawHeaders: String,
    val bodyPlain: String,
    val isRead: Boolean = false,
    val isStarred: Boolean = false
)

data class RemoteFolderInfo(
    val name: String,
    val serverPath: String,
    val flags: Set<String>
)

/**
 * Přímý síťový klient pro protokoly IMAP4rev1/rev2, POP3 a SMTP (Implicitní TLS i STARTTLS).
 * Nepoužívá žádnou offline simulaci – vyžaduje reálné spojení se serverem.
 */
@Singleton
class ImapSmtpPop3Client @Inject constructor() {

    private val connectTimeoutMs = 10_000
    private val readTimeoutMs = 15_000

    /**
     * Ověří přímé síťové spojení a přihlášení k IMAP nebo POP3 serveru.
     */
    suspend fun testIncomingConnection(
        host: String,
        port: Int,
        security: SecurityMode,
        username: String,
        password: String,
        isPop3: Boolean = false
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            require(host.isNotBlank()) { "Hostitel příchozího serveru nesmí být prázdný." }
            require(password.isNotBlank()) { "Heslo nesmí být prázdné." }

            openProtocolSession(host, port, security, isPop3) { reader, writer, upgradeTls ->
                val greeting = reader.readLine()
                    ?: throw IllegalStateException("Server nevrátil uvítací odpověď.")
                if (isPop3) {
                    if (!greeting.startsWith("+OK")) {
                        throw IllegalStateException("Chyba POP3 serveru: $greeting")
                    }
                    if (security == SecurityMode.STARTTLS) {
                        sendLine(writer, "STLS")
                        val stlsResp = reader.readLine().orEmpty()
                        if (!stlsResp.startsWith("+OK")) {
                            throw IllegalStateException("POP3 STARTTLS selhalo: $stlsResp")
                        }
                        upgradeTls()
                    }
                    sendLine(writer, "USER $username")
                    val userResp = reader.readLine().orEmpty()
                    if (!userResp.startsWith("+OK")) {
                        throw IllegalStateException("POP3 USER odmítnuto: $userResp")
                    }
                    sendLine(writer, "PASS $password")
                    val passResp = reader.readLine().orEmpty()
                    if (!passResp.startsWith("+OK")) {
                        throw IllegalStateException("POP3 autentizace selhala: $passResp")
                    }
                    sendLine(writer, "QUIT")
                    "POP3 spojení ověřeno ($host:$port)"
                } else {
                    if (!greeting.startsWith("* OK")) {
                        throw IllegalStateException("Neočekávaná odpověď IMAP serveru: $greeting")
                    }
                    if (security == SecurityMode.STARTTLS) {
                        sendLine(writer, "A01 STARTTLS")
                        val resp = readUntilTagged(reader, "A01")
                        if (!resp.last().startsWith("A01 OK")) {
                            throw IllegalStateException("IMAP STARTTLS selhalo: ${resp.last()}")
                        }
                        upgradeTls()
                    }
                    val escapedUser = escapeImapQuoted(username)
                    val escapedPass = escapeImapQuoted(password)
                    sendLine(writer, "A02 LOGIN \"$escapedUser\" \"$escapedPass\"")
                    val loginLines = readUntilTagged(reader, "A02")
                    val statusLine = loginLines.lastOrNull().orEmpty()
                    if (!statusLine.startsWith("A02 OK")) {
                        throw IllegalStateException("IMAP přihlášení selhalo: $statusLine")
                    }
                    sendLine(writer, "A03 LOGOUT")
                    "IMAP4 spojení ověřeno ($host:$port)"
                }
            }
        }
    }

    /**
     * Stáhne nejnovější zprávy ze složky INBOX přes IMAP4rev1/rev2.
     */
    suspend fun fetchImapInboxMessages(
        account: AccountEntity,
        password: String,
        limit: Int = 25
    ): Result<List<RemoteMailItem>> = withContext(Dispatchers.IO) {
        runCatching {
            openProtocolSession(
                host = account.incomingHost,
                port = account.incomingPort,
                security = account.incomingSecurity,
                isPop3 = false
            ) { reader, writer, upgradeTls ->
                val greeting = reader.readLine()
                    ?: throw IllegalStateException("IMAP server neodpověděl.")
                if (!greeting.startsWith("* OK")) {
                    throw IllegalStateException("Neplatný IMAP greeting: $greeting")
                }
                if (account.incomingSecurity == SecurityMode.STARTTLS) {
                    sendLine(writer, "A01 STARTTLS")
                    val stls = readUntilTagged(reader, "A01")
                    if (!stls.last().startsWith("A01 OK")) {
                        throw IllegalStateException("IMAP STARTTLS selhalo.")
                    }
                    upgradeTls()
                }
                val escapedUser = escapeImapQuoted(account.username)
                val escapedPass = escapeImapQuoted(password)
                sendLine(writer, "A02 LOGIN \"$escapedUser\" \"$escapedPass\"")
                val login = readUntilTagged(reader, "A02")
                if (!login.last().startsWith("A02 OK")) {
                    throw IllegalStateException("IMAP autentizace selhala: ${login.last()}")
                }

                sendLine(writer, "A03 SELECT \"INBOX\"")
                val selectLines = readUntilTagged(reader, "A03")
                if (!selectLines.last().startsWith("A03 OK")) {
                    throw IllegalStateException("Nelze otevřít složku INBOX: ${selectLines.last()}")
                }

                val existsCount = parseImapExistsCount(selectLines)
                val items = if (existsCount > 0) {
                    val startSeq = (existsCount - limit + 1).coerceAtLeast(1)
                    sendLine(writer, "A04 FETCH $startSeq:$existsCount (UID FLAGS BODY.PEEK[])")
                    val fetchLines = readUntilTagged(reader, "A04")
                    parseImapFetchResponse(fetchLines)
                } else {
                    emptyList()
                }

                sendLine(writer, "A05 LOGOUT")
                items
            }
        }
    }

    /**
     * Stáhne zprávy přes POP3 protokol (STAT / LIST / RETR).
     */
    suspend fun fetchPop3Messages(
        account: AccountEntity,
        password: String,
        limit: Int = 20
    ): Result<List<RemoteMailItem>> = withContext(Dispatchers.IO) {
        runCatching {
            openProtocolSession(
                host = account.incomingHost,
                port = account.incomingPort,
                security = account.incomingSecurity,
                isPop3 = true
            ) { reader, writer, upgradeTls ->
                val greeting = reader.readLine().orEmpty()
                if (!greeting.startsWith("+OK")) {
                    throw IllegalStateException("POP3 server vrátil chybu: $greeting")
                }
                if (account.incomingSecurity == SecurityMode.STARTTLS) {
                    sendLine(writer, "STLS")
                    val resp = reader.readLine().orEmpty()
                    if (!resp.startsWith("+OK")) throw IllegalStateException("POP3 STLS selhalo: $resp")
                    upgradeTls()
                }
                sendLine(writer, "USER ${account.username}")
                if (!reader.readLine().orEmpty().startsWith("+OK")) {
                    throw IllegalStateException("POP3 USER odmítnuto")
                }
                sendLine(writer, "PASS $password")
                if (!reader.readLine().orEmpty().startsWith("+OK")) {
                    throw IllegalStateException("POP3 PASS odmítnuto")
                }
                sendLine(writer, "STAT")
                val statLine = reader.readLine().orEmpty()
                val totalMessages = statLine.split(" ").getOrNull(1)?.toIntOrNull() ?: 0
                val results = mutableListOf<RemoteMailItem>()
                val startIdx = (totalMessages - limit + 1).coerceAtLeast(1)
                for (msgNum in totalMessages downTo startIdx) {
                    sendLine(writer, "RETR $msgNum")
                    val retrStatus = reader.readLine().orEmpty()
                    if (retrStatus.startsWith("+OK")) {
                        val rawLines = mutableListOf<String>()
                        while (true) {
                            val line = reader.readLine() ?: break
                            if (line == ".") break
                            rawLines.add(if (line.startsWith("..")) line.substring(1) else line)
                        }
                        val fullRaw = rawLines.joinToString("\r\n")
                        val splitIdx = fullRaw.indexOf("\r\n\r\n")
                        val headers = if (splitIdx >= 0) fullRaw.substring(0, splitIdx) else fullRaw
                        val body = if (splitIdx >= 0) fullRaw.substring(splitIdx + 4) else ""
                        results.add(
                            RemoteMailItem(
                                uid = "pop3-$msgNum",
                                rawHeaders = headers,
                                bodyPlain = body.trim()
                            )
                        )
                    }
                }
                sendLine(writer, "QUIT")
                results
            }
        }
    }

    /**
     * Odešle e-mail přímo přes SMTP server (Implicitní TLS nebo STARTTLS) s AUTH PLAIN.
     */
    suspend fun sendSmtpEmail(
        account: AccountEntity,
        password: String,
        toRecipients: List<String>,
        ccRecipients: List<String>,
        bccRecipients: List<String>,
        subject: String,
        bodyPlain: String,
        inReplyToMessageId: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            require(account.smtpHost.isNotBlank()) { "SMTP server není vyplněn." }
            require(password.isNotBlank()) { "Heslo pro SMTP není k dispozici." }
            val allRecipients = (toRecipients + ccRecipients + bccRecipients)
                .map { it.trim() }
                .filter { it.contains("@") }
            require(allRecipients.isNotEmpty()) { "Zadejte alespoň jednoho platného příjemce." }

            val (generatedMessageId, rawMimeMessage) = EmailHeaderParser.buildRfc5322Message(
                fromName = account.displayName.ifBlank { account.accountName },
                fromEmail = account.emailAddress,
                to = toRecipients,
                cc = ccRecipients,
                bcc = bccRecipients,
                subject = subject,
                bodyPlain = bodyPlain,
                inReplyToMessageId = inReplyToMessageId
            )

            openProtocolSession(
                host = account.smtpHost,
                port = account.smtpPort,
                security = account.smtpSecurity,
                isPop3 = false
            ) { reader, writer, upgradeTls ->
                val greeting = readSmtpResponse(reader)
                if (!greeting.startsWith("220")) {
                    throw IllegalStateException("SMTP server odmítl spojení: $greeting")
                }

                sendLine(writer, "EHLO kuryr.android.local")
                val ehloResp = readSmtpResponse(reader)
                if (!ehloResp.startsWith("250")) {
                    throw IllegalStateException("SMTP EHLO selhalo: $ehloResp")
                }

                if (account.smtpSecurity == SecurityMode.STARTTLS) {
                    sendLine(writer, "STARTTLS")
                    val stlsResp = readSmtpResponse(reader)
                    if (!stlsResp.startsWith("220")) {
                        throw IllegalStateException("SMTP STARTTLS selhalo: $stlsResp")
                    }
                    upgradeTls()
                    sendLine(writer, "EHLO kuryr.android.local")
                    val postTlsEhlo = readSmtpResponse(reader)
                    if (!postTlsEhlo.startsWith("250")) {
                        throw IllegalStateException("SMTP EHLO po STARTTLS selhalo: $postTlsEhlo")
                    }
                }

                val authToken = Base64.getEncoder().encodeToString(
                    "\u0000${account.username}\u0000$password".toByteArray(Charsets.UTF_8)
                )
                sendLine(writer, "AUTH PLAIN $authToken")
                val authResp = readSmtpResponse(reader)
                if (!authResp.startsWith("235")) {
                    throw IllegalStateException("SMTP autentizace selhala: $authResp")
                }

                sendLine(writer, "MAIL FROM:<${account.emailAddress}>")
                val mailFromResp = readSmtpResponse(reader)
                if (!mailFromResp.startsWith("250")) {
                    throw IllegalStateException("SMTP MAIL FROM odmítnuto: $mailFromResp")
                }

                for (rcpt in allRecipients) {
                    val cleanRcpt = EmailHeaderParser.parseAddress(rcpt).second
                    sendLine(writer, "RCPT TO:<$cleanRcpt>")
                    val rcptResp = readSmtpResponse(reader)
                    if (!rcptResp.startsWith("250") && !rcptResp.startsWith("251")) {
                        throw IllegalStateException("SMTP příjemce $cleanRcpt odmítnut: $rcptResp")
                    }
                }

                sendLine(writer, "DATA")
                val dataResp = readSmtpResponse(reader)
                if (!dataResp.startsWith("354")) {
                    throw IllegalStateException("SMTP DATA odmítnuto: $dataResp")
                }

                // Dot-stuffing podle RFC 5321
                val dotStuffed = rawMimeMessage.replace("\r\n.", "\r\n..")
                writer.write(dotStuffed)
                if (!dotStuffed.endsWith("\r\n")) {
                    writer.write("\r\n")
                }
                writer.write(".\r\n")
                writer.flush()

                val endDataResp = readSmtpResponse(reader)
                if (!endDataResp.startsWith("250")) {
                    throw IllegalStateException("SMTP odeslání dat selhalo: $endDataResp")
                }

                sendLine(writer, "QUIT")
                generatedMessageId
            }
        }
    }

    /**
     * Parsuje počet zpráv z IMAP SELECT odpovědi (* N EXISTS).
     */
    fun parseImapExistsCount(selectLines: List<String>): Int {
        val existsRegex = Regex("^\\*\\s+(\\d+)\\s+EXISTS", RegexOption.IGNORE_CASE)
        for (line in selectLines) {
            val match = existsRegex.find(line.trim())
            if (match != null) {
                return match.groupValues[1].toIntOrNull() ?: 0
            }
        }
        return 0
    }

    /**
     * Parsuje IMAP LIST odpovědi (* LIST (\HasNoChildren \Sent) "/" "Sent").
     */
    fun parseImapListResponse(listLines: List<String>): List<RemoteFolderInfo> {
        val listRegex = Regex("^\\*\\s+LIST\\s+\\(([^)]*)\\)\\s+\"?([^\"]*)\"?\\s+\"?([^\"]+)\"?$", RegexOption.IGNORE_CASE)
        return listLines.mapNotNull { line ->
            val match = listRegex.find(line.trim()) ?: return@mapNotNull null
            val rawFlags = match.groupValues[1]
                .split(Regex("\\s+"))
                .filter { it.isNotBlank() }
                .toSet()
            val path = match.groupValues[3].trim()
            val displayName = path.substringAfterLast('/').substringAfterLast('.')
            RemoteFolderInfo(
                name = displayName.ifBlank { path },
                serverPath = path,
                flags = rawFlags
            )
        }
    }

    /**
     * Parsuje víceřádkovou IMAP FETCH odpověď do seznamu RemoteMailItem.
     */
    fun parseImapFetchResponse(fetchLines: List<String>): List<RemoteMailItem> {
        val items = mutableListOf<RemoteMailItem>()
        var currentUid: String? = null
        var currentRead = false
        var currentStarred = false
        val currentBuffer = mutableListOf<String>()

        fun flushCurrent() {
            val uid = currentUid ?: return
            if (currentBuffer.isEmpty()) return
            val fullText = currentBuffer.joinToString("\r\n")
            val splitIdx = fullText.indexOf("\r\n\r\n")
            val rawHeaders = if (splitIdx >= 0) fullText.substring(0, splitIdx) else fullText
            val rawBody = if (splitIdx >= 0) fullText.substring(splitIdx + 4).removeSuffix(")") else ""
            items.add(
                RemoteMailItem(
                    uid = uid,
                    rawHeaders = rawHeaders.trim(),
                    bodyPlain = rawBody.trim(),
                    isRead = currentRead,
                    isStarred = currentStarred
                )
            )
            currentBuffer.clear()
        }

        val fetchStartRegex = Regex("^\\*\\s+\\d+\\s+FETCH\\s+\\((.*)$", RegexOption.IGNORE_CASE)
        val uidRegex = Regex("UID\\s+(\\d+)", RegexOption.IGNORE_CASE)
        val flagsRegex = Regex("FLAGS\\s+\\(([^)]*)\\)", RegexOption.IGNORE_CASE)

        for (line in fetchLines) {
            if (line.startsWith("A0", ignoreCase = true)) continue
            val startMatch = fetchStartRegex.find(line)
            if (startMatch != null) {
                flushCurrent()
                val meta = startMatch.groupValues[1]
                currentUid = uidRegex.find(meta)?.groupValues?.get(1) ?: "imap-${items.size + 1}"
                val flagsStr = flagsRegex.find(meta)?.groupValues?.get(1).orEmpty()
                currentRead = flagsStr.contains("\\Seen", ignoreCase = true)
                currentStarred = flagsStr.contains("\\Flagged", ignoreCase = true)
            } else if (currentUid != null) {
                currentBuffer.add(line)
            }
        }
        flushCurrent()
        return items
    }

    private fun <T> openProtocolSession(
        host: String,
        port: Int,
        security: SecurityMode,
        isPop3: Boolean,
        block: (BufferedReader, BufferedWriter, () -> Unit) -> T
    ): T {
        val sslFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
        var socket: Socket = if (security == SecurityMode.IMPLICIT_TLS) {
            val raw = Socket()
            raw.connect(InetSocketAddress(host, port), connectTimeoutMs)
            raw.soTimeout = readTimeoutMs
            (sslFactory.createSocket(raw, host, port, true) as SSLSocket).apply {
                soTimeout = readTimeoutMs
                startHandshake()
            }
        } else {
            Socket().apply {
                connect(InetSocketAddress(host, port), connectTimeoutMs)
                soTimeout = readTimeoutMs
            }
        }

        return socket.use { activeSocket ->
            var reader = BufferedReader(InputStreamReader(activeSocket.getInputStream(), Charsets.UTF_8))
            var writer = BufferedWriter(OutputStreamWriter(activeSocket.getOutputStream(), Charsets.UTF_8))

            val upgradeToTls: () -> Unit = {
                val upgraded = (sslFactory.createSocket(activeSocket, host, port, true) as SSLSocket).apply {
                    soTimeout = readTimeoutMs
                    startHandshake()
                }
                socket = upgraded
                reader = BufferedReader(InputStreamReader(upgraded.getInputStream(), Charsets.UTF_8))
                writer = BufferedWriter(OutputStreamWriter(upgraded.getOutputStream(), Charsets.UTF_8))
            }

            block(reader, writer, upgradeToTls)
        }
    }

    private fun sendLine(writer: BufferedWriter, command: String) {
        writer.write(command)
        writer.write("\r\n")
        writer.flush()
    }

    private fun readUntilTagged(reader: BufferedReader, tag: String): List<String> {
        val lines = mutableListOf<String>()
        while (true) {
            val line = reader.readLine() ?: break
            lines.add(line)
            if (line.startsWith("$tag ", ignoreCase = true)) {
                break
            }
        }
        return lines
    }

    private fun readSmtpResponse(reader: BufferedReader): String {
        val sb = StringBuilder()
        while (true) {
            val line = reader.readLine() ?: break
            sb.append(line).append("\n")
            if (line.length >= 4 && line[3] == ' ') {
                break
            }
            if (line.length == 3) {
                break
            }
        }
        return sb.toString().trim()
    }

    private fun escapeImapQuoted(value: String): String {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
    }
}
