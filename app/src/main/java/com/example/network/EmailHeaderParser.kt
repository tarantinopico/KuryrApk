package com.example.network

import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Base64
import java.util.Date
import java.util.Locale
import java.util.UUID

data class ParsedEmailHeaders(
    val subject: String,
    val fromName: String,
    val fromEmail: String,
    val to: String,
    val cc: String,
    val replyTo: String,
    val messageId: String,
    val threadId: String,
    val dateEpochMillis: Long,
    val spfStatus: String,
    val dkimStatus: String,
    val contentType: String,
    val rawHeadersMap: Map<String, String>
)

/**
 * RFC 5322 a RFC 2047 parser e-mailových hlaviček a sestavovač MIME zpráv.
 */
object EmailHeaderParser {

    private val encodedWordRegex = Regex("=\\?([^?]+)\\?([BbQq])\\?([^?]+)\\?=")
    private val angleAddrRegex = Regex("^(.*?)<([^>]+)>\\s*$")

    /**
     * Rozparsuje surový blok hlaviček RFC 5322 (včetně víceřádkového zalamování - folding).
     */
    fun parseHeaders(rawHeaderBlock: String): ParsedEmailHeaders {
        val unfoldedLines = mutableListOf<String>()
        rawHeaderBlock.replace("\r\n", "\n").lines().forEach { line ->
            if (line.isEmpty()) return@forEach
            if ((line.startsWith(" ") || line.startsWith("\t")) && unfoldedLines.isNotEmpty()) {
                val lastIdx = unfoldedLines.lastIndex
                unfoldedLines[lastIdx] = unfoldedLines[lastIdx] + " " + line.trim()
            } else {
                unfoldedLines.add(line)
            }
        }

        val headerMap = linkedMapOf<String, String>()
        for (line in unfoldedLines) {
            val colonIdx = line.indexOf(':')
            if (colonIdx > 0) {
                val key = line.substring(0, colonIdx).trim().lowercase(Locale.ROOT)
                val value = line.substring(colonIdx + 1).trim()
                // Pokud se hlavička opakuje (např. Authentication-Results), spojíme je
                headerMap[key] = if (headerMap.containsKey(key)) {
                    "${headerMap[key]}; $value"
                } else {
                    value
                }
            }
        }

        val decodedSubject = decodeRfc2047(headerMap["subject"] ?: "(Bez předmětu)")
        val rawFrom = decodeRfc2047(headerMap["from"] ?: "")
        val (fromName, fromEmail) = parseAddress(rawFrom)
        val to = decodeRfc2047(headerMap["to"] ?: "")
        val cc = decodeRfc2047(headerMap["cc"] ?: "")
        val replyTo = decodeRfc2047(headerMap["reply-to"] ?: fromEmail)
        val messageId = headerMap["message-id"]?.trim() ?: "<${UUID.randomUUID()}@kuryr.local>"
        val inReplyTo = headerMap["in-reply-to"]?.trim()
        val references = headerMap["references"]?.trim()
        val threadId = when {
            !references.isNullOrBlank() -> references.split(Regex("\\s+")).first()
            !inReplyTo.isNullOrBlank() -> inReplyTo
            else -> messageId
        }

        val authResults = headerMap["authentication-results"].orEmpty().lowercase(Locale.ROOT)
        val receivedSpf = headerMap["received-spf"].orEmpty().lowercase(Locale.ROOT)

        val spfStatus = when {
            authResults.contains("spf=pass") || receivedSpf.startsWith("pass") -> "PASS"
            authResults.contains("spf=fail") || receivedSpf.startsWith("fail") -> "FAIL"
            authResults.contains("spf=softfail") || receivedSpf.startsWith("softfail") -> "SOFTFAIL"
            else -> "NEUTRAL"
        }

        val dkimStatus = when {
            authResults.contains("dkim=pass") -> "PASS"
            authResults.contains("dkim=fail") -> "FAIL"
            headerMap.containsKey("dkim-signature") -> "SIGNED"
            else -> "NONE"
        }

        val dateMillis = parseRfc2822Date(headerMap["date"])

        return ParsedEmailHeaders(
            subject = decodedSubject,
            fromName = fromName,
            fromEmail = fromEmail,
            to = to,
            cc = cc,
            replyTo = replyTo,
            messageId = messageId,
            threadId = threadId,
            dateEpochMillis = dateMillis,
            spfStatus = spfStatus,
            dkimStatus = dkimStatus,
            contentType = headerMap["content-type"] ?: "text/plain; charset=utf-8",
            rawHeadersMap = headerMap
        )
    }

    /**
     * Dekóduje RFC 2047 encoded-words (=?charset?encoding?text?=).
     */
    fun decodeRfc2047(input: String): String {
        if (!input.contains("=?")) return input.trim()
        val collapsed = input.replace(Regex("\\?=\\s+=\\?"), "?==?")
        return encodedWordRegex.replace(collapsed) { match ->
            val charsetName = match.groupValues[1]
            val encoding = match.groupValues[2].uppercase(Locale.ROOT)
            val encodedText = match.groupValues[3]
            runCatching {
                val charset = Charset.forName(charsetName)
                val bytes = when (encoding) {
                    "B" -> Base64.getDecoder().decode(encodedText.replace(" ", ""))
                    "Q" -> decodeQuotedPrintableWord(encodedText)
                    else -> encodedText.toByteArray(Charsets.UTF_8)
                }
                String(bytes, charset)
            }.getOrDefault(match.value)
        }.trim()
    }

    private fun decodeQuotedPrintableWord(text: String): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            when {
                ch == '_' -> {
                    out.write(' '.code)
                    i++
                }
                ch == '=' && i + 2 < text.length -> {
                    val hex = text.substring(i + 1, i + 3)
                    val value = hex.toIntOrNull(16)
                    if (value != null) {
                        out.write(value)
                        i += 3
                    } else {
                        out.write(ch.code)
                        i++
                    }
                }
                else -> {
                    out.write(ch.code)
                    i++
                }
            }
        }
        return out.toByteArray()
    }

    fun parseAddress(rawAddress: String): Pair<String, String> {
        val trimmed = rawAddress.trim()
        val match = angleAddrRegex.find(trimmed)
        return if (match != null) {
            val rawName = match.groupValues[1].trim().removeSurrounding("\"").trim()
            val email = match.groupValues[2].trim()
            val finalName = rawName.ifBlank { email.substringBefore('@') }
            finalName to email
        } else {
            val cleanEmail = trimmed.removeSurrounding("\"")
            val nameFallback = cleanEmail.substringBefore('@').ifBlank { "Neznámý odesílatel" }
            nameFallback to cleanEmail
        }
    }

    fun parseRfc2822Date(dateHeader: String?): Long {
        if (dateHeader.isNullOrBlank()) return System.currentTimeMillis()
        val cleaned = dateHeader.replace(Regex("\\s*\\([^)]*\\)\\s*$"), "").trim()
        val patterns = listOf(
            "EEE, d MMM yyyy HH:mm:ss Z",
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "d MMM yyyy HH:mm:ss Z",
            "EEE, d MMM yyyy HH:mm Z"
        )
        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US)
                val parsed = sdf.parse(cleaned)
                if (parsed != null) return parsed.time
            } catch (_: Exception) {
                // Zkusíme další formát
            }
        }
        return System.currentTimeMillis()
    }

    /**
     * Sestaví RFC 5322 kompatibilní e-mailovou zprávu pro odeslání přes SMTP DATA.
     */
    fun buildRfc5322Message(
        fromName: String,
        fromEmail: String,
        to: List<String>,
        cc: List<String>,
        bcc: List<String>,
        subject: String,
        bodyPlain: String,
        inReplyToMessageId: String? = null
    ): Pair<String, String> {
        val messageId = "<${UUID.randomUUID()}@${fromEmail.substringAfter('@', "kuryr.local")}>"
        val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US)
        val encodedSubject = encodeRfc2047IfNeeded(subject)
        val encodedFromName = encodeRfc2047IfNeeded(fromName)

        val sb = StringBuilder()
        sb.append("Date: ${dateFormat.format(Date())}\r\n")
        sb.append("From: \"$encodedFromName\" <$fromEmail>\r\n")
        sb.append("To: ${to.joinToString(", ")}\r\n")
        if (cc.isNotEmpty()) {
            sb.append("Cc: ${cc.joinToString(", ")}\r\n")
        }
        sb.append("Message-ID: $messageId\r\n")
        if (!inReplyToMessageId.isNullOrBlank()) {
            sb.append("In-Reply-To: $inReplyToMessageId\r\n")
            sb.append("References: $inReplyToMessageId\r\n")
        }
        sb.append("Subject: $encodedSubject\r\n")
        sb.append("MIME-Version: 1.0\r\n")
        sb.append("User-Agent: Kuryr-Android/1.0\r\n")
        sb.append("Content-Type: text/plain; charset=UTF-8\r\n")
        sb.append("Content-Transfer-Encoding: 8bit\r\n")
        sb.append("\r\n")
        sb.append(bodyPlain.replace("\n", "\r\n"))
        sb.append("\r\n")
        return messageId to sb.toString()
    }

    fun encodeRfc2047IfNeeded(text: String): String {
        if (text.all { it.code in 32..126 }) return text
        val b64 = Base64.getEncoder().encodeToString(text.toByteArray(Charsets.UTF_8))
        return "=?UTF-8?B?$b64?="
    }
}
