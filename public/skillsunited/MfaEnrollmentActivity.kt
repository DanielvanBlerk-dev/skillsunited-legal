package com.dkvb.skillswap

import android.os.Bundle
import android.widget.*
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.util.concurrent.TimeUnit

class MfaEnrollmentActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mfa_enrollment)

        auth = Firebase.auth
        applyTheme()
        setupButtons()
    }

    private fun setupButtons() {
        findViewById<Button>(R.id.btnSendCode).setOnClickListener {
            val phone = findViewById<EditText>(R.id.etPhoneNumber).text.toString().trim()
            if (validatePhone(phone)) sendVerificationCode(phone)
        }

        findViewById<Button>(R.id.btnVerify).setOnClickListener {
            val code = findViewById<EditText>(R.id.etVerificationCode).text.toString().trim()
            if (code.length == 6) verifyCode(code)
            else Toast.makeText(this, "Enter the 6-digit code", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnSkip).setOnClickListener {
            finish()
        }

        // Hide verify section initially
        findViewById<LinearLayout>(R.id.layoutVerify).visibility = android.view.View.GONE
    }

    private fun validatePhone(phone: String): Boolean {
        if (phone.isEmpty()) {
            Toast.makeText(this, "Please enter a phone number", Toast.LENGTH_SHORT).show()
            return false
        }
        if (!phone.startsWith("+")) {
            Toast.makeText(this, "Include country code e.g. +61412345678", Toast.LENGTH_SHORT).show()
            return false
        }
        if (phone.length < 10 || phone.length > 15) {
            Toast.makeText(this, "Please enter a valid phone number", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun sendVerificationCode(phoneNumber: String) {
        val user = auth.currentUser ?: return
        val btn = findViewById<Button>(R.id.btnSendCode)
        btn.isEnabled = false
        btn.text = "Sending..."

        // Get multi-factor session
        user.multiFactor.session
            .addOnSuccessListener { session ->
                val phoneOptions = PhoneAuthOptions.newBuilder()
                    .setPhoneNumber(phoneNumber)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(this)
                    .setMultiFactorSession(session)
                    .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                            // Auto-verification on some devices
                            enrollWithCredential(
                                PhoneMultiFactorGenerator.getAssertion(credential)
                            )
                        }

                        override fun onVerificationFailed(e: FirebaseException) {
                            Toast.makeText(
                                this@MfaEnrollmentActivity,
                                "Verification failed: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                            btn.isEnabled = true
                            btn.text = "Send Code"
                        }

                        override fun onCodeSent(
                            vId: String,
                            token: PhoneAuthProvider.ForceResendingToken
                        ) {
                            verificationId = vId
                            resendToken = token
                            Toast.makeText(
                                this@MfaEnrollmentActivity,
                                "Code sent to $phoneNumber",
                                Toast.LENGTH_SHORT
                            ).show()
                            btn.isEnabled = true
                            btn.text = "Resend Code"
                            // Show verify section
                            findViewById<LinearLayout>(R.id.layoutVerify).visibility =
                                android.view.View.VISIBLE
                        }
                    })
                    .build()

                PhoneAuthProvider.verifyPhoneNumber(phoneOptions)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                btn.isEnabled = true
                btn.text = "Send Code"
            }
    }

    private fun verifyCode(code: String) {
        val vId = verificationId ?: return
        val btn = findViewById<Button>(R.id.btnVerify)
        btn.isEnabled = false
        btn.text = "Verifying..."

        val credential = PhoneAuthProvider.getCredential(vId, code)
        val assertion = PhoneMultiFactorGenerator.getAssertion(credential)
        enrollWithCredential(assertion)
    }

    private fun enrollWithCredential(assertion: MultiFactorAssertion) {
        val user = auth.currentUser ?: return
        val displayName = findViewById<EditText>(R.id.etPhoneNumber).text.toString().trim()

        user.multiFactor.enroll(assertion, displayName)
            .addOnSuccessListener {
                Toast.makeText(this, "Two-factor authentication enabled!", Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Enrollment failed: ${e.message}", Toast.LENGTH_LONG).show()
                findViewById<Button>(R.id.btnVerify).isEnabled = true
                findViewById<Button>(R.id.btnVerify).text = "Verify"
            }
    }

    private fun applyTheme() {
        val bg = ThemeManager.parseColor(ThemeManager.getBackground())
        val textPrimary = ThemeManager.parseColor(ThemeManager.getTextPrimary())
        val textSecondary = ThemeManager.parseColor(ThemeManager.getTextSecondary())
        val primary = ThemeManager.parseColor(ThemeManager.getPrimary())

        findViewById<ScrollView>(R.id.scrollView)?.setBackgroundColor(bg)
        findViewById<TextView>(R.id.tvTitle)?.setTextColor(primary)
        findViewById<TextView>(R.id.tvSubtitle)?.setTextColor(textSecondary)
        findViewById<TextView>(R.id.tvVerifyLabel)?.setTextColor(textPrimary)
        findViewById<EditText>(R.id.etPhoneNumber)?.apply {
            setTextColor(textPrimary)
            setHintTextColor(textSecondary)
        }
        findViewById<EditText>(R.id.etVerificationCode)?.apply {
            setTextColor(textPrimary)
            setHintTextColor(textSecondary)
        }
    }
}