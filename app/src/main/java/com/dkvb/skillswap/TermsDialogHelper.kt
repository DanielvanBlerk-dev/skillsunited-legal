package com.dkvb.skillswap

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

object TermsDialogHelper {

    const val TERMS_URL = "https://danielvanblerk-dev.github.io/skillsunited-legal/terms.html"
    const val PRIVACY_URL = "https://danielvanblerk-dev.github.io/skillsunited-legal/privacy.html"

    fun showTermsDialog(
        context: Context,
        onAccepted: () -> Unit,
        onDeclined: () -> Unit = {}
    ) {
        val surface = ThemeManager.parseColor(ThemeManager.getSurface())
        val textPrimary = ThemeManager.parseColor(ThemeManager.getTextPrimary())
        val textSecondary = ThemeManager.parseColor(ThemeManager.getTextSecondary())
        val primary = ThemeManager.parseColor(ThemeManager.getPrimary())

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
            setBackgroundColor(surface)
        }

        // Scrollable terms summary
        val scrollView = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                400
            )
            setBackgroundColor(surface)
        }

        val summaryText = TextView(context).apply {
            text = buildTermsSummary()
            textSize = 13f
            setPadding(0, 0, 0, 16)
            setTextColor(textPrimary)
        }
        scrollView.addView(summaryText)
        layout.addView(scrollView)

        // Links row
        val linksLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, 8, 0, 8)
            setBackgroundColor(surface)
        }

        val termsLink = TextView(context).apply {
            text = "Read full Terms of Service"
            textSize = 13f
            setTextColor(primary)
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
            setOnClickListener { openUrl(context, TERMS_URL) }
        }

        val privacyLink = TextView(context).apply {
            text = "Read Privacy Policy"
            textSize = 13f
            setTextColor(primary)
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
            setOnClickListener { openUrl(context, PRIVACY_URL) }
        }

        linksLayout.addView(termsLink)
        linksLayout.addView(privacyLink)
        layout.addView(linksLayout)

        // Checkbox
        val checkBox = CheckBox(context).apply {
            text = "I have read and agree to the Terms of Service and Privacy Policy"
            textSize = 13f
            setTextColor(textPrimary)
            buttonTintList = ColorStateList.valueOf(primary)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 16 }
        }
        layout.addView(checkBox)

        val dialog = AlertDialog.Builder(context)
            .setTitle("Terms of Service & Privacy Policy")
            .setView(layout)
            .setCancelable(false)
            .setPositiveButton("Accept") { _, _ ->
                if (checkBox.isChecked) {
                    ThemeManager.setTermsAcceptedWithDate(context)
                    onAccepted()
                }
            }
            .setNegativeButton("Decline") { _, _ ->
                onDeclined()
            }
            .create()

        dialog.show()

        // Apply theme to the dialog window background and title
        dialog.window?.setBackgroundDrawable(ColorDrawable(surface))

        // Title text colour
        val titleId = context.resources.getIdentifier("alertTitle", "id", "android")
        dialog.findViewById<TextView>(titleId)?.setTextColor(textPrimary)

        // Button colours
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(primary)
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(textSecondary)

        // Disable accept button until checkbox is ticked
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = false
        checkBox.setOnCheckedChangeListener { _, isChecked ->
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = isChecked
        }
    }

    fun showTermsIfNotAccepted(
        context: Context,
        onAccepted: () -> Unit,
        onDeclined: () -> Unit = {}
    ) {
        if (ThemeManager.hasAcceptedTerms(context)) {
            onAccepted()
        } else {
            showTermsDialog(context, onAccepted, onDeclined)
        }
    }

    private fun buildTermsSummary(): String {
        return """SUMMARY OF KEY TERMS

By using Skills United you agree to the following:

WHAT WE PROVIDE
Skills United is a technology platform that connects people who want to exchange skills. We are a neutral intermediary — we do not verify users, their qualifications, or their intentions.

YOUR RESPONSIBILITIES
- You are solely responsible for your own safety and conduct
- You must be 18 years of age or older
- You must not use the App for any illegal purpose
- You must not harass, threaten, or harm other users
- You interact with other users entirely at your own risk

YOUR PRIVACY
- We collect your name, username, email, bio, skills, and messages
- Your data is stored using Google Firebase
- We do not sell your personal information
- Messages are stored on our servers and may be reviewed if reported
- You can request deletion of your data at any time

OUR LIABILITY
- We are not responsible for the conduct of other users
- We do not guarantee the safety of in-person meetings
- Our liability is limited to the maximum extent permitted by law

SAFETY NOTICE
Always meet other users in public places. Never share sensitive personal or financial information through the App.

For full details, please read our complete Terms of Service and Privacy Policy using the links below."""
    }

    private fun openUrl(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            AlertDialog.Builder(context)
                .setTitle("Could not open link")
                .setMessage("Please visit $url in your browser.")
                .setPositiveButton("OK", null)
                .show()
        }
    }
}