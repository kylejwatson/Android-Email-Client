package com.email.scenes.emaildetail

import android.support.v7.widget.RecyclerView
import android.view.View
import com.email.IHostActivity
import com.email.R
import com.email.db.models.FullEmail
import com.email.db.models.Label
import com.email.scenes.labelChooser.LabelChooserDialog
import com.email.scenes.emaildetail.ui.FullEmailListAdapter
import com.email.scenes.emaildetail.ui.FullEmailRecyclerView
import com.email.scenes.emaildetail.ui.labels.LabelsRecyclerView
import com.email.scenes.labelChooser.LabelDataHandler
import com.email.scenes.mailbox.MoveToDialog
import com.email.scenes.mailbox.OnMoveThreadsListener
import com.email.utils.VirtualList

/**
 * Created by sebas on 3/12/18.
 */

interface EmailDetailScene {

    fun attachView(
            fullEmailEventListener: FullEmailListAdapter.OnFullEmailEventListener,
            fullEmailList : VirtualList<FullEmail>)

    fun notifyFullEmailListChanged()
    fun notifyFullEmailChanged(position: Int)
    fun showDialogLabelsChooser(labelDataHandler: LabelDataHandler)
    fun showDialogMoveTo(onMoveThreadsListener: OnMoveThreadsListener)
    fun onFetchedLabels(
            defaultSelectedLabels: List<Label>,
            labels: List<Label>)

    class EmailDetailSceneView(
            private val emailDetailView: View,
            val hostActivity: IHostActivity)
        : EmailDetailScene {

        private val context = emailDetailView.context


        private lateinit var fullEmailsRecyclerView: FullEmailRecyclerView
        private lateinit var labelsRecyclerView: LabelsRecyclerView

        private val labelChooserDialog = LabelChooserDialog(context)
        private val moveToDialog = MoveToDialog(context)

        private val recyclerView: RecyclerView by lazy {
            emailDetailView.findViewById<RecyclerView>(R.id.emails_detail_recycler)
        }

        private val recyclerLabelsView: RecyclerView by lazy {
            emailDetailView.findViewById<RecyclerView>(R.id.labels_recycler)
        }

        override fun attachView(
                fullEmailEventListener: FullEmailListAdapter.OnFullEmailEventListener,
                fullEmailList : VirtualList<FullEmail> ){

            labelsRecyclerView = LabelsRecyclerView(recyclerLabelsView, getLabelsFromEmails(fullEmailList))

            fullEmailsRecyclerView = FullEmailRecyclerView(
                    recyclerView,
                    fullEmailEventListener,
                    fullEmailList)

        }

        private fun getLabelsFromEmails(
                emails: VirtualList<FullEmail>) : VirtualList<Label>{
            val labelSet = HashSet<Label>()
            for (i in 0 until emails.size) {
                labelSet.addAll(emails[i].labels)
            }
            val labelsList = ArrayList(labelSet)
            return VirtualList.Map(labelsList, { t->t })
        }

        override fun notifyFullEmailListChanged() {
            fullEmailsRecyclerView.notifyFullEmailListChanged()
        }

        override fun notifyFullEmailChanged(position: Int) {
            fullEmailsRecyclerView.notifyFullEmailChanged(position = position)
        }

        override fun showDialogLabelsChooser(labelDataHandler: LabelDataHandler) {
            labelChooserDialog.showDialogLabelsChooser(dataHandler = labelDataHandler)
        }

        override fun onFetchedLabels(defaultSelectedLabels: List<Label>, labels: List<Label>) {
            labelChooserDialog.onFetchedLabels(
                    defaultSelectedLabels = defaultSelectedLabels,
                    allLabels = labels)
        }

        override fun showDialogMoveTo(onMoveThreadsListener: OnMoveThreadsListener) {
            moveToDialog.showMoveToDialog(
                    onMoveThreadsListener = onMoveThreadsListener)
        }

        override fun onDecryptedBody(decryptedText: String) {
        }
    }

}
