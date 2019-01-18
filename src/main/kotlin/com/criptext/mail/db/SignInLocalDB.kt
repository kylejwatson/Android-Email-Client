package com.criptext.mail.db

import android.content.Context
import com.criptext.mail.utils.EmailUtils
import java.io.File

/**
 * Created by sebas on 2/15/18.
 */


interface SignInLocalDB {
    fun login(): Boolean
    fun accountExistsLocally(username: String): Boolean
    fun deleteDatabase()

    class Default(applicationContext: Context, private val filesDir: File): SignInLocalDB {

        private val db = AppDatabase.getAppDatabase(applicationContext)

        override fun accountExistsLocally(username: String): Boolean {
            val account = db.accountDao().getLoggedInAccount()
            if(account == null) return false
            else if(account.recipientId == username) return true
            return false
        }

        override fun login(): Boolean {
            TODO("LOGIN NOT IMPLEMENTED")
        }

        override fun deleteDatabase() {
            EmailUtils.deleteEmailsInFileSystem(filesDir, db.accountDao().getLoggedInAccount()!!.recipientId)
            db.clearAllTables()
        }

    }

}
