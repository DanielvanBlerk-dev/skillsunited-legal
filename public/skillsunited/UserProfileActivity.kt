package com.dkvb.skillswap

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class UserProfileActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        val userId = intent.getStringExtra("userId") ?: return
        val currentUid = Firebase.auth.currentUser?.uid ?: return

        Firebase.firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { doc ->
                val user = doc.toObject(User::class.java) ?: return@addOnSuccessListener

                findViewById<TextView>(R.id.tvName).text = user.name
                findViewById<TextView>(R.id.tvBio).text = user.bio.ifEmpty { "No bio yet" }
                findViewById<TextView>(R.id.tvTeach).text =
                    "Teaches: ${user.skillsToTeach.joinToString(", ").ifEmpty { "Nothing yet" }}"
                findViewById<TextView>(R.id.tvLearn).text =
                    "Wants to learn: ${user.skillsToLearn.joinToString(", ").ifEmpty { "Nothing yet" }}"
                findViewById<AvatarView>(R.id.avatarView).setName(user.name)

                val messageBtn = findViewById<Button>(R.id.btnMessage)

                checkExistingRequest(currentUid, userId) { status ->
                    when (status) {
                        "pending" -> {
                            messageBtn.text = "Request Sent"
                            messageBtn.isEnabled = false
                        }
                        "accepted" -> {
                            messageBtn.text = "Open Chat"
                            messageBtn.isEnabled = true
                            messageBtn.setOnClickListener {
                                openChatIfEnabled(userId, user.name)
                            }
                        }
                        else -> {
                            messageBtn.text = "Request Chat"
                            messageBtn.isEnabled = true
                            messageBtn.setOnClickListener {
                                sendMatchRequest(userId, user)
                            }
                        }
                    }
                }

                findViewById<Button>(R.id.btnReport).setOnClickListener {
                    reportUser(userId, user.name)
                }

                findViewById<TextView>(R.id.tvTermsLink).setOnClickListener {
                    TermsDialogHelper.showTermsDialog(this, onAccepted = {}, onDeclined = {})
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkExistingRequest(
        currentUid: String,
        userId: String,
        onResult: (String?) -> Unit
    ) {
        Firebase.firestore.collection("matchRequests")
            .whereEqualTo("fromUid", currentUid)
            .whereEqualTo("toUid", userId)
            .get()
            .addOnSuccessListener { docs ->
                val latest = docs.documents.firstOrNull()
                onResult(latest?.getString("status"))
            }
            .addOnFailureListener { onResult(null) }
    }

    private fun sendMatchRequest(userId: String, user: User) {
        val currentUid = Firebase.auth.currentUser?.uid ?: return

        checkExistingRequest(currentUid, userId) { existingStatus ->
            when (existingStatus) {
                "pending" -> {
                    Toast.makeText(this,
                        "You already sent a chat request to ${user.name}",
                        Toast.LENGTH_LONG).show()
                }
                "accepted" -> {
                    openChatIfEnabled(userId, user.name)
                }
                else -> {
                    // Check if they already sent us a request
                    Firebase.firestore.collection("matchRequests")
                        .whereEqualTo("fromUid", userId)
                        .whereEqualTo("toUid", currentUid)
                        .whereEqualTo("status", "pending")
                        .get()
                        .addOnSuccessListener { docs ->
                            if (!docs.isEmpty) {
                                // They already requested us — accept and open chat
                                val docId = docs.documents.first().id
                                Firebase.firestore.collection("matchRequests")
                                    .document(docId)
                                    .update("status", "accepted")
                                    .addOnSuccessListener {
                                        openChatIfEnabled(userId, user.name)
                                    }
                            } else {
                                // No existing request — fetch current user profile
                                // and create new request
                                Firebase.firestore.collection("users")
                                    .document(currentUid)
                                    .get()
                                    .addOnSuccessListener { currentUserDoc ->
                                        val currentUser = currentUserDoc
                                            .toObject(User::class.java)
                                            ?: return@addOnSuccessListener

                                        val request = hashMapOf(
                                            "fromUid" to currentUid,
                                            "fromName" to currentUser.name,
                                            "fromUsername" to currentUser.username,
                                            "fromBio" to currentUser.bio,
                                            "fromSkillsToTeach" to currentUser.skillsToTeach,
                                            "fromSkillsToLearn" to currentUser.skillsToLearn,
                                            "toUid" to userId,
                                            "toName" to user.name,
                                            "toSkillsToTeach" to user.skillsToTeach,    // ← added
                                            "toSkillsToLearn" to user.skillsToLearn,    // ← added
                                            "status" to "pending",
                                            "timestamp" to System.currentTimeMillis()
                                        )

                                        Firebase.firestore.collection("matchRequests")
                                            .add(request)
                                            .addOnSuccessListener {
                                                Toast.makeText(this,
                                                    "Chat request sent to ${user.name}!",
                                                    Toast.LENGTH_LONG).show()
                                                runOnUiThread {
                                                    findViewById<Button>(R.id.btnMessage)
                                                        ?.apply {
                                                            text = "Request Sent"
                                                            isEnabled = false
                                                        }
                                                }
                                            }
                                            .addOnFailureListener {
                                                Toast.makeText(this,
                                                    "Failed to send request",
                                                    Toast.LENGTH_SHORT).show()
                                            }
                                    }
                            }
                        }
                }
            }
        }
    }

    private fun reportUser(userId: String, userName: String) {
        AlertDialog.Builder(this)
            .setTitle("Report $userName")
            .setItems(arrayOf(
                "Inappropriate content",
                "Spam or fake account",
                "Harassment",
                "Other"
            )) { _, which ->
                val reasons = listOf(
                    "Inappropriate content",
                    "Spam or fake account",
                    "Harassment",
                    "Other"
                )
                val selectedReason = reasons[which]
                val reporterUid = Firebase.auth.currentUser?.uid ?: ""
                val timestampMillis = System.currentTimeMillis()
                val sdf = java.text.SimpleDateFormat(
                    "dd MMM yyyy HH:mm:ss", java.util.Locale.getDefault()
                )
                val timestampString = sdf.format(java.util.Date(timestampMillis))

                val report = hashMapOf(
                    "reportedUid" to userId,
                    "reportedName" to userName,
                    "reporterUid" to reporterUid,
                    "reason" to selectedReason,
                    "timestamp" to timestampMillis
                )

                Firebase.firestore.collection("reports")
                    .add(report)
                    .addOnSuccessListener {
                        Toast.makeText(this,
                            "Report submitted. Thank you.",
                            Toast.LENGTH_SHORT).show()

                        EmailService.sendReportEmail(
                            reportedName = userName,
                            reportedUid = userId,
                            reporterUid = reporterUid,
                            reason = selectedReason,
                            timestamp = timestampString,
                            onSuccess = {
                                android.util.Log.d("Report", "Email notification sent")
                            },
                            onFailure = { error ->
                                android.util.Log.e("Report", "Email failed: $error")
                            }
                        )
                    }
                    .addOnFailureListener {
                        Toast.makeText(this,
                            "Failed to submit report. Please try again.",
                            Toast.LENGTH_SHORT).show()
                    }
            }
            .show()
    }
}