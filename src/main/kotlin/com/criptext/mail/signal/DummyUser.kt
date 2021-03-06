package com.criptext.mail.signal

import com.criptext.mail.db.models.signal.CRPreKey
import org.whispersystems.libsignal.state.SignalProtocolStore
import java.io.File

/**
 * Created by gabriel on 3/17/18.
 */

abstract class DummyUser(generator: SignalKeyGenerator, recipientId: String, deviceId: Int) {

    val registrationBundles = generator.register(recipientId, deviceId)
    abstract val store: SignalProtocolStore
    private lateinit var client: SignalClient

    fun setup(): DummyUser {
        client = SignalClient.Default(store)
        return this
    }

    fun fetchAPreKeyBundle(accountId: Long): PreKeyBundleShareData.DownloadBundle {
        val bundle = registrationBundles.uploadBundle
        val preKeyPublic = bundle.preKeys[1]!!
        val preKeyRecord = CRPreKey(1, 1, preKeyPublic, accountId)

        return PreKeyBundleShareData.DownloadBundle(
                shareData = registrationBundles.uploadBundle.shareData,
                preKey = preKeyRecord)
    }

    fun buildSession(downloadBundle: PreKeyBundleShareData.DownloadBundle) {
        client.createSessionsFromBundles(listOf(downloadBundle))
    }

    fun encrypt(recipientId: String, deviceId: Int, text: String) =
        client.encryptMessage(recipientId, deviceId, text)

    fun decrypt(recipientId: String, deviceId: Int, encryptedData: SignalEncryptedData) =
            client.decryptMessage(recipientId, deviceId, encryptedData)

    fun decryptFileByChunks(fileToDecrypt: File, recipientId: String, deviceId: Int) =
            client.decryptFileByChunks(fileToDecrypt, recipientId, deviceId)

}