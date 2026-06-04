package com.dkvb.skillswap

import android.content.Intent
import android.os.Bundle
import android.widget.*
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.util.concurrent.TimeUnit

class MfaSignInActivity : BaseActivity() {

    private var verificationId: String? = null
    private lateinit var resolver: MultiFactorResolver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mfa_signin)

        // Get the resolver passed from LoginActivity
        @Suppress("DEPRECATION")
        resolver = intent.getParcelableExtra("resolver")
            ?: run {
                finish()
                return
            }

        applyTheme()
        sendSmsCode()

        findViewById<Button>(R.id.btnVerify).setOnClickListener {
            val code = findViewById<EditText>(R.id.etCode).text.toString().trim()
            if (code.length == 6) verifyCode(code)
            else Toast.makeText(this, "Enter the 6-digit code", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendSmsCode() {
        val hint = resolver.hints.firstOrNull() as? PhoneMultiFactorInfo ?: return
        val phoneOptions = PhoneAuthOptions.newBuilder()
            .setMultiFactorHint(hint)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setMultiFactorSession(resolver.session)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    signInWithAssertion(PhoneMultiFactorGenerator.getAssertion(credential))
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Toast.makeText(
                        this@MfaSignInActivity,
                        "Verification failed: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }

                override fun onCodeSent(
                    vId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    verificationId = vId
                    val phone = hint.phoneNumber
                    findViewById<TextView>(R.id.tvPrompt).text =
                        "Enter the code sent to $phone"
                }
            })
            .build()

        PhoneAuthProvider.verifyPhoneNumber(phoneOptions)
    }

    private fun verifyCode(code: String) {
        val vId = verificationId ?: return
        val credential = PhoneAuthProvider.getCredential(vId, code)
        signInWithAssertion(PhoneMultiFactorGenerator.getAssertion(credential))
    }

    private fun signInWithAssertion(assertion: MultiFactorAssertion) {
        val btn = findViewById<Button>(R.id.btnVerify)
        btn.isEnabled = false
        btn.text = "Verifying..."

        resolver.resolveSignIn(assertion)
            .addOnSuccessListener {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Sign in failed: ${e.message}", Toast.LENGTH_LONG).show()
                btn.isEnabled = true
                btn.text = "Verify"
            }
    }

    private fun applyTheme() {
        val bg = ThemeManager.parseColor(ThemeManager.getBackground())
        val textPrimary = ThemeManager.parseColor(ThemeManager.getTextPrimary())
        val textSecondary = ThemeManager.parseColor(ThemeManager.getTextSecondary())
        val primary = ThemeManager.parseColor(ThemeManager.getPrimary())

        findViewById<android.widget.LinearLayout>(R.id.mfaRoot)?.setBackgroundColor(bg)
        findViewById<TextView>(R.id.tvTitle)?.setTextColor(primary)
        findViewById<TextView>(R.id.tvPrompt)?.setTextColor(textSecondary)
        findViewById<EditText>(R.id.etCode)?.apply {
            setTextColor(textPrimary)
            setHintTextColor(textSecondary)
        }
    }
}