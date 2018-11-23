package com.criptext.mail.utils.generaldatasource.workers

import android.content.ContentResolver
import android.provider.ContactsContract
import com.criptext.mail.R
import com.criptext.mail.bgworker.BackgroundWorker
import com.criptext.mail.bgworker.ProgressReporter
import com.criptext.mail.db.dao.ContactDao
import com.criptext.mail.db.models.Contact
import com.criptext.mail.utils.UIMessage
import com.criptext.mail.utils.generaldatasource.data.GeneralResult
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import java.util.*


class SyncPhonebookWorker(private val contactDao: ContactDao,
                          private val contentResolver: ContentResolver,
                          override val publishFn: (GeneralResult.SyncPhonebook) -> Unit
                          ) : BackgroundWorker<GeneralResult.SyncPhonebook> {

    override val canBeParallelized = true

    override fun catchException(ex: Exception): GeneralResult.SyncPhonebook {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun work(reporter: ProgressReporter<GeneralResult.SyncPhonebook>)
            : GeneralResult.SyncPhonebook? {
        val operation = Result.of { getNameEmailDetails() }
                .flatMap { Result.of {
                    val contacts = contactDao.getAll().map { dbContact -> dbContact.email }
                    val phonebookContacts = it.filter { contact -> contact.email !in contacts }
                    if(phonebookContacts.isEmpty())
                        throw java.lang.Exception()
                    contactDao.insertAll(phonebookContacts)
                } }
        return when (operation){
            is Result.Success -> {
                GeneralResult.SyncPhonebook.Success()
            }
            is Result.Failure -> {
                GeneralResult.SyncPhonebook.Failure(UIMessage(R.string.error_getting_email))
            }
        }
    }

    fun getNameEmailDetails(): ArrayList<Contact> {
        val emlRecs = ArrayList<Contact>()
        val emlRecsHS = HashSet<String>()
        val projection = arrayOf(ContactsContract.RawContacts._ID, ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.Contacts.PHOTO_ID, ContactsContract.CommonDataKinds.Email.DATA, ContactsContract.CommonDataKinds.Photo.CONTACT_ID)
        val order = ("CASE WHEN "
                + ContactsContract.Contacts.DISPLAY_NAME
                + " NOT LIKE '%@%' THEN 1 ELSE 2 END, "
                + ContactsContract.Contacts.DISPLAY_NAME
                + ", "
                + ContactsContract.CommonDataKinds.Email.DATA
                + " COLLATE NOCASE")
        val filter = ContactsContract.CommonDataKinds.Email.DATA + " NOT LIKE ''"
        val cur = contentResolver.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, projection, filter, null, order)
        if (cur!!.moveToFirst()) {
            do {
                // names comes in hand sometimes
                val name = cur.getString(1)
                val emlAddr = cur.getString(3)

                // keep unique only
                if (emlRecsHS.add(emlAddr.toLowerCase())) {
                    emlRecs.add(Contact(0, emlAddr, name))
                }
            } while (cur.moveToNext())
        }

        cur.close()
        return emlRecs
    }

    override fun cancel() {
        TODO("not implemented")
    }
}