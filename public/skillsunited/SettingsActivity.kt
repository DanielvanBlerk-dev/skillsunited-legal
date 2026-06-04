package com.dkvb.skillswap

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.skydoves.colorpickerview.ColorPickerView
import com.skydoves.colorpickerview.sliders.BrightnessSlideBar

class SettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setupPresets()
        setupColorPickers()
        setupButtons()

        // Terms accepted date
        val acceptedDate = ThemeManager.getTermsAcceptedDate(this)
        findViewById<TextView>(R.id.tvTermsAcceptedDate).text =
            "Terms accepted: $acceptedDate"

// View terms
        findViewById<Button>(R.id.btnViewTerms).setOnClickListener {
            TermsDialogHelper.showTermsDialog(this, onAccepted = {}, onDeclined = {})
        }

// View privacy policy
        findViewById<Button>(R.id.btnViewPrivacy).setOnClickListener {
            android.app.AlertDialog.Builder(this)
                .setTitle("Privacy Policy")
                .setMessage("Our full Privacy Policy is available at:\n\n${TermsDialogHelper.PRIVACY_URL}\n\nWe collect your name, username, email, bio, skills, and messages. We store data using Google Firebase. We do not sell your data. You can request deletion at any time.")
                .setPositiveButton("View Full Policy") { _, _ ->
                    try {
                        startActivity(android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse(TermsDialogHelper.PRIVACY_URL)
                        ))
                    } catch (e: Exception) { }
                }
                .setNegativeButton("Close", null)
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        applyThemeToRoot()
        applyStatusBarColor()
    }

    override fun setupSettingsButton() {
        // Don't show settings button on the settings page
    }

    private fun setupPresets() {
        val rvPresets = findViewById<RecyclerView>(R.id.rvPresets)
        rvPresets.layoutManager = GridLayoutManager(this, 2)
        rvPresets.adapter = PresetAdapter(ThemeManager.presets) { theme ->
            ThemeManager.applyPreset(theme)
            Toast.makeText(this, "${theme.name} applied!", Toast.LENGTH_SHORT).show()
            recreate()
        }
    }

    private fun setupColorPickers() {
        val colorSettings = listOf(
            Triple(R.id.rowPrimary, "Primary color", ThemeManager.getPrimary()),
            Triple(R.id.rowSecondary, "Secondary color", ThemeManager.getSecondary()),
            Triple(R.id.rowBackground, "Background", ThemeManager.getBackground()),
            Triple(R.id.rowSurface, "Surface / Cards", ThemeManager.getSurface()),
            Triple(R.id.rowTextPrimary, "Primary text", ThemeManager.getTextPrimary()),
            Triple(R.id.rowTextSecondary, "Secondary text", ThemeManager.getTextSecondary()),
            Triple(R.id.rowBubbleSent, "Sent bubble", ThemeManager.getBubbleSent()),
            Triple(R.id.rowBubbleReceived, "Received bubble", ThemeManager.getBubbleReceived())
        )

        colorSettings.forEach { (rowId, label, currentColor) ->
            setupColorRow(rowId, label, currentColor)
        }
    }

    private fun setupColorRow(rowId: Int, label: String, currentColor: String) {
        val row = findViewById<android.view.ViewGroup>(rowId)
        val tvLabel = row.findViewById<TextView>(R.id.tvColorLabel)
        val colorPreview = row.findViewById<android.view.View>(R.id.colorPreview)
        val btnPick = row.findViewById<Button>(R.id.btnPickColor)

        tvLabel.text = label
        tvLabel.setTextColor(ThemeManager.parseColor(ThemeManager.getTextPrimary()))
        updatePreviewCircle(colorPreview, currentColor)

        btnPick.setOnClickListener {
            showColorPickerDialog(label, currentColor) { selectedColor ->
                when (rowId) {
                    R.id.rowPrimary -> ThemeManager.setPrimary(selectedColor)
                    R.id.rowSecondary -> ThemeManager.setSecondary(selectedColor)
                    R.id.rowBackground -> ThemeManager.setBackground(selectedColor)
                    R.id.rowSurface -> ThemeManager.setSurface(selectedColor)
                    R.id.rowTextPrimary -> ThemeManager.setTextPrimary(selectedColor)
                    R.id.rowTextSecondary -> ThemeManager.setTextSecondary(selectedColor)
                    R.id.rowBubbleSent -> ThemeManager.setBubbleSent(selectedColor)
                    R.id.rowBubbleReceived -> ThemeManager.setBubbleReceived(selectedColor)
                }
                updatePreviewCircle(colorPreview, selectedColor)
                Toast.makeText(this, "$label updated!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showColorPickerDialog(
        title: String,
        currentColor: String,
        onColorSelected: (String) -> Unit
    ) {
        val dialogView = LayoutInflater.from(this)
            .inflate(R.layout.dialog_color_picker, null)

        val colorPickerView = dialogView.findViewById<ColorPickerView>(R.id.colorPickerView)
        val brightnessSlide = dialogView.findViewById<BrightnessSlideBar>(R.id.brightnessSlide)
        val previewBox = dialogView.findViewById<android.view.View>(R.id.previewBox)

        colorPickerView.attachBrightnessSlider(brightnessSlide)

        // Set initial color
        try {
            colorPickerView.setInitialColor(android.graphics.Color.parseColor(currentColor))
        } catch (e: Exception) {
            colorPickerView.setInitialColor(android.graphics.Color.BLUE)
        }

        // Live preview
        colorPickerView.setColorListener(
            com.skydoves.colorpickerview.listeners.ColorEnvelopeListener { envelope, _ ->
                previewBox.setBackgroundColor(envelope.color)
            }
        )

        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(dialogView)
            .setPositiveButton("Apply") { _, _ ->
                val hex = "#%06X".format(0xFFFFFF and colorPickerView.color)
                onColorSelected(hex)
                recreate()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updatePreviewCircle(view: android.view.View, hex: String) {
        try {
            val drawable = android.graphics.drawable.GradientDrawable()
            drawable.shape = android.graphics.drawable.GradientDrawable.OVAL
            drawable.setColor(android.graphics.Color.parseColor(hex))
            view.background = drawable
        } catch (e: Exception) { }
    }

    private fun setupButtons() {
        findViewById<Button>(R.id.btnReset).setOnClickListener {
            ThemeManager.resetToDefaults()
            Toast.makeText(this, "Reset to defaults", Toast.LENGTH_SHORT).show()
            recreate()
        }
        findViewById<Button>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }
}