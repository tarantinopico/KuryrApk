package com.example.data.repository

import android.content.ContentResolver
import android.provider.ContactsContract
import com.example.data.local.ContactEntity
import com.example.data.local.ContactGroupEntity
import com.example.data.local.KuryrDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactRepository @Inject constructor(
    private val dao: KuryrDao
) {
    val contactsFlow: Flow<List<ContactEntity>> = dao.observeContacts()
    val contactGroupsFlow: Flow<List<ContactGroupEntity>> = dao.observeContactGroups()

    fun searchContacts(query: String): Flow<List<ContactEntity>> {
        return if (query.isBlank()) contactsFlow else dao.searchContacts(query.trim())
    }

    suspend fun saveContact(contact: ContactEntity): Long {
        return dao.insertContact(contact)
    }

    suspend fun updateContact(contact: ContactEntity) {
        dao.updateContact(contact)
    }

    suspend fun toggleVip(contact: ContactEntity) {
        dao.updateContact(contact.copy(isVip = !contact.isVip))
    }

    suspend fun deleteContact(contactId: Long) {
        dao.deleteContactById(contactId)
    }

    suspend fun saveContactGroup(group: ContactGroupEntity): Long {
        return dao.insertContactGroup(group)
    }

    suspend fun deleteContactGroup(group: ContactGroupEntity) {
        dao.deleteContactGroup(group)
    }

    /**
     * Načte a naimportuje e-mailové kontakty ze systémového Android ContactsProvideru.
     * Vyžaduje udělené runtime oprávnění android.permission.READ_CONTACTS.
     */
    suspend fun importFromSystemContacts(contentResolver: ContentResolver): Int = withContext(Dispatchers.IO) {
        val existingEmails = dao.getAllContactsOnce().map { it.email.lowercase() }.toSet()
        val importedList = mutableListOf<ContactEntity>()
        val seenInBatch = mutableSetOf<String>()

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Email.DISPLAY_NAME_PRIMARY,
            ContactsContract.CommonDataKinds.Email.ADDRESS,
            ContactsContract.CommonDataKinds.Email.CONTACT_ID
        )

        contentResolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            projection,
            null,
            null,
            "${ContactsContract.CommonDataKinds.Email.DISPLAY_NAME_PRIMARY} ASC"
        )?.use { cursor ->
            val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.DISPLAY_NAME_PRIMARY)
            val emailIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)

            val palette = listOf(0xFF1E40AF, 0xFF0F766E, 0xFF4338CA, 0xFFB45309, 0xFFBE123C, 0xFF334155)
            var colorCounter = 0

            while (cursor.moveToNext()) {
                val rawName = if (nameIdx >= 0) cursor.getString(nameIdx).orEmpty().trim() else ""
                val rawEmail = if (emailIdx >= 0) cursor.getString(emailIdx).orEmpty().trim() else ""
                val normalizedEmail = rawEmail.lowercase()

                if (rawEmail.contains("@") && normalizedEmail !in existingEmails && normalizedEmail !in seenInBatch) {
                    seenInBatch.add(normalizedEmail)
                    val displayName = rawName.ifBlank { rawEmail.substringBefore('@') }
                    importedList.add(
                        ContactEntity(
                            displayName = displayName,
                            email = rawEmail,
                            isImportedFromSystem = true,
                            avatarColorHex = palette[colorCounter % palette.size]
                        )
                    )
                    colorCounter++
                }
            }
        }

        if (importedList.isNotEmpty()) {
            dao.insertContacts(importedList)
        }
        importedList.size
    }
}
