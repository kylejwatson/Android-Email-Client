package com.email.scenes.signin

import android.annotation.SuppressLint
import android.os.Handler
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.email.R
import com.email.scenes.signin.holders.ConnectionHolder
import com.email.scenes.signin.holders.LoginValidationHolder
import com.email.scenes.signin.holders.PasswordLoginHolder
import com.email.scenes.signin.holders.SignInFormHolder
import com.email.utils.UIMessage
import com.email.utils.getLocalizedUIMessage

/**
 * Created by sebas on 2/15/18.
 */

interface SignInScene {
    fun drawNormalSignInOptions()
    fun toggleSignUpPressed(isPressed: Boolean)
    fun toggleLoginProgressBar(isLoggingIn : Boolean)
    fun drawError()
    fun showError(message: UIMessage)
    fun drawSuccess()
    fun initListeners(signInListener: SignInSceneController.SignInListener)
    fun showConnectionHolder()
    fun showFormHolder()
    fun startLoadingAnimation()
    fun startSucceedAnimation(showForm: (
            signInListener: SignInSceneController.SignInListener) -> Unit)
    fun stopAnimationLoading()
    fun startAnimation()
    fun initFormUI()
    fun showLoginValidationHolder()
    fun showPasswordLoginHolder(user: String)
    fun showPasswordLoginDialog(
            onPasswordLoginDialogListener: OnPasswordLoginDialogListener)
    fun toggleConfirmButton(activated: Boolean)

    var signInListener: SignInSceneController.SignInListener?

    class SignInSceneView(val view: View): SignInScene {

        override fun showPasswordLoginDialog(
                onPasswordLoginDialogListener: OnPasswordLoginDialogListener) {
            loginValidationHolder?.showPasswordLoginDialog(onPasswordLoginDialogListener)
        }

        override fun startAnimation() {
            startLoadingAnimation()

            Handler().postDelayed({
                startSucceedAnimation(showMailboxScene)
            }, 3000)
        }

        private val res = view.context.resources
        private val viewGroup = view.parent as ViewGroup
        private var signInFormHolder: SignInFormHolder? = null
        private var connectionHolder: ConnectionHolder? = null
        private var loginValidationHolder: LoginValidationHolder? = null
        private var passwordLoginHolder: PasswordLoginHolder? = null

        override var signInListener: SignInSceneController.SignInListener? = null
            set(value) {
                signInFormHolder?.signInListener = value
                connectionHolder?.signInListener = value
                loginValidationHolder?.signInListener = value
                passwordLoginHolder?.signInListener = value
                field = value
            }
        private fun assignForgotPasswordClickListener() {
            passwordLoginHolder?.assignForgotPasswordClickListener()
        }

        private fun assignConfirmButtonListener() {
            passwordLoginHolder?.assignConfirmButtonListener()
        }
        private fun assignPasswordChangeListener() {
            passwordLoginHolder?.assignPasswordChangeListener()
        }
        private fun assignCantAccessDeviceListener() {
            loginValidationHolder?.assignCantAccessDeviceListener()
        }
        private fun assignUsernameInputListener(){
            signInFormHolder?.assignUsernameInputListener()
        }

        @SuppressLint("ClickableViewAccessibility")
        private fun assignSignUpTextViewListener() {
            signInFormHolder?.assignSignUpTextViewListener()
        }

        private fun assignLoginButtonListener() {
            signInFormHolder?.assignLoginButtonListener()
        }

        override fun toggleSignUpPressed(isPressed: Boolean){
            signInFormHolder?.toggleSignUpPressed(isPressed)
        }
        @SuppressLint("RestrictedApi")
        override fun drawError() {
            signInFormHolder?.drawError()
            passwordLoginHolder?.drawError()
        }

        override fun showError(message: UIMessage) {
            val duration = Toast.LENGTH_LONG
            val toast = Toast.makeText(
                    view.context,
                    view.context.getLocalizedUIMessage(message),
                    duration)
            toast.show()
        }
        @SuppressLint("RestrictedApi")
        override fun drawNormalSignInOptions(){
            signInFormHolder?.drawNormalSignInOptions()
        }

        override fun drawSuccess() {
            TODO("Show progress dialog...")
        }

        override fun toggleLoginProgressBar(isLoggingIn : Boolean){
            signInFormHolder?.toggleLoginProgressBar(isLoggingIn)
        }

        init {
            showFormHolder()
        }

        override fun initFormUI() {
        }

        override fun initListeners(
                signInListener: SignInSceneController.SignInListener
        ) {
            this.signInListener = signInListener
            assignLoginButtonListener()
            assignSignUpTextViewListener()
            assignUsernameInputListener()
            assignCantAccessDeviceListener()
            assignConfirmButtonListener()
            assignPasswordChangeListener()
            assignForgotPasswordClickListener()
          }


        override fun showConnectionHolder() {
            removeAllViews()
            val connectionLayout = View.inflate(
                    view.context,
                    R.layout.activity_connection, viewGroup)

            connectionHolder = ConnectionHolder(connectionLayout)
            connectionHolder?.signInListener = signInListener
        }

        private fun removeAllViews() {
            viewGroup.removeAllViews()
            connectionHolder?.signInListener = null
            signInFormHolder?.signInListener = null
            loginValidationHolder?.signInListener = null
            passwordLoginHolder?.signInListener = null
        }

        override fun showFormHolder() {
            removeAllViews()
            val layout = View.inflate(
                    view.context,
                    R.layout.activity_form_signin, viewGroup)
            val formLayout = layout.findViewById<View>(R.id.signin_form_container)
            signInFormHolder = SignInFormHolder(formLayout)
        }


        override fun startLoadingAnimation() {
            connectionHolder?.startLoadingAnimation()
        }

        override fun startSucceedAnimation(launchMailboxScene: (
                signInListener: SignInSceneController.SignInListener) -> Unit) {
            connectionHolder?.startSucceedAnimation(launchMailboxScene)
        }
        private val showMailboxScene = {
            signInListener: SignInSceneController.SignInListener ->
            signInListener.userLoginReady()
        }

        override fun stopAnimationLoading() {
            connectionHolder?.stopAnimationLoading()
        }

        override fun showLoginValidationHolder() {
            removeAllViews()
            val layout = View.inflate(
                    view.context,
                    R.layout.activity_login_validation, viewGroup)
            loginValidationHolder = LoginValidationHolder(layout)
            initListeners(signInListener!!)
        }

        override fun showPasswordLoginHolder(username: String) {
            removeAllViews()
            val layout = View.inflate(
                    view.context,
                    R.layout.activity_password_login, viewGroup)
            passwordLoginHolder = PasswordLoginHolder(
                    layout,
                    user = username)
            initListeners(signInListener!!)
        }

        override fun toggleConfirmButton(activated: Boolean) {
            passwordLoginHolder?.toggleConfirmButton(activated = activated)
        }
    }
}

