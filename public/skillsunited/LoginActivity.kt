package com.dkvb.skillswap

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthMultiFactorException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        applyTheme()

        auth = Firebase.auth

        if (auth.currentUser != null) {
            goToMain()
            return
        }

        findViewById<Button>(R.id.btnLogin).setOnClickListener {
            loginWithEmail()
        }

        findViewById<Button>(R.id.btnRegister).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Informational only — user is not yet signed in so declining has no consequence
        findViewById<TextView>(R.id.tvTermsLink).setOnClickListener {
            TermsDialogHelper.showTermsDialog(this, onAccepted = {}, onDeclined = {})
        }
    }

    override fun onResume() {
        super.onResume()
        applyTheme()
    }

    override fun setupInboxButton() {
        // Don't show inbox button on login page
    }

    private fun applyTheme() {
        val bg = ThemeManager.parseColor(ThemeManager.getBackground())
        val surface = ThemeManager.parseColor(ThemeManager.getSurface())
        val textPrimary = ThemeManager.parseColor(ThemeManager.getTextPrimary())
        val textSecondary = ThemeManager.parseColor(ThemeManager.getTextSecondary())
        val primary = ThemeManager.parseColor(ThemeManager.getPrimary())

        findViewById<android.widget.LinearLayout>(R.id.loginRoot)
            ?.setBackgroundColor(bg)
        findViewById<android.widget.TextView>(R.id.tvAppName)
            ?.setTextColor(primary)
        findViewById<android.widget.TextView>(R.id.tvTagline)
            ?.setTextColor(textSecondary)

        listOf(R.id.etEmail, R.id.etPassword).forEach { fieldId ->
            val field = findViewById<TextInputEditText>(fieldId) ?: return@forEach
            val layout = field.parent.parent as? TextInputLayout ?: return@forEach
            field.setTextColor(textPrimary)
            field.setHintTextColor(textSecondary)
            layout.boxBackgroundColor = surface
            layout.setBoxStrokeColor(primary)
            layout.defaultHintTextColor = ColorStateList.valueOf(textSecondary)
            layout.hintTextColor = ColorStateList.valueOf(textSecondary)
            layout.setEndIconTintList(ColorStateList.valueOf(textSecondary))
        }

        findViewById<Button>(R.id.btnLogin)?.apply {
            setBackgroundColor(primary)
            setTextColor(ThemeManager.parseColor(ThemeManager.getButtonText()))
        }
        findViewById<Button>(R.id.btnRegister)?.apply {
            setBackgroundColor(surface)
            setTextColor(primary)
        }
    }

    private fun loginWithEmail() {
        val email = findViewById<TextInputEditText>(R.id.etEmail).text.toString().trim()
        val password = findViewById<TextInputEditText>(R.id.etPassword).text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        TermsDialogHelper.showTermsIfNotAccepted(
            context = this,
            onAccepted = { performLogin(email, password) },
            onDeclined = {
                // Clear the password field and block login
                findViewById<TextInputEditText>(R.id.etPassword).text?.clear()
                Toast.makeText(
                    this,
                    "You must accept the Terms of Service and Privacy Policy to use Skills United.",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    private fun performLogin(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { goToMain() }
            .addOnFailureListener { e ->
                if (e is FirebaseAuthMultiFactorException) {
                    val intent = Intent(this, MfaSignInActivity::class.java)
                    intent.putExtra("resolver", e.resolver)
                    startActivity(intent)
                } else if (e.message?.contains("PASSWORD_DOES_NOT_MEET_REQUIREMENTS") == true) {
                    Toast.makeText(this,
                        "Your password needs to be updated. Please reset it.",
                        Toast.LENGTH_LONG).show()
                    if (email.isNotEmpty()) {
                        Firebase.auth.sendPasswordResetEmail(email)
                            .addOnSuccessListener {
                                Toast.makeText(this,
                                    "Password reset email sent to $email",
                                    Toast.LENGTH_LONG).show()
                            }
                    }
                } else {
                    Toast.makeText(this, "Login failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}