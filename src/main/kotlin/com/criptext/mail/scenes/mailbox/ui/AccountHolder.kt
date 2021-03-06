package com.criptext.mail.scenes.mailbox.ui

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.criptext.mail.R
import com.criptext.mail.db.AccountTypes
import com.criptext.mail.db.models.Account
import com.criptext.mail.utils.AccountUtils
import com.criptext.mail.utils.UIUtils
import de.hdodenhof.circleimageview.CircleImageView


class AccountHolder(val view: View) : RecyclerView.ViewHolder(view){

    private val nameView : TextView = view.findViewById(R.id.textViewNombre) as TextView
    private val mailView : TextView = view.findViewById(R.id.textViewCorreo) as TextView
    private val badgeNumber : TextView = view.findViewById(R.id.badgeNumber) as TextView
    private val avatarView : CircleImageView = view.findViewById(R.id.accountItemAvatar)
    private val avatarRingView : ImageView = view.findViewById(R.id.plusBadgeRing)
    private val rootView: View = view.findViewById(R.id.rootView)

    fun bindAccount(account: Account, badgeCount: Int) {
        nameView.text = account.name
        val email = account.recipientId.plus("@").plus(account.domain)
        mailView.text = email

        UIUtils.setProfilePicture(
                avatar = avatarView,
                avatarRing = avatarRingView,
                resources = avatarView.context.resources,
                recipientId = account.recipientId,
                name = account.name,
                runnable = null,
                domain = account.domain)

        if(AccountUtils.isPlus(account.type)){
            avatarRingView.visibility = View.VISIBLE
        } else {
            avatarRingView.visibility = View.GONE
        }

        if(badgeCount > 0) {
            badgeNumber.visibility = View.VISIBLE
            badgeNumber.text = badgeCount.toString()
        }else{
            badgeNumber.visibility = View.GONE
            badgeNumber.text = ""
        }
    }

    fun setOnClickedListener(onClick: () -> Unit) {
        rootView.setOnClickListener {
            onClick()
        }
    }
}
