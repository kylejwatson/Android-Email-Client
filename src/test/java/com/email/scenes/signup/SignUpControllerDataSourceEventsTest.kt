package com.email.scenes.signup

import com.email.R
import com.email.scenes.signup.data.SignUpResult
import com.email.validation.FormInputState
import com.email.utils.UIMessage
import io.mockk.*
import org.amshove.kluent.`should be instance of`
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * Created by gabriel on 5/16/18.
 */
class SignUpControllerDataSourceEventsTest: SignUpControllerTest() {

    private val listenerSlot = CapturingSlot<(SignUpResult) -> Unit>()

    @Before
    override fun setUp() {
        super.setUp()
        // mock SignInScene capturing the UI Observer
        every { dataSource::listener.set(capture(listenerSlot)) } just Runs

        controller.onStart(null)
        clearMocks(scene)
    }

    @Test
    fun `when register user fails, it should show an error, without leaving the activity`() {
        val errorMessage = UIMessage(R.string.username_invalid_error)

        listenerSlot.captured(SignUpResult.RegisterUser.Failure(errorMessage, IOException()))

        verify { scene.showError(errorMessage) }
        verify(inverse = true) { host.exitToScene(any(), any()) }
    }

    @Test
    fun `when register user succeeds, it show success in the UI`() {

        listenerSlot.captured(SignUpResult.RegisterUser.Success())

        // no errors!
        verify(inverse = true) { scene.showError(any()) }
        verify { scene.showSuccess() }
    }

    @Test
    fun `when check username availability succeeds with isAvailable=true, sets username as valid`() {

        listenerSlot.captured(SignUpResult.CheckUsernameAvailability.Success(isAvailable = true))

        verify { scene.setUsernameState(FormInputState.Valid()) }
    }

    @Test
    fun `when check username availability succeeds with isAvailable=false, sets username as error`() {

        listenerSlot.captured(SignUpResult.CheckUsernameAvailability.Success(isAvailable = false))

        verify { scene.setUsernameState(FormInputState.Error(UIMessage(R.string.taken_username_error))) }

        model.username.state `should be instance of`  FormInputState.Error::class.java
    }
}
