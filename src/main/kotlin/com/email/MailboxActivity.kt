package com.email

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.email.DB.MailboxLocalDB
import com.email.androidui.SceneFactory
import com.email.scenes.LabelChooser.DialogLabelsChooser
import com.email.scenes.LabelChooser.LabelChooserSceneController
import com.email.scenes.LabelChooser.LabelChooserSceneModel
import com.email.scenes.LabelChooser.data.LabelChooserDataSource
import com.email.scenes.mailbox.MailboxSceneController
import com.email.scenes.mailbox.MailboxSceneModel
import com.email.scenes.mailbox.data.MailboxDataSource

/**
 * Created by sebas on 1/30/18.
 */

class MailboxActivity : AppCompatActivity(), IHostActivity, DialogLabelsChooser.DialogLabelsListener {

    private lateinit var sceneFactory : SceneFactory

    private lateinit var mailboxSceneController: MailboxSceneController
    private lateinit var labelChooserSceneController: LabelChooserSceneController
    private var mailboxSceneModel : MailboxSceneModel = MailboxSceneModel()
    private var labelChooserSceneModel : LabelChooserSceneModel = LabelChooserSceneModel()
    private val threadListHandler : ThreadListHandler = ThreadListHandler(mailboxSceneModel)
    private val labelThreadListHandler : LabelThreadListHandler = LabelThreadListHandler(labelChooserSceneModel)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.main_layout)
        sceneFactory = SceneFactory.SceneInflater(this, threadListHandler, labelThreadListHandler)

        initController()
    }

    override fun initController() {
        val DB : MailboxLocalDB.Default = MailboxLocalDB.Default(this.applicationContext)
        mailboxSceneController = MailboxSceneController(
                    scene = sceneFactory.createMailboxScene(),
                    model = mailboxSceneModel,
                    dataSource = MailboxDataSource(DB))
    }

    fun startLabelChooserDialog() {
        val DB : MailboxLocalDB.Default = MailboxLocalDB.Default(this.applicationContext)
         labelChooserSceneController = LabelChooserSceneController(
                scene = sceneFactory.createChooserDialogScene(),
                model = labelChooserSceneModel,
                dataSource = LabelChooserDataSource(DB))

        labelChooserSceneController.onStart()
    }

    override fun onStart() {
        super.onStart()
        mailboxSceneController.onStart()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return mailboxSceneController.onOptionSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if(!mailboxSceneModel.isInMultiSelect) {
            menu?.clear()
            menuInflater.inflate(R.menu.mailbox_menu_normal_mode, menu) // rendering normal mode items...
        } else {
            menu?.clear()
           menuInflater.inflate(R.menu.mailbox_menu_multi_mode, menu) // rendering multi mode items...
        }
        mailboxSceneController.toggleMultiModeBar()
        return super.onCreateOptionsMenu(menu)
    }

    override fun onBackPressed() {
        mailboxSceneController.onBackPressed(this)
    }

    override fun onDialogPositiveClick(dialog: DialogFragment) {
        labelChooserSceneController.assignLabels(mailboxSceneModel.selectedThreads)
        labelChooserSceneController.clearSelectedLabels()
        mailboxSceneController.changeMode(multiSelectON = false, silent = false)
        dialog.dismiss()
    }

    override fun onDialogNegativeClick(dialog: DialogFragment) {
        dialog.dismiss()
    }

    inner class ThreadListHandler(val model: MailboxSceneModel) {
        val getThreadFromIndex = {
            i: Int ->
            model.threads[i]
        }
        val getEmailThreadsCount = {
            model.threads.size
        }
    }

    inner class LabelThreadListHandler(val model: LabelChooserSceneModel) {
        val getLabelThreadFromIndex = {
            i: Int ->
            model.labels[i]
        }
        val getLabelThreadsCount = {
            model.labels.size
        }
    }
}