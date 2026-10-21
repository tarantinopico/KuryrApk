package com.example.network

import com.example.data.model.FolderType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class JmapSessionInfo(
    val apiUrl: String,
    val mailAccountId: String,
    val username: String
)

data class JmapMailboxDto(
    val id: String,
    val name: String,
    val role: String?,
    val unreadEmails: Int,
    val totalEmails: Int
)

data class JmapEmailDto(
    val id: String,
    val threadId: String,
    val mailboxIds: Set<String>,
    val subject: String,
    val fromName: String,
    val fromEmail: String,
    val toFormatted: String,
    val preview: String,
    val bodyPlain: String,
    val receivedAtEpochMillis: Long,
    val isSeen: Boolean,
    val isFlagged: Boolean,
    val hasAttachment: Boolean
)

/**
 * Pure-Kotlin JMAP klient podle RFC 8620 (JMAP Core) a RFC 8621 (JMAP for Mail),
 * navržený podle architektury Sterna Mail / Ltt.rs.
 */
@Singleton
class JmapClient @Inject constructor() {

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Objeví JMAP Session na /.well-known/jmap nebo zadaném endpointu.
     */
    suspend fun fetchSession(
        sessionEndpointUrl: String,
        username: String,
        password: String
    ): Result<JmapSessionInfo> = withContext(Dispatchers.IO) {
        runCatching {
            require(sessionEndpointUrl.startsWith("http")) {
                "JMAP endpoint musí začínat http:// nebo https://"
            }
            val request = Request.Builder()
                .url(sessionEndpointUrl)
                .header("Authorization", Credentials.basic(username, password, Charsets.UTF_8))
                .header("Accept", "application/json")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("JMAP Session HTTP ${response.code}: ${response.message}")
                }
                val bodyStr = response.body?.string().orEmpty()
                parseSessionJson(bodyStr)
            }
        }
    }

    /**
     * Stáhne schránky (Mailbox/get) přes JMAP API.
     */
    suspend fun fetchMailboxes(
        session: JmapSessionInfo,
        username: String,
        password: String
    ): Result<List<JmapMailboxDto>> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = buildMailboxGetRequestJson(session.mailAccountId)
            val request = Request.Builder()
                .url(session.apiUrl)
                .header("Authorization", Credentials.basic(username, password, Charsets.UTF_8))
                .post(payload.toRequestBody(jsonMediaType))
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("JMAP Mailbox/get selhalo s kódem HTTP ${response.code}")
                }
                parseMailboxGetResponseJson(response.body?.string().orEmpty())
            }
        }
    }

    /**
     * Stáhne nejnovější e-maily (Email/query + Email/get s back-referencí #ids) v jednom JMAP volání.
     */
    suspend fun fetchRecentEmails(
        session: JmapSessionInfo,
        username: String,
        password: String,
        limit: Int = 25
    ): Result<List<JmapEmailDto>> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = buildEmailQueryAndGetRequestJson(session.mailAccountId, limit)
            val request = Request.Builder()
                .url(session.apiUrl)
                .header("Authorization", Credentials.basic(username, password, Charsets.UTF_8))
                .post(payload.toRequestBody(jsonMediaType))
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("JMAP Email/get selhalo s kódem HTTP ${response.code}")
                }
                parseEmailGetResponseJson(response.body?.string().orEmpty())
            }
        }
    }

    /**
     * Odešle e-mail přes JMAP (Email/set create + EmailSubmission/set).
     */
    suspend fun sendEmail(
        session: JmapSessionInfo,
        username: String,
        password: String,
        fromName: String,
        fromEmail: String,
        toEmails: List<String>,
        subject: String,
        bodyPlain: String
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = buildEmailSendRequestJson(
                accountId = session.mailAccountId,
                fromName = fromName,
                fromEmail = fromEmail,
                toEmails = toEmails,
                subject = subject,
                bodyPlain = bodyPlain
            )
            val request = Request.Builder()
                .url(session.apiUrl)
                .header("Authorization", Credentials.basic(username, password, Charsets.UTF_8))
                .post(payload.toRequestBody(jsonMediaType))
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("JMAP odeslání selhalo: HTTP ${response.code}")
                }
                val body = response.body?.string().orEmpty()
                parseEmailSendResponseJson(body)
            }
        }
    }

    fun parseSessionJson(jsonString: String): JmapSessionInfo {
        val root = JSONObject(jsonString)
        val apiUrl = root.optString("apiUrl")
        require(apiUrl.isNotBlank()) { "V JMAP Session chybí pole apiUrl." }
        val primaryAccounts = root.optJSONObject("primaryAccounts")
        val mailAccountId = primaryAccounts?.optString("urn:ietf:params:jmap:mail").orEmpty()
            .ifBlank {
                root.optJSONObject("accounts")?.keys()?.asSequence()?.firstOrNull().orEmpty()
            }
        require(mailAccountId.isNotBlank()) { "V JMAP Session nebyl nalezen žádný mailový účet." }
        val username = root.optString("username", "")
        return JmapSessionInfo(
            apiUrl = apiUrl,
            mailAccountId = mailAccountId,
            username = username
        )
    }

    fun buildMailboxGetRequestJson(accountId: String): String {
        val root = JSONObject()
        root.put(
            "using",
            JSONArray()
                .put("urn:ietf:params:jmap:core")
                .put("urn:ietf:params:jmap:mail")
        )
        val args = JSONObject().put("accountId", accountId)
        val call = JSONArray().put("Mailbox/get").put(args).put("c0")
        root.put("methodCalls", JSONArray().put(call))
        return root.toString()
    }

    fun parseMailboxGetResponseJson(jsonString: String): List<JmapMailboxDto> {
        val root = JSONObject(jsonString)
        val methodResponses = root.optJSONArray("methodResponses") ?: return emptyList()
        val result = mutableListOf<JmapMailboxDto>()
        for (i in 0 until methodResponses.length()) {
            val tuple = methodResponses.optJSONArray(i) ?: continue
            if (tuple.optString(0) == "Mailbox/get") {
                val data = tuple.optJSONObject(1) ?: continue
                val list = data.optJSONArray("list") ?: continue
                for (j in 0 until list.length()) {
                    val mbox = list.optJSONObject(j) ?: continue
                    result.add(
                        JmapMailboxDto(
                            id = mbox.optString("id"),
                            name = mbox.optString("name", "Složka"),
                            role = mbox.optString("role", "").ifBlank { null },
                            unreadEmails = mbox.optInt("unreadEmails", 0),
                            totalEmails = mbox.optInt("totalEmails", 0)
                        )
                    )
                }
            }
        }
        return result
    }

    fun buildEmailQueryAndGetRequestJson(accountId: String, limit: Int): String {
        val root = JSONObject()
        root.put(
            "using",
            JSONArray()
                .put("urn:ietf:params:jmap:core")
                .put("urn:ietf:params:jmap:mail")
        )
        val queryArgs = JSONObject()
            .put("accountId", accountId)
            .put("limit", limit)
            .put(
                "sort",
                JSONArray().put(JSONObject().put("property", "receivedAt").put("isAscending", false))
            )
        val queryCall = JSONArray().put("Email/query").put(queryArgs).put("q0")

        val getArgs = JSONObject()
            .put("accountId", accountId)
            .put(
                "#ids",
                JSONObject()
                    .put("resultOf", "q0")
                    .put("name", "Email/query")
                    .put("path", "/ids")
            )
            .put(
                "properties",
                JSONArray()
                    .put("id")
                    .put("threadId")
                    .put("mailboxIds")
                    .put("subject")
                    .put("from")
                    .put("to")
                    .put("preview")
                    .put("receivedAt")
                    .put("keywords")
                    .put("hasAttachment")
                    .put("bodyValues")
                    .put("textBody")
            )
            .put("fetchTextBodyValues", true)
        val getCall = JSONArray().put("Email/get").put(getArgs).put("g0")

        root.put("methodCalls", JSONArray().put(queryCall).put(getCall))
        return root.toString()
    }

    fun parseEmailGetResponseJson(jsonString: String): List<JmapEmailDto> {
        val root = JSONObject(jsonString)
        val methodResponses = root.optJSONArray("methodResponses") ?: return emptyList()
        val emails = mutableListOf<JmapEmailDto>()

        for (i in 0 until methodResponses.length()) {
            val tuple = methodResponses.optJSONArray(i) ?: continue
            if (tuple.optString(0) == "Email/get") {
                val data = tuple.optJSONObject(1) ?: continue
                val list = data.optJSONArray("list") ?: continue
                for (j in 0 until list.length()) {
                    val item = list.optJSONObject(j) ?: continue
                    val id = item.optString("id")
                    val threadId = item.optString("threadId", id)
                    val subject = item.optString("subject", "(Bez předmětu)")
                    val preview = item.optString("preview", "")

                    val fromArr = item.optJSONArray("from")
                    val firstFrom = fromArr?.optJSONObject(0)
                    val fromEmail = firstFrom?.optString("email").orEmpty().ifBlank { "neznamy@server.local" }
                    val fromName = firstFrom?.optString("name").orEmpty().ifBlank { fromEmail.substringBefore('@') }

                    val toArr = item.optJSONArray("to")
                    val toList = mutableListOf<String>()
                    if (toArr != null) {
                        for (k in 0 until toArr.length()) {
                            val t = toArr.optJSONObject(k)
                            val addr = t?.optString("email").orEmpty()
                            if (addr.isNotBlank()) toList.add(addr)
                        }
                    }

                    val mailboxIdsObj = item.optJSONObject("mailboxIds")
                    val mailboxIds = mailboxIdsObj?.keys()?.asSequence()?.toSet() ?: emptySet()

                    val keywordsObj = item.optJSONObject("keywords")
                    val isSeen = keywordsObj?.optBoolean("\$seen", false) ?: false
                    val isFlagged = keywordsObj?.optBoolean("\$flagged", false) ?: false
                    val hasAttachment = item.optBoolean("hasAttachment", false)

                    val receivedAtIso = item.optString("receivedAt", "")
                    val receivedAtMillis = runCatching {
                        Instant.parse(receivedAtIso).toEpochMilli()
                    }.getOrDefault(System.currentTimeMillis())

                    var bodyPlain = preview
                    val bodyValues = item.optJSONObject("bodyValues")
                    if (bodyValues != null) {
                        val firstPartKey = bodyValues.keys().asSequence().firstOrNull()
                        if (firstPartKey != null) {
                            bodyPlain = bodyValues.optJSONObject(firstPartKey)?.optString("value", preview) ?: preview
                        }
                    }

                    emails.add(
                        JmapEmailDto(
                            id = id,
                            threadId = threadId,
                            mailboxIds = mailboxIds,
                            subject = subject,
                            fromName = fromName,
                            fromEmail = fromEmail,
                            toFormatted = toList.joinToString(", "),
                            preview = preview,
                            bodyPlain = bodyPlain,
                            receivedAtEpochMillis = receivedAtMillis,
                            isSeen = isSeen,
                            isFlagged = isFlagged,
                            hasAttachment = hasAttachment
                        )
                    )
                }
            }
        }
        return emails
    }

    fun buildEmailSendRequestJson(
        accountId: String,
        fromName: String,
        fromEmail: String,
        toEmails: List<String>,
        subject: String,
        bodyPlain: String
    ): String {
        val draftObject = JSONObject()
            .put("from", JSONArray().put(JSONObject().put("name", fromName).put("email", fromEmail)))
            .put(
                "to",
                JSONArray().apply {
                    toEmails.forEach { email ->
                        put(JSONObject().put("email", email.trim()))
                    }
                }
            )
            .put("subject", subject)
            .put(
                "bodyValues",
                JSONObject().put("body1", JSONObject().put("value", bodyPlain))
            )
            .put(
                "textBody",
                JSONArray().put(JSONObject().put("partId", "body1").put("type", "text/plain"))
            )

        val setCall = JSONArray()
            .put("Email/set")
            .put(
                JSONObject()
                    .put("accountId", accountId)
                    .put("create", JSONObject().put("kuryrDraft", draftObject))
            )
            .put("s0")

        val submitCall = JSONArray()
            .put("EmailSubmission/set")
            .put(
                JSONObject()
                    .put("accountId", accountId)
                    .put(
                        "create",
                        JSONObject().put(
                            "sub0",
                            JSONObject().put("emailId", "#kuryrDraft")
                        )
                    )
            )
            .put("s1")

        return JSONObject()
            .put(
                "using",
                JSONArray()
                    .put("urn:ietf:params:jmap:core")
                    .put("urn:ietf:params:jmap:mail")
                    .put("urn:ietf:params:jmap:submission")
            )
            .put("methodCalls", JSONArray().put(setCall).put(submitCall))
            .toString()
    }

    fun parseEmailSendResponseJson(jsonString: String): String {
        val root = JSONObject(jsonString)
        val responses = root.optJSONArray("methodResponses") ?: throw IllegalStateException("Chybí methodResponses")
        for (i in 0 until responses.length()) {
            val tuple = responses.optJSONArray(i) ?: continue
            if (tuple.optString(0) == "Email/set") {
                val created = tuple.optJSONObject(1)?.optJSONObject("created")
                val draftInfo = created?.optJSONObject("kuryrDraft")
                if (draftInfo != null) {
                    return draftInfo.optString("id", "jmap-sent-ok")
                }
            }
        }
        throw IllegalStateException("JMAP server nepotvrdil vytvoření zprávy.")
    }

    fun mapRoleToFolderType(role: String?): FolderType {
        return when (role?.lowercase()) {
            "inbox" -> FolderType.INBOX
            "sent" -> FolderType.SENT
            "drafts" -> FolderType.DRAFTS
            "archive" -> FolderType.ARCHIVE
            "trash" -> FolderType.TRASH
            "junk", "spam" -> FolderType.SPAM
            else -> FolderType.CUSTOM
        }
    }
}
