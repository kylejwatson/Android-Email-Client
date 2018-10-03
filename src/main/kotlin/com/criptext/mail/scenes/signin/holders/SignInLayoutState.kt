package com.criptext.mail.scenes.signin.holders

import com.criptext.mail.validation.ProgressButtonState

/**
 * Created by gabriel on 5/18/18.
 */
sealed class SignInLayoutState {
    data class Start(val username: String, val firstTime: Boolean): SignInLayoutState()
    data class LoginValidation(val username: String): SignInLayoutState()
    data class InputPassword(val username: String, val password: String,
                             val buttonState: ProgressButtonState): SignInLayoutState()
    data class WaitForApproval(val username: String) : SignInLayoutState()
}