package com.criptext.mail.scenes.mailbox.data

import com.criptext.mail.R
import com.criptext.mail.api.HttpClient
import com.criptext.mail.api.HttpErrorHandlingHelper
import com.criptext.mail.bgworker.BackgroundWorker
import com.criptext.mail.bgworker.ProgressReporter
import com.criptext.mail.db.EventLocalDB
import com.criptext.mail.db.models.ActiveAccount
import com.criptext.mail.db.models.Label
import com.criptext.mail.email_preview.EmailPreview
import com.criptext.mail.signal.SignalClient
import com.criptext.mail.utils.EventHelper
import com.criptext.mail.utils.UIMessage
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.mapError
import org.whispersystems.libsignal.DuplicateMessageException

/**
 * Created by sebas on 3/22/18.
 */

class UpdateMailboxWorker(
        signalClient: SignalClient,
        dbEvents: EventLocalDB,
        activeAccount: ActiveAccount,
        private val loadedThreadsCount: Int,
        private val label: Label,
        httpClient: HttpClient,
        override val publishFn: (
                MailboxResult.UpdateMailbox) -> Unit)
    : BackgroundWorker<MailboxResult.UpdateMailbox> {


    override val canBeParallelized = false

    private val eventHelper = EventHelper(dbEvents, httpClient, activeAccount, signalClient, true)

    override fun catchException(ex: Exception): MailboxResult.UpdateMailbox {
        val message = createErrorMessage(ex)
        return MailboxResult.UpdateMailbox.Failure(label, message, ex)
    }

    private fun processFailure(failure: Result.Failure<List<EmailPreview>, Exception>): MailboxResult.UpdateMailbox {
        return if (failure.error is EventHelper.NothingNewException)
            MailboxResult.UpdateMailbox.Success(
                    mailboxLabel = label,
                    isManual = true,
                    mailboxThreads = null)
        else
            MailboxResult.UpdateMailbox.Failure(
                    mailboxLabel = label,
                    message = createErrorMessage(failure.error),
                    exception = failure.error)
    }

    override fun work(reporter: ProgressReporter<MailboxResult.UpdateMailbox>)
            : MailboxResult.UpdateMailbox? {
        eventHelper.setupForMailbox(label, loadedThreadsCount)
        val operationResult = eventHelper.fetchPendingEvents()
                .mapError(HttpErrorHandlingHelper.httpExceptionsToNetworkExceptions)
                .flatMap(eventHelper.parseEvents)
                .flatMap(eventHelper.processEvents)

        return when(operationResult) {
            is Result.Success -> {
                return MailboxResult.UpdateMailbox.Success(
                        mailboxLabel = label,
                        isManual = true,
                        mailboxThreads = operationResult.value
                )
            }

            is Result.Failure -> processFailure(operationResult)
        }
    }

    override fun cancel() {
        TODO("CANCEL IS NOT IMPLEMENTED")
    }

    private val createErrorMessage: (ex: Exception) -> UIMessage = { ex ->
        when(ex) {
            is DuplicateMessageException ->
                UIMessage(resId = R.string.email_already_decrypted)
            else -> {
                UIMessage(resId = R.string.failed_getting_emails)
            }
        }
    }
}
