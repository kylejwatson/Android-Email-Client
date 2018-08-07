package com.criptext.mail.scenes.mailbox.data

import android.accounts.NetworkErrorException
import com.criptext.mail.R
import com.criptext.mail.aes.AESUtil
import com.criptext.mail.api.HttpClient
import com.criptext.mail.api.HttpErrorHandlingHelper
import com.criptext.mail.api.ServerErrorException
import com.criptext.mail.bgworker.BackgroundWorker
import com.criptext.mail.bgworker.ProgressReporter
import com.criptext.mail.db.DeliveryTypes
import com.criptext.mail.db.MailboxLocalDB
import com.criptext.mail.db.dao.signal.RawIdentityKeyDao
import com.criptext.mail.db.dao.signal.RawSessionDao
import com.criptext.mail.db.models.ActiveAccount
import com.criptext.mail.db.models.Contact
import com.criptext.mail.db.models.KnownAddress
import com.criptext.mail.scenes.composer.data.*
import com.criptext.mail.signal.*
import com.criptext.mail.utils.DateUtils
import com.criptext.mail.utils.DeviceUtils
import com.criptext.mail.utils.EmailAddressUtils.extractRecipientIdFromCriptextAddress
import com.criptext.mail.utils.EmailAddressUtils.isFromCriptextDomain
import com.criptext.mail.utils.Encoding
import com.criptext.mail.utils.UIMessage
import com.criptext.mail.utils.file.FileUtils
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.mapError
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.whispersystems.libsignal.SignalProtocolAddress


/**
 * Created by gabriel on 2/26/18.
 */

class SendMailWorker(private val signalClient: SignalClient,
                     private val rawSessionDao: RawSessionDao,
                     private val rawIdentityKeyDao: RawIdentityKeyDao,
                     private val db: MailboxLocalDB,
                     httpClient: HttpClient,
                     private val activeAccount: ActiveAccount,
                     private val emailId: Long,
                     private val threadId: String?,
                     private val composerInputData: ComposerInputData,
                     private val attachments: List<ComposerAttachment>,
                     private val fileKey: String?,
                     override val publishFn: (MailboxResult.SendMail) -> Unit)
    : BackgroundWorker<MailboxResult.SendMail> {
    override val canBeParallelized = false

    private val apiClient = ComposerAPIClient(httpClient, activeAccount.jwt)

    private var guestEmails: PostEmailBody.GuestEmail? = null

    private fun getMailRecipients(): MailRecipients {
        val toAddresses = composerInputData.to.map(Contact.toAddress)
        val ccAddresses = composerInputData.cc.map(Contact.toAddress)
        val bccAddresses = composerInputData.bcc.map(Contact.toAddress)

        val toCriptext = toAddresses.filter(isFromCriptextDomain)
                                    .map(extractRecipientIdFromCriptextAddress)
        val ccCriptext = ccAddresses.filter(isFromCriptextDomain)
                                    .map(extractRecipientIdFromCriptextAddress)
        val bccCriptext = bccAddresses.filter(isFromCriptextDomain)
                                      .map(extractRecipientIdFromCriptextAddress)

        return MailRecipients(toCriptext = toCriptext, ccCriptext = ccCriptext,
                bccCriptext = bccCriptext, peerCriptext = listOf(activeAccount.recipientId))
    }

    private fun getMailRecipientsNonCriptext(): MailRecipients {
        val toAddresses = composerInputData.to.map(Contact.toAddress)
        val ccAddresses = composerInputData.cc.map(Contact.toAddress)
        val bccAddresses = composerInputData.bcc.map(Contact.toAddress)

        val toNonCriptext = toAddresses.filterNot(isFromCriptextDomain)
        val ccNonCriptext = ccAddresses.filterNot(isFromCriptextDomain)
        val bccNonCriptext = bccAddresses.filterNot(isFromCriptextDomain)

        return MailRecipients(toCriptext = toNonCriptext, ccCriptext = ccNonCriptext,
                bccCriptext = bccNonCriptext, peerCriptext = listOf(activeAccount.recipientId))
    }

    private fun findKnownAddresses(criptextRecipients: List<String>): Map<String, List<Int>> {
        val knownAddresses = HashMap<String, List<Int>>()
        val existingSessions = rawSessionDao.getKnownAddresses(criptextRecipients)
        existingSessions.forEach { knownAddress: KnownAddress ->
            knownAddresses[knownAddress.recipientId] = knownAddresses[knownAddress.recipientId]
                                                ?.plus(knownAddress.deviceId)
                                                ?: listOf(knownAddress.deviceId)
        }
        return knownAddresses
    }

    private fun addMissingSessions(criptextRecipients: List<String>) {
        val knownAddresses = findKnownAddresses(criptextRecipients)

        val findKeyBundlesResponse = apiClient.findKeyBundles(criptextRecipients, knownAddresses)
        val bundlesJSONArray = JSONArray(findKeyBundlesResponse)
        if (bundlesJSONArray.length() > 0) {
            val downloadedBundles = PreKeyBundleShareData.DownloadBundle.fromJSONArray(bundlesJSONArray)
            signalClient.createSessionsFromBundles(downloadedBundles)
        }
    }

    private fun encryptForCriptextRecipients(criptextRecipients: List<String>,
                                             availableAddresses: Map<String, List<Int>>,
                                             type: PostEmailBody.RecipientTypes)
            : List<PostEmailBody.CriptextEmail> {
        return criptextRecipients.map { recipientId ->
            val devices = availableAddresses[recipientId]
            if (devices == null || devices.isEmpty()) {
                if (type == PostEmailBody.RecipientTypes.peer)
                    return emptyList()
                throw IllegalArgumentException("Signal address for '$recipientId' does not exist in the store")
            }
            devices.filter { deviceId ->
                type != PostEmailBody.RecipientTypes.peer || deviceId != activeAccount.deviceId
            }.map { deviceId ->
                val encryptedData = signalClient.encryptMessage(recipientId, deviceId, composerInputData.body)
                PostEmailBody.CriptextEmail(recipientId = recipientId, deviceId = deviceId,
                        type = type, body = encryptedData.encryptedB64,
                        messageType = encryptedData.type, fileKey = if(fileKey != null)
                                signalClient.encryptMessage(recipientId, deviceId, fileKey).encryptedB64
                                else null)
            }
        }.flatten()
    }

    private fun createEncryptedEmails(mailRecipients: MailRecipients): List<PostEmailBody.CriptextEmail> {
        val knownCriptextAddresses = findKnownAddresses(mailRecipients.criptextRecipients)
        val criptextToEmails = encryptForCriptextRecipients(mailRecipients.toCriptext,
                knownCriptextAddresses, PostEmailBody.RecipientTypes.to)
        val criptextCcEmails = encryptForCriptextRecipients(mailRecipients.ccCriptext,
                knownCriptextAddresses, PostEmailBody.RecipientTypes.cc)
        val criptextBccEmails = encryptForCriptextRecipients(mailRecipients.bccCriptext,
                knownCriptextAddresses, PostEmailBody.RecipientTypes.bcc)
        val criptextPeerEmails = encryptForCriptextRecipients(mailRecipients.peerCriptext,
                knownCriptextAddresses, PostEmailBody.RecipientTypes.peer)
        return listOf(criptextToEmails, criptextCcEmails, criptextBccEmails, criptextPeerEmails).flatten()
    }

    override fun catchException(ex: Exception): MailboxResult.SendMail {
        val message = createErrorMessage(ex)
        return MailboxResult.SendMail.Failure(message)
    }

    private fun checkEncryptionKeysOperation(mailRecipients: MailRecipients)
            : Result<Unit, Exception> =
            Result.of { addMissingSessions(mailRecipients.criptextRecipients) }

    private fun encryptOperation(mailRecipients: MailRecipients)
            : Result<List<PostEmailBody.CriptextEmail>, Exception> =
            Result.of { createEncryptedEmails(mailRecipients) }

    private fun createCriptextAttachment(attachments: List<ComposerAttachment>)
            : List<PostEmailBody.CriptextAttachment> = attachments.map { attachment ->
        PostEmailBody.CriptextAttachment(token = attachment.filetoken,
                name = FileUtils.getName(attachment.filepath), size = attachment.size)
    }

    private val sendEmailOperation
            : (List<PostEmailBody.CriptextEmail>) -> Result<String, Exception> =
            { criptextEmails ->
                Result.of {
                    val requestBody = PostEmailBody(
                            threadId = threadId,
                            subject = composerInputData.subject,
                            criptextEmails = criptextEmails,
                            guestEmail = guestEmails,
                            attachments = createCriptextAttachment(this.attachments))
                    apiClient.postEmail(requestBody)
                }.mapError(HttpErrorHandlingHelper.httpExceptionsToNetworkExceptions)
            }

    private val updateSentMailInDB: (String) -> Result<Unit, Exception> =
            { response ->
               Result.of {
                   val sentMailData = SentMailData.fromJSON(JSONObject(response))
                   db.updateEmailAndAddLabelSent(id = emailId, threadId = sentMailData.threadId,
                       messageId = sentMailData.messageId, metadataKey = sentMailData.metadataKey,
                       status = DeliveryTypes.SENT,
                       date = DateUtils.getDateFromString(sentMailData.date, null)
                   )
               }
            }


    override fun work(reporter: ProgressReporter<MailboxResult.SendMail>): MailboxResult.SendMail? {
        val mailRecipients = getMailRecipients()
        val mailRecipientsNonCriptext = getMailRecipientsNonCriptext()
        guestEmails = if(!mailRecipientsNonCriptext.isEmpty)
            getGuestEmails(mailRecipientsNonCriptext)
        else
            null
        val result = checkEncryptionKeysOperation(mailRecipients)
                .flatMap { encryptOperation(mailRecipients) }
                .flatMap(sendEmailOperation)
                .flatMap(updateSentMailInDB)

        return when (result) {
            is Result.Success -> {
                MailboxResult.SendMail.Success(emailId)
            }
            is Result.Failure -> {
                val message = createErrorMessage(result.error)
                MailboxResult.SendMail.Failure(message)
            }
        }
    }

    private fun getGuestEmails(mailRecipientsNonCriptext: MailRecipients) : PostEmailBody.GuestEmail?{
        val postGuestEmailBody: PostEmailBody.GuestEmail?
        if(composerInputData.passwordForNonCriptextUsers == null) {
            postGuestEmailBody = PostEmailBody.GuestEmail(mailRecipientsNonCriptext.toCriptext,
                    mailRecipientsNonCriptext.ccCriptext, mailRecipientsNonCriptext.bccCriptext,
                    composerInputData.body, null, null, null)
        }else {
            val tempSignalUser = getDummySignalSession(composerInputData.passwordForNonCriptextUsers)
            val sessionToEncrypt = getSignalSessionJSON(tempSignalUser, tempSignalUser.fetchAPreKeyBundle()).toString().toByteArray()
            val (salt, iv, encryptedSession) =
                    AESUtil.encryptWithPassword(composerInputData.passwordForNonCriptextUsers, sessionToEncrypt)
            val encryptedBody = signalClient.encryptMessage(composerInputData.passwordForNonCriptextUsers,
                    1,composerInputData.body).encryptedB64
            postGuestEmailBody = PostEmailBody.GuestEmail(mailRecipientsNonCriptext.toCriptext,
                    mailRecipientsNonCriptext.ccCriptext, mailRecipientsNonCriptext.bccCriptext,
                    encryptedBody, salt, iv, encryptedSession)
            tempSignalUser.store.deleteAllSessions(composerInputData.passwordForNonCriptextUsers)
            rawSessionDao.deleteByRecipientId(composerInputData.passwordForNonCriptextUsers)
            rawIdentityKeyDao.deleteByRecipientId(composerInputData.passwordForNonCriptextUsers)

        }
        return postGuestEmailBody
    }

    private fun getDummySignalSession(recipientId: String): DummyUser{
        val keyGenerator = SignalKeyGenerator.Default(DeviceUtils.DeviceType.Android)
        val tempUser = InMemoryUser(keyGenerator, recipientId, 1).setup()
        val keyBundleFromTempUser = tempUser.fetchAPreKeyBundle()

        signalClient.createSessionsFromBundles(listOf(keyBundleFromTempUser))
        return tempUser
    }

    private fun getSignalSessionJSON(tempUser: DummyUser, keyBundleFromTempUser: PreKeyBundleShareData.DownloadBundle):JSONObject{
        val signedPreKey = tempUser.store.loadSignedPreKey(keyBundleFromTempUser.shareData.signedPreKeyId)

        val jsonReturn = JSONObject()
        val jsonIdentityKey = JSONObject()
        val jsonPreKey = JSONObject()
        val jsonSignedPreKey = JSONObject()
        jsonIdentityKey.put("publicKey", Encoding.byteArrayToString(tempUser.store.identityKeyPair.publicKey.serialize()))
        jsonIdentityKey.put("privateKey", Encoding.byteArrayToString(tempUser.store.identityKeyPair.privateKey.serialize()))
        jsonReturn.put("identityKey", jsonIdentityKey)
        jsonReturn.put("registrationId", tempUser.store.localRegistrationId)
        jsonPreKey.put("keyId", keyBundleFromTempUser.preKey?.id)
        jsonPreKey.put("publicKey", Encoding.byteArrayToString(tempUser.store.loadPreKey(keyBundleFromTempUser.preKey!!.id).keyPair.publicKey.serialize()))
        jsonPreKey.put("privateKey", Encoding.byteArrayToString(tempUser.store.loadPreKey(keyBundleFromTempUser.preKey.id).keyPair.privateKey.serialize()))
        jsonReturn.put("preKey", jsonPreKey)
        jsonSignedPreKey.put("keyId", keyBundleFromTempUser.shareData.signedPreKeyId)
        jsonSignedPreKey.put("publicKey", Encoding.byteArrayToString(signedPreKey.keyPair.publicKey.serialize()))
        jsonSignedPreKey.put("privateKey", Encoding.byteArrayToString(signedPreKey.keyPair.privateKey.serialize()))
        jsonReturn.put("signedPreKey", jsonSignedPreKey)
        jsonReturn.put("fileKey", fileKey)
        println(jsonReturn.toString())
        return jsonReturn
    }

    private val createErrorMessage: (ex: Exception) -> UIMessage = { ex ->
        when (ex) {
            is JSONException -> UIMessage(resId = R.string.send_json_error)
            is ServerErrorException ->
                UIMessage(resId = R.string.send_bad_status, args = arrayOf(ex.errorCode))
            is NetworkErrorException -> UIMessage(resId = R.string.send_network_error)
            else -> UIMessage(resId = R.string.send_try_again_error)
        }
    }

    override fun cancel() {
        TODO("not implemented") //To change body of created functions use CRFile | Settings | CRFile Templates.
    }

    private class MailRecipients(val toCriptext: List<String>, val ccCriptext: List<String>,
                                 val bccCriptext: List<String>, val peerCriptext: List<String>) {
        val criptextRecipients = listOf(toCriptext, ccCriptext, bccCriptext, peerCriptext).flatten()
        val isEmpty = toCriptext.isEmpty() && ccCriptext.isEmpty() && bccCriptext.isEmpty()
    }

}