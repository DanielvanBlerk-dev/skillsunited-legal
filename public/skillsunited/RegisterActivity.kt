package com.dkvb.skillswap

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class RegisterActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        applyTheme()

        findViewById<Button>(R.id.btnRegister).setOnClickListener { registerUser() }
        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        applyTheme()
    }

    override fun setupInboxButton() {
        // Don't show inbox button on create account page
    }

    private fun validateInput(
        name: String,
        username: String,
        email: String,
        password: String
    ): String? {
        if (name.isEmpty()) return "Name is required"
        if (name.length > 50) return "Name must be under 50 characters"
        if (name.any { it.isDigit() }) return "Name cannot contain numbers"

        if (username.isEmpty()) return "Username is required"
        if (username.length < 3) return "Username must be at least 3 characters"
        if (username.length > 30) return "Username must be under 30 characters"
        if (!username.matches(Regex("^[a-z0-9._]+$")))
            return "Username can only contain lowercase letters, numbers, dots and underscores"
        if (username.startsWith(".") || username.endsWith("."))
            return "Username cannot start or end with a dot"
        if (username.contains(".."))
            return "Username cannot contain consecutive dots"

        if (email.isEmpty()) return "Email is required"
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches())
            return "Please enter a valid email address"
        if (email.length > 100) return "Email is too long"

        if (password.isEmpty()) return "Password is required"
        if (password.length < 8) return "Password must be at least 8 characters"
        if (password.length > 128) return "Password is too long"
        if (!password.any { it.isUpperCase() })
            return "Password must contain an uppercase letter"
        if (!password.any { it.isLowerCase() })
            return "Password must contain a lowercase letter"
        if (!password.any { it.isDigit() })
            return "Password must contain a number"
        if (!password.any { !it.isLetterOrDigit() })
            return "Password must contain a special character (e.g. !@#\$%)"
        return null
    }

    private fun proceedWithRegistration(
        name: String, username: String, email: String, password: String
    ) {
        findViewById<Button>(R.id.btnRegister).isEnabled = false
        Toast.makeText(this, "Checking username...", Toast.LENGTH_SHORT).show()

        Firebase.firestore.collection("usernames")
            .document(username)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    Toast.makeText(this, "Username \"$username\" is already taken",
                        Toast.LENGTH_LONG).show()
                    findViewById<Button>(R.id.btnRegister).isEnabled = true
                } else {
                    createAccount(name, username, email, password)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error checking username: ${e.message}",
                    Toast.LENGTH_LONG).show()
                findViewById<Button>(R.id.btnRegister).isEnabled = true
            }
    }

    private fun registerUser() {
        val name = findViewById<TextInputEditText>(R.id.etName).text.toString().trim()
        val username = findViewById<TextInputEditText>(R.id.etUsername).text.toString()
            .trim().lowercase()
        val email = findViewById<TextInputEditText>(R.id.etEmail).text.toString().trim()
        val password = findViewById<TextInputEditText>(R.id.etPassword).text.toString()

        val error = validateInput(name, username, email, password)
        if (error != null) {
            Toast.makeText(this, error, Toast.LENGTH_LONG).show()
            return
        }

        // Always show terms on registration — must explicitly accept
        TermsDialogHelper.showTermsDialog(
            context = this,
            onAccepted = { proceedWithRegistration(name, username, email, password) },
            onDeclined = {
                Toast.makeText(this,
                    "You must accept the Terms of Service to create an account",
                    Toast.LENGTH_LONG).show()
            }
        )

        findViewById<Button>(R.id.btnRegister).isEnabled = false
        Toast.makeText(this, "Checking username...", Toast.LENGTH_SHORT).show()

        Firebase.firestore.collection("usernames")
            .document(username)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    Toast.makeText(this, "Username \"$username\" is already taken",
                        Toast.LENGTH_LONG).show()
                    findViewById<Button>(R.id.btnRegister).isEnabled = true
                } else {
                    createAccount(name, username, email, password)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error checking username: ${e.message}",
                    Toast.LENGTH_LONG).show()
                findViewById<Button>(R.id.btnRegister).isEnabled = true
            }
    }

    private fun createAccount(
        name: String,
        username: String,
        email: String,
        password: String
    ) {
        Firebase.auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user!!.uid
                val user = hashMapOf(
                    "uid" to uid,
                    "name" to name,
                    "username" to username,
                    "email" to email,
                    "skillsToTeach" to emptyList<String>(),
                    "skillsToLearn" to emptyList<String>(),
                    "bio" to "",
                    "createdAt" to System.currentTimeMillis()
                )

                val db = Firebase.firestore
                val batch = db.batch()

                batch.set(db.collection("users").document(uid), user)
                batch.set(
                    db.collection("usernames").document(username),
                    hashMapOf("uid" to uid)
                )

                batch.commit()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Account created!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                    .addOnFailureListener { e ->
                        result.user?.delete()
                        Toast.makeText(this, "Failed to save profile: ${e.message}",
                            Toast.LENGTH_LONG).show()
                        findViewById<Button>(R.id.btnRegister).isEnabled = true
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Registration failed: ${e.message}",
                    Toast.LENGTH_LONG).show()
                findViewById<Button>(R.id.btnRegister).isEnabled = true
            }
    }

    private fun applyTheme() {
        val bg = ThemeManager.parseColor(ThemeManager.getBackground())
        val surface = ThemeManager.parseColor(ThemeManager.getSurface())
        val textPrimary = ThemeManager.parseColor(ThemeManager.getTextPrimary())
        val textSecondary = ThemeManager.parseColor(ThemeManager.getTextSecondary())
        val primary = ThemeManager.parseColor(ThemeManager.getPrimary())

        findViewById<android.widget.LinearLayout>(R.id.registerRoot)
            ?.setBackgroundColor(bg)
        findViewById<android.widget.TextView>(R.id.tvRegisterTitle)
            ?.setTextColor(primary)
        findViewById<android.widget.TextView>(R.id.tvRegisterSubtitle)
            ?.setTextColor(textSecondary)

        listOf(R.id.etName, R.id.etUsername, R.id.etEmail, R.id.etPassword)
            .forEach { fieldId ->
                val field = findViewById<TextInputEditText>(fieldId) ?: return@forEach
                val layout = field.parent.parent
                        as? com.google.android.material.textfield.TextInputLayout
                    ?: return@forEach
                field.setTextColor(textPrimary)
                field.setHintTextColor(textSecondary)
                layout.boxBackgroundColor = surface
                layout.setBoxStrokeColor(primary)
                layout.defaultHintTextColor =
                    android.content.res.ColorStateList.valueOf(textSecondary)
                layout.hintTextColor =
                    android.content.res.ColorStateList.valueOf(textSecondary)
            }

        findViewById<Button>(R.id.btnRegister)?.apply {
            setBackgroundColor(primary)
            setTextColor(ThemeManager.parseColor(ThemeManager.getButtonText()))
        }
        findViewById<Button>(R.id.btnBack)?.apply {
            setBackgroundColor(surface)
            setTextColor(primary)
        }
    }
}