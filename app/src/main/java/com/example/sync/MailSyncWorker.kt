package com.example.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.local.KuryrDatabase
import com.example.data.preferences.AppPreferencesRepository
import com.example.data.repository.MailRepository
import com.example.data.security.AndroidKeystoreCredentialVault
import com.example.network.ImapSmtpPop3Client
import com.example.network.JmapClient
import com.example.notifications.NotificationRuleEngine
import com.example.widget.KuryrWidgetUpdater
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * WorkManager úloha pro synchronizaci všech nakonfigurovaných e-mailových účtů na pozadí,
 * vyhodnocení notifikačních pravidel / souhrnů a aktualizaci domovských Glance widgetů.
 */
class MailSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val database = KuryrDatabase.getInstance(applicationContext)
        val dao = database.kuryrDao()
        val vault = AndroidKeystoreCredentialVault(applicationContext)
        val repository = MailRepository(
            dao = dao,
            passwordVault = vault,
            imapSmtpPop3Client = ImapSmtpPop3Client(),
            jmapClient = JmapClient()
        )
        val prefsRepo = AppPreferencesRepository(applicationContext)
        val notificationEngine = NotificationRuleEngine(dao)

        val preferences = prefsRepo.preferencesFlow.first()
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val accounts = dao.getAllAccountsOnce()

        for (account in accounts) {
            val syncResult = repository.synchronizeAccount(account.id)
            syncResult.onSuccess { syncedMessages ->
                syncedMessages.filter { !it.isRead }.take(3).forEach { message ->
                    notificationEngine.processIncomingMessage(
                        context = applicationContext,
                        message = message,
                        preferences = preferences,
                        currentHourOfDay = currentHour
                    )
                }
            }
        }

        // Pokud již skončily tiché hodiny a čekají položky v souhrnu, odešleme souhrnnou notifikaci
        val inQuiet = notificationEngine.isWithinQuietHours(
            enabled = preferences.quietHoursEnabled,
            startHour = preferences.quietHoursStartHour,
            endHour = preferences.quietHoursEndHour,
            currentHour = currentHour
        )
        if (!inQuiet) {
            notificationEngine.dispatchPendingDigestNotification(
                context = applicationContext,
                frequency = preferences.digestFrequency
            )
        }

        KuryrWidgetUpdater.updateAllWidgets(applicationContext)
        return Result.success()
    }

    companion object {
        private const val PERIODIC_WORK_NAME = "kuryr_periodic_mail_sync"
        private const val IMMEDIATE_WORK_NAME = "kuryr_immediate_mail_sync"

        fun schedulePeriodicSync(context: Context, intervalMinutes: Int) {
            runCatching {
                val safeInterval = intervalMinutes.coerceAtLeast(15).toLong()
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val request = PeriodicWorkRequestBuilder<MailSyncWorker>(safeInterval, TimeUnit.MINUTES)
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    PERIODIC_WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    request
                )
            }
        }

        fun enqueueImmediateSync(context: Context) {
            runCatching {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val request = OneTimeWorkRequestBuilder<MailSyncWorker>()
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context).enqueueUniqueWork(
                    IMMEDIATE_WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    request
                )
            }
        }
    }
}
