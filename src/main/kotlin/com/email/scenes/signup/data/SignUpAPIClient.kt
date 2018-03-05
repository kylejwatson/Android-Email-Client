package com.email.scenes.signup.data

import com.email.api.ApiCall
import com.email.api.PreKeyBundleShareData
import com.email.api.ServerErrorException
import com.email.db.models.User
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Created by sebas on 2/26/18.
 */

interface SignUpAPIClient {

    fun createUser(
            user: User,
            password: String,
            recoveryEmail: String?,
            keybundle : PreKeyBundleShareData.UploadBundle)
            : String

    class Default : SignUpAPIClient {
        private val client = OkHttpClient().
                newBuilder().
                connectTimeout(5, TimeUnit.SECONDS).
                readTimeout(5, TimeUnit.SECONDS).
                build()

        override fun createUser(
                user: User,
                password: String,
                recoveryEmail: String?,
                keybundle : PreKeyBundleShareData.UploadBundle
        ): String {
            val request = ApiCall.createUser(
                    recipientId = user.nickname,
                    name = user.name,
                    password = password,
                    recoveryEmail = recoveryEmail,
                    keyBundle = keybundle
            )
            val response = client.newCall(request).execute()
            if(!response.isSuccessful) throw(ServerErrorException(response.code()))
            return response.message()
        }
    }
}
