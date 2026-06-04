package com.dkvb.skillswap

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class UserAdapter(
    private val users: List<User>,
    private val onClick: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: CardView = view.findViewById(R.id.cardView)
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvUsername: TextView = view.findViewById(R.id.tvUsername)
        val tvTeach: TextView = view.findViewById(R.id.tvTeach)
        val tvLearn: TextView = view.findViewById(R.id.tvLearn)
        val avatarView: AvatarView = view.findViewById(R.id.avatarView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]

        // Always explicitly apply theme colors on every bind
        val cardBg = ThemeManager.parseColor(ThemeManager.getCardBackground())
        val textPrimary = ThemeManager.parseColor(ThemeManager.getTextPrimary())
        val primary = ThemeManager.parseColor(ThemeManager.getPrimary())
        val secondary = ThemeManager.parseColor(ThemeManager.getSecondary())

        holder.card.setCardBackgroundColor(cardBg)
        holder.tvName.text = user.name
        holder.tvName.setTextColor(textPrimary)
        holder.tvUsername.text = if (user.username.isNotEmpty()) "@${user.username}" else ""
        holder.tvUsername.setTextColor(secondary)
        holder.tvTeach.text = "Teaches: ${user.skillsToTeach.joinToString(", ").ifEmpty { "Nothing yet" }}"
        holder.tvTeach.setTextColor(primary)
        holder.tvLearn.text = "Wants to learn: ${user.skillsToLearn.joinToString(", ").ifEmpty { "Nothing yet" }}"
        holder.tvLearn.setTextColor(secondary)
        holder.avatarView.setName(user.name)
        holder.itemView.setOnClickListener { onClick(user) }
    }

    override fun getItemCount() = users.size
}