package com.dkvb.skillswap

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SkillAdapter(
    private val skills: MutableList<String>,
    private val onChanged: () -> Unit
) : RecyclerView.Adapter<SkillAdapter.SkillViewHolder>() {

    class SkillViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSkill: TextView = view.findViewById(R.id.tvSkill)
        val btnEdit: Button = view.findViewById(R.id.btnEdit)
        val btnDelete: Button = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SkillViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_skill, parent, false)
        return SkillViewHolder(view)
    }

    override fun onBindViewHolder(holder: SkillViewHolder, position: Int) {
        val textPrimary = ThemeManager.parseColor(ThemeManager.getTextPrimary())
        val surface = ThemeManager.parseColor(ThemeManager.getSurface())
        val primary = ThemeManager.parseColor(ThemeManager.getPrimary())

        // Apply theme to item background
        holder.itemView.setBackgroundColor(surface)

        // Apply theme to skill text
        holder.tvSkill.text = skills[position]
        holder.tvSkill.setTextColor(textPrimary)

        // Apply theme to buttons
        holder.btnEdit.apply {
            setTextColor(primary)
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }
        holder.btnDelete.apply {
            setTextColor(ThemeManager.parseColor("#CC0000"))
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }

        holder.btnEdit.setOnClickListener {
            val context = holder.itemView.context
            val input = EditText(context)
            input.setText(skills[position])
            input.setSelection(input.text.length)
            input.setTextColor(textPrimary)

            AlertDialog.Builder(context)
                .setTitle("Edit skill")
                .setView(input)
                .setPositiveButton("Save") { _, _ ->
                    val edited = input.text.toString().trim()
                    if (edited.isNotEmpty()) {
                        skills[position] = edited
                        notifyItemChanged(position)
                        onChanged()  // ← this must be here
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        holder.btnDelete.setOnClickListener {
            val context = holder.itemView.context
            AlertDialog.Builder(context)
                .setTitle("Remove skill")
                .setMessage("Remove \"${skills[position]}\"?")
                .setPositiveButton("Remove") { _, _ ->
                    skills.removeAt(position)
                    notifyItemRemoved(position)
                    notifyItemRangeChanged(position, skills.size)
                    onChanged()  // ← this must be here
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun getItemCount() = skills.size
}