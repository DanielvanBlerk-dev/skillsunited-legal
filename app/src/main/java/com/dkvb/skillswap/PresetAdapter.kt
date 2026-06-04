package com.dkvb.skillswap

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PresetAdapter(
    private val presets: List<ThemeManager.Theme>,
    private val onClick: (ThemeManager.Theme) -> Unit
) : RecyclerView.Adapter<PresetAdapter.PresetViewHolder>() {

    class PresetViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val colorCircle: View = view.findViewById(R.id.colorCircle)
        val tvName: TextView = view.findViewById(R.id.tvPresetName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PresetViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_preset, parent, false)
        return PresetViewHolder(view)
    }

    override fun onBindViewHolder(holder: PresetViewHolder, position: Int) {
        val theme = presets[position]

        // Name text uses current theme text color so it's visible in dark mode
        holder.tvName.text = theme.name
        holder.tvName.setTextColor(ThemeManager.parseColor(ThemeManager.getTextPrimary()))

        val drawable = GradientDrawable()
        drawable.shape = GradientDrawable.OVAL
        drawable.setColor(Color.parseColor(theme.primary))
        holder.colorCircle.background = drawable

        holder.itemView.setOnClickListener { onClick(theme) }
    }

    override fun getItemCount() = presets.size
}