package com.dkvb.skillswap

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class MatchRequestAdapter(
    private val requests: List<MatchRequest>,
    private val showAcceptDecline: Boolean = true,
    private val onAccept: (MatchRequest) -> Unit,
    private val onDecline: (MatchRequest) -> Unit
) : RecyclerView.Adapter<MatchRequestAdapter.RequestViewHolder>() {

    class RequestViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val avatarView: AvatarView = view.findViewById(R.id.avatarView)
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvUsername: TextView = view.findViewById(R.id.tvUsername)
        val tvBio: TextView = view.findViewById(R.id.tvBio)
        val tvTheirSkillsHeader: TextView = view.findViewById(R.id.tvTheirSkillsHeader)
        val tvTeach: TextView = view.findViewById(R.id.tvTeach)
        val tvLearn: TextView = view.findViewById(R.id.tvLearn)
        val tvYourSkillsHeader: TextView = view.findViewById(R.id.tvYourSkillsHeader)
        val tvYourTeach: TextView = view.findViewById(R.id.tvYourTeach)
        val tvYourLearn: TextView = view.findViewById(R.id.tvYourLearn)
        val btnAccept: Button = view.findViewById(R.id.btnAccept)
        val btnDecline: Button = view.findViewById(R.id.btnDecline)
        val btnCancel: Button = view.findViewById(R.id.btnCancel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_match_request, parent, false)
        return RequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        val request = requests[position]
        val textPrimary = ThemeManager.parseColor(ThemeManager.getTextPrimary())
        val textSecondary = ThemeManager.parseColor(ThemeManager.getTextSecondary())
        val primary = ThemeManager.parseColor(ThemeManager.getPrimary())
        val surface = ThemeManager.parseColor(ThemeManager.getSurface())

        // Apply theme to card and content
        (holder.itemView as? CardView)?.setCardBackgroundColor(surface)
        holder.itemView.findViewById<LinearLayout>(R.id.cardContent)
            ?.setBackgroundColor(surface)

        // Theme all text
        holder.tvName.setTextColor(primary)
        holder.tvUsername.setTextColor(textSecondary)
        holder.tvBio.setTextColor(textSecondary)
        holder.tvTheirSkillsHeader.setTextColor(textSecondary)
        holder.tvTeach.setTextColor(primary)
        holder.tvLearn.setTextColor(textSecondary)
        holder.tvYourSkillsHeader.setTextColor(textSecondary)
        holder.tvYourTeach.setTextColor(primary)
        holder.tvYourLearn.setTextColor(textSecondary)

        if (showAcceptDecline) {
            // Incoming — show requester's info
            holder.avatarView.setName(request.fromName)
            holder.tvName.text = request.fromName
            holder.tvUsername.text = "@${request.fromUsername}"
            holder.tvBio.text = request.fromBio.ifEmpty { "No bio" }

            holder.tvTheirSkillsHeader.text = "${request.fromName}'s Skills"
            holder.tvTeach.text = "Teaches: ${request.fromSkillsToTeach
                .joinToString(", ").ifEmpty { "Nothing listed" }}"
            holder.tvLearn.text = "Wants to learn: ${request.fromSkillsToLearn
                .joinToString(", ").ifEmpty { "Nothing listed" }}"

            holder.tvYourSkillsHeader.text = "Your Skills"
            holder.tvYourTeach.text = "You teach: ${request.toSkillsToTeach
                .joinToString(", ").ifEmpty { "Nothing listed" }}"
            holder.tvYourLearn.text = "You want to learn: ${request.toSkillsToLearn
                .joinToString(", ").ifEmpty { "Nothing listed" }}"

            holder.btnAccept.visibility = View.VISIBLE
            holder.btnDecline.visibility = View.VISIBLE
            holder.btnCancel.visibility = View.GONE
            holder.btnAccept.setOnClickListener { onAccept(request) }
            holder.btnDecline.setOnClickListener { onDecline(request) }
        } else {
            // Outgoing — show recipient info
            holder.avatarView.setName(request.toName)
            holder.tvName.text = request.toName
            holder.tvUsername.text = "Awaiting response"
            holder.tvBio.text = "Waiting for ${request.toName} to respond"

            holder.tvTheirSkillsHeader.text = "Your Skills"
            holder.tvTeach.text = "You teach: ${request.fromSkillsToTeach
                .joinToString(", ").ifEmpty { "Nothing listed" }}"
            holder.tvLearn.text = "You want to learn: ${request.fromSkillsToLearn
                .joinToString(", ").ifEmpty { "Nothing listed" }}"

            holder.tvYourSkillsHeader.text = "${request.toName}'s Skills"
            holder.tvYourTeach.text = "Teaches: ${request.toSkillsToTeach
                .joinToString(", ").ifEmpty { "Nothing listed" }}"
            holder.tvYourLearn.text = "Wants to learn: ${request.toSkillsToLearn
                .joinToString(", ").ifEmpty { "Nothing listed" }}"

            holder.btnAccept.visibility = View.GONE
            holder.btnDecline.visibility = View.GONE
            holder.btnCancel.visibility = View.VISIBLE
            holder.btnCancel.setOnClickListener { onDecline(request) }
        }
    }

    override fun getItemCount() = requests.size
}