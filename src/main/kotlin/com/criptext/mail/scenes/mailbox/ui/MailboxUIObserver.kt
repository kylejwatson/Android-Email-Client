package com.criptext.mail.scenes.mailbox.ui

/**
 * Created by gabriel on 2/28/18.
 */

interface MailboxUIObserver {
    fun onOpenComposerButtonClicked()
    fun onRefreshMails()
    fun onBackButtonPressed()
    fun onFeedDrawerClosed()
}