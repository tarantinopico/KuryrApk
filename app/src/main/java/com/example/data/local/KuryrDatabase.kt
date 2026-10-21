package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.data.model.FolderType
import com.example.data.model.MailProtocol
import com.example.data.model.OutboxStatus
import com.example.data.model.SecurityMode

class KuryrTypeConverters {
    @TypeConverter
    fun fromProtocol(value: MailProtocol): String = value.name

    @TypeConverter
    fun toProtocol(value: String): MailProtocol = MailProtocol.valueOf(value)

    @TypeConverter
    fun fromSecurity(value: SecurityMode): String = value.name

    @TypeConverter
    fun toSecurity(value: String): SecurityMode = SecurityMode.valueOf(value)

    @TypeConverter
    fun fromFolderType(value: FolderType): String = value.name

    @TypeConverter
    fun toFolderType(value: String): FolderType = FolderType.valueOf(value)

    @TypeConverter
    fun fromOutboxStatus(value: OutboxStatus): String = value.name

    @TypeConverter
    fun toOutboxStatus(value: String): OutboxStatus = OutboxStatus.valueOf(value)
}

@Database(
    entities = [
        AccountGroupEntity::class,
        AccountEntity::class,
        FolderEntity::class,
        EmailMessageEntity::class,
        AttachmentEntity::class,
        ContactGroupEntity::class,
        ContactEntity::class,
        NotificationRuleEntity::class,
        DigestQueueEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(KuryrTypeConverters::class)
abstract class KuryrDatabase : RoomDatabase() {
    abstract fun kuryrDao(): KuryrDao

    companion object {
        @Volatile
        private var INSTANCE: KuryrDatabase? = null

        fun getInstance(context: Context): KuryrDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    KuryrDatabase::class.java,
                    "kuryr_mail.db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build().also { INSTANCE = it }
            }
        }
    }
}
