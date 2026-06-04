package com.dkvb.skillswap

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MessageAdapter(
    private val messages: List<Message>,
    private val currentUid: String
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    companion object {
        const val VIEW_TYPE_SENT = 1
        const val VIEW_TYPE_RECEIVED = 2
    }

    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMessage: TextView = view.findViewById(R.id.tvMessage)
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].senderId == currentUid) VIEW_TYPE_SENT
        else VIEW_TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layout = if (viewType == VIEW_TYPE_SENT) R.layout.item_message_sent
        else R.layout.item_message_received
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.tvMessage.text = message.text

        // Apply theme colors to bubble
        val bubbleColor = if (message.senderId == currentUid) {
            ThemeManager.parseColor(ThemeManager.getBubbleSent())
        } else {
            ThemeManager.parseColor(ThemeManager.getBubbleReceived())
        }

        val textColor = if (message.senderId == currentUid) {
            ThemeManager.parseColor(ThemeManager.getButtonText())
        } else {
            ThemeManager.parseColor(ThemeManager.getTextPrimary())
        }

        // Apply rounded bubble shape with theme color
        val drawable = GradientDrawable()
        drawable.shape = GradientDrawable.RECTANGLE
        drawable.cornerRadii = if (message.senderId == currentUid) {
            floatArrayOf(16f, 16f, 16f, 16f, 4f, 4f, 16f, 16f)
        } else {
            floatArrayOf(16f, 16f, 16f, 16f, 16f, 16f, 4f, 4f)
        }
        drawable.setColor(bubbleColor)
        holder.tvMessage.background = drawable
        holder.tvMessage.setTextColor(textColor)
    }

    override fun getItemCount() = messages.size
}