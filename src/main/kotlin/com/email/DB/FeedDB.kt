package com.email.DB

import android.content.Context
import com.email.DB.models.FeedItem
import com.email.DB.seeders.FeedSeeder

/**
 * Created by danieltigse on 2/7/18.
 */

interface FeedLocalDB {

    fun getFeedItems(): List<FeedItem>
    fun seed()
    fun deleteFeedItem(id: Int)
    fun muteFeedItem(id: Int, isMuted: Boolean)

    class Default(applicationContext: Context): FeedLocalDB {

        private val db = AppDatabase.getAppDatabase(applicationContext)

        override fun getFeedItems(): List<FeedItem> {
            return db.feedDao().getAll()
        }

        override fun seed() {
            try {
                FeedSeeder.seed(db.feedDao())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun deleteFeedItem(id: Int) {
            db.feedDao().delete(id)
        }

        override fun muteFeedItem(id: Int, isMuted: Boolean){
            db.feedDao().toggleMute(id, isMuted)
        }

    }

}