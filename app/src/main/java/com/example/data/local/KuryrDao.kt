package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.FolderType
import com.example.data.model.OutboxStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface KuryrDao {
    // --- Account Groups ---
    @Query("SELECT * FROM account_groups ORDER BY name ASC")
    fun observeAccountGroups(): Flow<List<AccountGroupEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccountGroup(group: AccountGroupEntity): Long

    @Delete
    suspend fun deleteAccountGroup(group: AccountGroupEntity)

    // --- Accounts ---
    @Query("SELECT * FROM accounts ORDER BY isDefault DESC, accountName ASC")
    fun observeAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY isDefault DESC, accountName ASC")
    suspend fun getAllAccountsOnce(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE id = :accountId LIMIT 1")
    suspend fun getAccountById(accountId: Long): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Query("DELETE FROM accounts WHERE id = :accountId")
    suspend fun deleteAccountById(accountId: Long)

    // --- Folders ---
    @Query("SELECT * FROM folders ORDER BY accountId ASC, id ASC")
    fun observeAllFolders(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE accountId = :accountId ORDER BY id ASC")
    fun observeFoldersByAccount(accountId: Long): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE accountId = :accountId ORDER BY id ASC")
    suspend fun getFoldersByAccountOnce(accountId: Long): List<FolderEntity>

    @Query("SELECT * FROM folders WHERE accountId = :accountId AND type = :type LIMIT 1")
    suspend fun getFolderByType(accountId: Long, type: FolderType): FolderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolders(folders: List<FolderEntity>): List<Long>

    @Update
    suspend fun updateFolder(folder: FolderEntity)

    // --- Messages ---
    @Query("SELECT * FROM messages ORDER BY receivedAt DESC")
    fun observeAllMessages(): Flow<List<EmailMessageEntity>>

    @Query("SELECT * FROM messages WHERE folderId = :folderId ORDER BY receivedAt DESC")
    fun observeMessagesByFolder(folderId: Long): Flow<List<EmailMessageEntity>>

    @Query("SELECT * FROM messages WHERE accountId = :accountId ORDER BY receivedAt DESC")
    fun observeMessagesByAccount(accountId: Long): Flow<List<EmailMessageEntity>>

    @Query("SELECT * FROM messages WHERE id = :messageId LIMIT 1")
    fun observeMessageById(messageId: Long): Flow<EmailMessageEntity?>

    @Query("SELECT * FROM messages WHERE id = :messageId LIMIT 1")
    suspend fun getMessageById(messageId: Long): EmailMessageEntity?

    @Query("SELECT * FROM messages WHERE outboxStatus = :status ORDER BY receivedAt ASC")
    suspend fun getMessagesByOutboxStatus(status: OutboxStatus): List<EmailMessageEntity>

    @Query(
        """
        SELECT * FROM messages 
        WHERE subject LIKE '%' || :query || '%' 
           OR fromName LIKE '%' || :query || '%' 
           OR fromEmail LIKE '%' || :query || '%' 
           OR bodyPlain LIKE '%' || :query || '%' 
           OR toRecipients LIKE '%' || :query || '%'
        ORDER BY receivedAt DESC
        """
    )
    fun searchMessages(query: String): Flow<List<EmailMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: EmailMessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<EmailMessageEntity>)

    @Update
    suspend fun updateMessage(message: EmailMessageEntity)

    @Query("UPDATE messages SET isRead = :isRead WHERE id = :messageId")
    suspend fun setMessageRead(messageId: Long, isRead: Boolean)

    @Query("UPDATE messages SET isStarred = :isStarred WHERE id = :messageId")
    suspend fun setMessageStarred(messageId: Long, isStarred: Boolean)

    @Query("UPDATE messages SET folderId = :targetFolderId WHERE id = :messageId")
    suspend fun moveMessageToFolder(messageId: Long, targetFolderId: Long)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessageById(messageId: Long)

    // --- Attachments ---
    @Query("SELECT * FROM attachments WHERE messageId = :messageId")
    fun observeAttachmentsForMessage(messageId: Long): Flow<List<AttachmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachments(attachments: List<AttachmentEntity>)

    // --- Contact Groups ---
    @Query("SELECT * FROM contact_groups ORDER BY name ASC")
    fun observeContactGroups(): Flow<List<ContactGroupEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContactGroup(group: ContactGroupEntity): Long

    @Delete
    suspend fun deleteContactGroup(group: ContactGroupEntity)

    // --- Contacts ---
    @Query("SELECT * FROM contacts ORDER BY isVip DESC, displayName ASC")
    fun observeContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts ORDER BY isVip DESC, displayName ASC")
    suspend fun getAllContactsOnce(): List<ContactEntity>

    @Query(
        """
        SELECT * FROM contacts 
        WHERE displayName LIKE '%' || :query || '%' 
           OR email LIKE '%' || :query || '%' 
           OR organization LIKE '%' || :query || '%' 
           OR notes LIKE '%' || :query || '%'
        ORDER BY isVip DESC, displayName ASC
        """
    )
    fun searchContacts(query: String): Flow<List<ContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<ContactEntity>)

    @Update
    suspend fun updateContact(contact: ContactEntity)

    @Query("DELETE FROM contacts WHERE id = :contactId")
    suspend fun deleteContactById(contactId: Long)

    // --- Notification Rules ---
    @Query("SELECT * FROM notification_rules ORDER BY id ASC")
    fun observeNotificationRules(): Flow<List<NotificationRuleEntity>>

    @Query("SELECT * FROM notification_rules WHERE isEnabled = 1 ORDER BY id ASC")
    suspend fun getEnabledNotificationRules(): List<NotificationRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotificationRule(rule: NotificationRuleEntity): Long

    @Update
    suspend fun updateNotificationRule(rule: NotificationRuleEntity)

    @Query("DELETE FROM notification_rules WHERE id = :ruleId")
    suspend fun deleteNotificationRule(ruleId: Long)

    // --- Digest Queue ---
    @Query("SELECT * FROM digest_queue WHERE isProcessed = 0 ORDER BY receivedAt DESC")
    fun observePendingDigestItems(): Flow<List<DigestQueueEntity>>

    @Query("SELECT * FROM digest_queue WHERE isProcessed = 0 ORDER BY receivedAt DESC")
    suspend fun getPendingDigestItemsOnce(): List<DigestQueueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDigestItem(item: DigestQueueEntity): Long

    @Query("UPDATE digest_queue SET isProcessed = 1 WHERE isProcessed = 0")
    suspend fun markAllDigestItemsProcessed()
}
