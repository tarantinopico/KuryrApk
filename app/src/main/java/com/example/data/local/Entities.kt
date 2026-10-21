package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.FolderType
import com.example.data.model.MailProtocol
import com.example.data.model.OutboxStatus
import com.example.data.model.SecurityMode

@Entity(tableName = "account_groups")
data class AccountGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: Long = 0xFF1E40AF,
    val iconName: String = "work"
)

@Entity(
    tableName = "accounts",
    indices = [Index(value = ["emailAddress"], unique = true)]
)
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountName: String,
    val displayName: String,
    val emailAddress: String,
    val username: String,
    val providerId: String,
    val protocol: MailProtocol,
    val incomingHost: String,
    val incomingPort: Int,
    val incomingSecurity: SecurityMode,
    val smtpHost: String,
    val smtpPort: Int,
    val smtpSecurity: SecurityMode,
    val jmapEndpoint: String = "",
    val signature: String = "Odesláno z aplikace Kurýr",
    val syncIntervalMinutes: Int = 15,
    val colorHex: Long = 0xFF1E40AF,
    val groupId: Long? = null,
    val isDefault: Boolean = false,
    val lastSyncTimestamp: Long? = null,
    val lastSyncStatus: String = "Nesynchronizováno"
)

@Entity(
    tableName = "folders",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("accountId")]
)
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val name: String,
    val serverPath: String,
    val type: FolderType,
    val unreadCount: Int = 0,
    val totalCount: Int = 0
)

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("accountId"), Index("folderId"), Index("receivedAt")]
)
data class EmailMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val folderId: Long,
    val remoteUid: String,
    val messageIdHeader: String,
    val threadId: String,
    val subject: String,
    val fromName: String,
    val fromEmail: String,
    val toRecipients: String,
    val ccRecipients: String = "",
    val bccRecipients: String = "",
    val replyTo: String = "",
    val snippet: String,
    val bodyPlain: String,
    val bodyHtml: String = "",
    val rawHeaders: String = "",
    val spfStatus: String = "PASS",
    val dkimStatus: String = "PASS",
    val receivedAt: Long,
    val isRead: Boolean = false,
    val isStarred: Boolean = false,
    val isVipSender: Boolean = false,
    val hasAttachments: Boolean = false,
    val attachmentNames: String = "",
    val outboxStatus: OutboxStatus = OutboxStatus.SENT
)

@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(
            entity = EmailMessageEntity::class,
            parentColumns = ["id"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("messageId")]
)
data class AttachmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val messageId: Long,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val localUri: String = ""
)

@Entity(tableName = "contact_groups")
data class ContactGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: Long = 0xFF1E40AF,
    val description: String = ""
)

@Entity(
    tableName = "contacts",
    indices = [Index(value = ["email"], unique = false)]
)
data class ContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val displayName: String,
    val email: String,
    val secondaryEmail: String = "",
    val phone: String = "",
    val organization: String = "",
    val notes: String = "",
    val groupId: Long? = null,
    val isVip: Boolean = false,
    val isImportedFromSystem: Boolean = false,
    val avatarColorHex: Long = 0xFF1E40AF
)

@Entity(tableName = "notification_rules")
data class NotificationRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isEnabled: Boolean = true,
    val vipOnly: Boolean = false,
    val senderContains: String = "",
    val subjectContains: String = "",
    val requiresAttachment: Boolean = false,
    val targetAccountId: Long? = null,
    val bypassQuietHours: Boolean = false,
    val highPriority: Boolean = true,
    val customSoundLabel: String = "Výchozí tón Kurýr"
)

@Entity(tableName = "digest_queue")
data class DigestQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val messageId: Long,
    val accountId: Long,
    val sender: String,
    val subject: String,
    val receivedAt: Long,
    val isProcessed: Boolean = false
)
