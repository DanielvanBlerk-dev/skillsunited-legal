package com.dkvb.skillswap

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class InboxAdapter(
    private val messages: List<InboxMessage>,
    private val onClick: (InboxMessage) -> Unit
) : RecyclerView.Adapter<InboxAdapter.InboxViewHolder>() {

    class InboxViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val avatarView: AvatarView = view.findViewById(R.id.avatarView)
        val tvSenderName: TextView = view.findViewById(R.id.tvSenderName)
        val tvMessagePreview: TextView = view.findViewById(R.id.tvMessagePreview)
        val tvTimestamp: TextView = view.findViewById(R.id.tvTimestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InboxViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_inbox_message, parent, false)
        return InboxViewHolder(view)
    }

    override fun onBindViewHolder(holder: InboxViewHolder, position: Int) {
        val message = messages[position]
        val textPrimary = ThemeManager.parseColor(ThemeManager.getTextPrimary())
        val textSecondary = ThemeManager.parseColor(ThemeManager.getTextSecondary())
        val bg = ThemeManager.parseColor(ThemeManager.getBackground())
        val surface = ThemeManager.parseColor(ThemeManager.getSurface())
        val primary = ThemeManager.parseColor(ThemeManager.getPrimary())

        android.util.Log.d("BoldDebug", "Binding: ${message.senderName} isUnread=${message.isUnread}")

        holder.itemView.setBackgroundColor(if (position % 2 == 0) bg else surface)
        holder.avatarView.setName(message.senderName)

        holder.tvSenderName.text = message.senderName
        holder.tvSenderName.setTextColor(
            if (message.text.startsWith("You:")) textSecondary else primary
        )
        holder.tvSenderName.setTypeface(
            null,
            if (message.isUnread) android.graphics.Typeface.BOLD
            else android.graphics.Typeface.NORMAL
        )

        holder.tvMessagePreview.text = message.text
        holder.tvMessagePreview.setTextColor(textSecondary)
        holder.tvMessagePreview.setTypeface(
            null,
            if (message.isUnread) android.graphics.Typeface.BOLD
            else android.graphics.Typeface.NORMAL
        )

        val sdf = java.text.SimpleDateFormat("dd MMM, HH:mm", java.util.Locale.getDefault())
        holder.tvTimestamp.text = sdf.format(java.util.Date(message.timestamp))
        holder.tvTimestamp.setTextColor(textSecondary)

        holder.itemView.setOnClickListener { onClick(message) }
    }

    override fun getItemCount() = messages.size
}