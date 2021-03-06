package com.criptext.mail.db.seeders

import com.criptext.mail.db.dao.OpenDao
import com.criptext.mail.db.models.Open
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by sebas on 2/7/18.
 */

class OpenSeeder {

    companion object {
        var ops : List<Open> = mutableListOf()
        var sdf : SimpleDateFormat = SimpleDateFormat( "yyyy-MM-dd HH:mm:dd")

        fun seed(openDao: OpenDao){
            ops = openDao.getAll()
            openDao.deleteAll(ops)
            ops = mutableListOf<Open>()
            for (a in 1..1){
                ops += fillOpen(a)
            }
            openDao.insertAll(ops)
        }


        private fun fillOpen(iteration: Int): Open {
            lateinit var op: Open
            when (iteration) {
                1 -> op = Open( id = 1 ,
                        fileId = "XXXXXXXXXX2312XXXXX1",
                        date = sdf.parse("1992-05-23 20:12:58"),
                        type = 1,
                        location = "pc")
            }
            return op
        }
    }

    init {
        sdf.timeZone = TimeZone.getDefault()
    }
}
