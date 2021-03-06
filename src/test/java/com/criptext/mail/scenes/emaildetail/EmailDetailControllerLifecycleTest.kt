package com.criptext.mail.scenes.emaildetail

import com.criptext.mail.scenes.emaildetail.data.EmailDetailRequest
import org.amshove.kluent.`should be empty`
import org.amshove.kluent.`should be instance of`
import org.junit.Before
import org.junit.Test

/**
 * Created by gabriel on 6/27/18.
 */

class EmailDetailControllerLifecycleTest: EmailDetailControllerTest() {

    @Before
    override fun setUp() {
        super.setUp()
    }

    @Test
    fun `onStart, if model has NO emails, should send request to load emails`() {

        controller.onStart(null)

        val sentRequest = sentRequests.first()
        sentRequest `should be instance of` EmailDetailRequest.LoadFullEmailsFromThreadId::class.java
    }

    @Test
    fun `onStart, if model has emails, should not send any requests`() {

        model.emails.addAll(createEmailItemsInThread(4))

        controller.onStart(null)

        sentRequests.`should be empty`()
    }
}