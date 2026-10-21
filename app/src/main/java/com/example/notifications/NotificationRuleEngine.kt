package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.DigestQueueEntity
import com.example.data.local.EmailMessageEntity
import com.example.data.local.KuryrDao
import com.example.data.local.NotificationRuleEntity
import com.example.data.model.DigestFrequency
import com.example.data.preferences.UserPreferences
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

enum class NotificationDecisionType {
    NOTIFY_HIGH_PRIORITY,
    NOTIFY_STANDARD,
    QUEUE_FOR_DIGEST,
    SUPPRESSED
}

data class NotificationEvaluationResult(
    val decision: NotificationDecisionType,
    val matchedRule: NotificationRuleEntity?,
    val reason: String
)

/**
 * Engine pro vyhodnocování notifikačních pravidel (VIP, odesílatel, předmět, přílohy),
 * tichých hodin a denních/týdenních souhrnů (digests) + správa Android Notification Channels.
 */
@Singleton
class NotificationRuleEngine @Inject constructor(
    private val dao: KuryrDao
) {

    /**
     * Čistá doménová funkce pro vyhodnocení, zda má příchozí zpráva vyvolat okamžitou notifikaci,
     * prioritní VIP upozornění nebo se zařadit do fronty souhrnu (digestu) během tichých hodin.
     */
    fun evaluateMessage(
        message: EmailMessageEntity,
        enabledRules: List<NotificationRuleEntity>,
        preferences: UserPreferences,
        currentHourOfDay: Int
    ): NotificationEvaluationResult {
        if (message.isRead) {
            return NotificationEvaluationResult(
                decision = NotificationDecisionType.SUPPRESSED,
                matchedRule = null,
                reason = "Zpráva je již označena jako přečtená"
            )
        }

        val matchingRule = enabledRules.firstOrNull { rule ->
            matchesRule(message, rule)
        }

        val inQuietHours = isWithinQuietHours(
            enabled = preferences.quietHoursEnabled,
            startHour = preferences.quietHoursStartHour,
            endHour = preferences.quietHoursEndHour,
            currentHour = currentHourOfDay
        )

        if (inQuietHours) {
            val canBypass = (matchingRule?.bypassQuietHours == true) || message.isVipSender
            if (!canBypass) {
                return if (preferences.digestFrequency != DigestFrequency.OFF) {
                    NotificationEvaluationResult(
                        decision = NotificationDecisionType.QUEUE_FOR_DIGEST,
                        matchedRule = matchingRule,
                        reason = "Aktivní tiché hodiny – zařazeno do souhrnu"
                    )
                } else {
                    NotificationEvaluationResult(
                        decision = NotificationDecisionType.SUPPRESSED,
                        matchedRule = matchingRule,
                        reason = "Potlačeno během tichých hodin"
                    )
                }
            }
        }

        if (matchingRule != null) {
            return NotificationEvaluationResult(
                decision = if (matchingRule.highPriority) {
                    NotificationDecisionType.NOTIFY_HIGH_PRIORITY
                } else {
                    NotificationDecisionType.NOTIFY_STANDARD
                },
                matchedRule = matchingRule,
                reason = "Shoda s pravidlem: ${matchingRule.name}"
            )
        }

        if (message.isVipSender) {
            return NotificationEvaluationResult(
                decision = NotificationDecisionType.NOTIFY_HIGH_PRIORITY,
                matchedRule = null,
                reason = "Odesílatel je v seznamu VIP kontaktů"
            )
        }

        return NotificationEvaluationResult(
            decision = NotificationDecisionType.NOTIFY_STANDARD,
            matchedRule = null,
            reason = "Standardní příchozí zpráva"
        )
    }

    fun matchesRule(message: EmailMessageEntity, rule: NotificationRuleEntity): Boolean {
        if (!rule.isEnabled) return false
        if (rule.targetAccountId != null && rule.targetAccountId != message.accountId) return false
        if (rule.vipOnly && !message.isVipSender) return false
        if (rule.requiresAttachment && !message.hasAttachments) return false

        if (rule.senderContains.isNotBlank()) {
            val needle = rule.senderContains.trim().lowercase(Locale.ROOT)
            val hayFrom = "${message.fromName} ${message.fromEmail}".lowercase(Locale.ROOT)
            if (!hayFrom.contains(needle)) return false
        }

        if (rule.subjectContains.isNotBlank()) {
            val needle = rule.subjectContains.trim().lowercase(Locale.ROOT)
            if (!message.subject.lowercase(Locale.ROOT).contains(needle)) return false
        }

        return true
    }

    fun isWithinQuietHours(
        enabled: Boolean,
        startHour: Int,
        endHour: Int,
        currentHour: Int
    ): Boolean {
        if (!enabled) return false
        val start = startHour.coerceIn(0, 23)
        val end = endHour.coerceIn(0, 23)
        val current = currentHour.coerceIn(0, 23)
        return if (start <= end) {
            current in start until end
        } else {
            // Přes půlnoc (např. 22:00 až 07:00)
            current >= start || current < end
        }
    }

    /**
     * Sestaví čitelný text denního nebo týdenního souhrnu z fronty DigestQueueEntity.
     */
    fun formatDigestSummary(
        items: List<DigestQueueEntity>,
        frequency: DigestFrequency
    ): String {
        if (items.isEmpty()) return "Žádné zmeškané zprávy ve frontě souhrnu."
        val header = when (frequency) {
            DigestFrequency.WEEKLY -> "Týdenní souhrn (${items.size} zpráv)"
            else -> "Denní souhrn (${items.size} zpráv)"
        }
        val lines = items.take(5).joinToString("\n") { item ->
            "• ${item.sender}: ${item.subject}"
        }
        val more = if (items.size > 5) "\n…a dalších ${items.size - 5} zpráv" else ""
        return "$header\n$lines$more"
    }

    /**
     * Zaregistruje Android Notification Channels (VIP, Standardní pošta, Souhrny).
     */
    fun ensureNotificationChannels(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val vipChannel = NotificationChannel(
            CHANNEL_VIP,
            context.getString(R.string.channel_vip_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Okamžitá upozornění od VIP kontaktů a prioritních pravidel"
            enableVibration(true)
        }

        val standardChannel = NotificationChannel(
            CHANNEL_STANDARD,
            context.getString(R.string.channel_standard_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Upozornění na nově doručené e-maily"
        }

        val digestChannel = NotificationChannel(
            CHANNEL_DIGEST,
            context.getString(R.string.channel_digest_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Plánované denní a týdenní souhrny zpráv z tichých hodin"
        }

        manager.createNotificationChannels(listOf(vipChannel, standardChannel, digestChannel))
    }

    suspend fun processIncomingMessage(
        context: Context,
        message: EmailMessageEntity,
        preferences: UserPreferences,
        currentHourOfDay: Int
    ): NotificationEvaluationResult {
        val rules = dao.getEnabledNotificationRules()
        val evaluation = evaluateMessage(message, rules, preferences, currentHourOfDay)

        when (evaluation.decision) {
            NotificationDecisionType.QUEUE_FOR_DIGEST -> {
                dao.insertDigestItem(
                    DigestQueueEntity(
                        messageId = message.id,
                        accountId = message.accountId,
                        sender = message.fromName.ifBlank { message.fromEmail },
                        subject = message.subject,
                        receivedAt = message.receivedAt
                    )
                )
            }
            NotificationDecisionType.NOTIFY_HIGH_PRIORITY -> {
                postSystemNotification(context, message, highPriority = true, ruleName = evaluation.matchedRule?.name)
            }
            NotificationDecisionType.NOTIFY_STANDARD -> {
                postSystemNotification(context, message, highPriority = false, ruleName = evaluation.matchedRule?.name)
            }
            NotificationDecisionType.SUPPRESSED -> Unit
        }
        return evaluation
    }

    suspend fun dispatchPendingDigestNotification(
        context: Context,
        frequency: DigestFrequency
    ): Int {
        val pending = dao.getPendingDigestItemsOnce()
        if (pending.isEmpty()) return 0

        ensureNotificationChannels(context)
        val summaryText = formatDigestSummary(pending, frequency)
        if (hasNotificationPermission(context)) {
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                9900,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_DIGEST)
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .setContentTitle("Kurýr • ${frequency.displayName}")
                .setContentText("${pending.size} zpráv čeká na přečtení")
                .setStyle(NotificationCompat.BigTextStyle().bigText(summaryText))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            NotificationManagerCompat.from(context).notify(9900, notification)
        }
        dao.markAllDigestItemsProcessed()
        return pending.size
    }

    private fun postSystemNotification(
        context: Context,
        message: EmailMessageEntity,
        highPriority: Boolean,
        ruleName: String?
    ) {
        ensureNotificationChannels(context)
        if (!hasNotificationPermission(context)) return

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_message_id", message.id)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            message.id.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = if (highPriority) CHANNEL_VIP else CHANNEL_STANDARD
        val subText = ruleName ?: if (message.isVipSender) "VIP kontakt" else null

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(message.fromName.ifBlank { message.fromEmail })
            .setContentText(message.subject)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(message.subject)
                    .bigText(message.snippet.ifBlank { message.bodyPlain.take(180) })
            )
            .setPriority(if (highPriority) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .addAction(
                android.R.drawable.ic_menu_view,
                "Otevřít",
                contentPendingIntent
            )

        if (subText != null) {
            builder.setSubText(subText)
        }

        NotificationManagerCompat.from(context).notify(message.id.toInt().coerceAtLeast(1001), builder.build())
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    companion object {
        const val CHANNEL_VIP = "kuryr_vip_channel"
        const val CHANNEL_STANDARD = "kuryr_standard_channel"
        const val CHANNEL_DIGEST = "kuryr_digest_channel"
    }
}
