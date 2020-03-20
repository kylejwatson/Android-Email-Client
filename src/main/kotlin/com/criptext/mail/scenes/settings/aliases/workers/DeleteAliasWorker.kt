package com.criptext.mail.scenes.settings.aliases.workers

import com.criptext.mail.R
import com.criptext.mail.api.HttpClient
import com.criptext.mail.api.HttpErrorHandlingHelper
import com.criptext.mail.api.ServerErrorException
import com.criptext.mail.bgworker.BackgroundWorker
import com.criptext.mail.bgworker.ProgressReporter
import com.criptext.mail.db.KeyValueStorage
import com.criptext.mail.db.dao.AccountDao
import com.criptext.mail.db.dao.AliasDao
import com.criptext.mail.db.dao.CustomDomainDao
import com.criptext.mail.db.models.ActiveAccount
import com.criptext.mail.db.models.Alias
import com.criptext.mail.db.models.Contact
import com.criptext.mail.db.models.CustomDomain
import com.criptext.mail.scenes.settings.aliases.data.AliasesAPIClient
import com.criptext.mail.scenes.settings.aliases.data.AliasesResult
import com.criptext.mail.scenes.settings.custom_domain.data.CustomDomainAPIClient
import com.criptext.mail.scenes.settings.custom_domain.data.CustomDomainResult
import com.criptext.mail.utils.DateAndTimeUtils
import com.criptext.mail.utils.ServerCodes
import com.criptext.mail.utils.UIMessage
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.mapError
import org.json.JSONObject

class DeleteAliasWorker(
        private val storage: KeyValueStorage,
        private val accountDao: AccountDao,
        private val httpClient: HttpClient,
        private val activeAccount: ActiveAccount,
        private val alias: String,
        private val domain: String?,
        private val position: Int,
        private val aliasDao: AliasDao,
        override val publishFn: (
                AliasesResult.DeleteAlias) -> Unit)
    : BackgroundWorker<AliasesResult.DeleteAlias> {

    override val canBeParallelized = true
    private val apiClient = AliasesAPIClient(httpClient, activeAccount.jwt)

    override fun catchException(ex: Exception): AliasesResult.DeleteAlias {
        return AliasesResult.DeleteAlias.Failure(UIMessage(R.string.unknown_error, arrayOf(ex.toString())))
    }

    override fun work(reporter: ProgressReporter<AliasesResult.DeleteAlias>): AliasesResult.DeleteAlias? {
        val operation = workOperation()

        val sessionExpired = HttpErrorHandlingHelper.didFailBecauseInvalidSession(operation)

        val finalResult = if(sessionExpired)
            newRetryWithNewSessionOperation()
        else
            operation

        return when (finalResult){
            is Result.Success -> {
                AliasesResult.DeleteAlias.Success(alias, domain, position)
            }
            is Result.Failure -> {
                catchException(finalResult.error)
            }
        }
    }

    override fun cancel() {
        TODO("CANCEL IS NOT IMPLEMENTED")
    }

    private fun workOperation() : Result<Unit, Exception> = Result.of {
        aliasDao.getAliasByName(alias, domain, activeAccount.id) ?: throw Exception()
    }.flatMap {  Result.of { apiClient.deleteAlias(it.rowId) }
    }.mapError(HttpErrorHandlingHelper.httpExceptionsToNetworkExceptions)
    .flatMap { Result.of {
        aliasDao.deleteByName(alias, domain, activeAccount.id)
    } }


    private fun newRetryWithNewSessionOperation()
            : Result<Unit, Exception> {
        val refreshOperation =  HttpErrorHandlingHelper.newRefreshSessionOperation(apiClient, activeAccount, storage, accountDao)
                .mapError(HttpErrorHandlingHelper.httpExceptionsToNetworkExceptions)
        return when(refreshOperation){
            is Result.Success -> {
                apiClient.token = refreshOperation.value
                workOperation()
            }
            is Result.Failure -> {
                Result.of { throw refreshOperation.error }
            }
        }
    }

}