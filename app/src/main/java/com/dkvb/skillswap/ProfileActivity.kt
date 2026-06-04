package com.dkvb.skillswap

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ProfileActivity : BaseActivity() {

    private val teachSkills = mutableListOf<String>()
    private val learnSkills = mutableListOf<String>()
    private var profileLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_SECURE,
            android.view.WindowManager.LayoutParams.FLAG_SECURE
        )

        findViewById<TextView>(R.id.tvTermsFooter).setOnClickListener {
            TermsDialogHelper.showTermsDialog(this, onAccepted = {}, onDeclined = {})
        }

        applyTheme()
        loadProfile()
        setupScrollBehavior()
        setupInsets()

        val user = Firebase.auth.currentUser
        val mfaBtn = findViewById<Button>(R.id.btnMfa)
        if (user?.multiFactor?.enrolledFactors?.isNotEmpty() == true) {
            mfaBtn.text = "Two-Factor Authentication: Enabled ✓"
            mfaBtn.isEnabled = false
        } else {
            mfaBtn.setOnClickListener {
                startActivity(Intent(this, MfaEnrollmentActivity::class.java))
            }
        }

        findViewById<Button>(R.id.btnAddTeach).setOnClickListener {
            val skill = findViewById<EditText>(R.id.etTeachSkill).text.toString().trim()
            if (skill.isNotEmpty()) {
                teachSkills.add(skill)
                findViewById<EditText>(R.id.etTeachSkill).text.clear()
                renderSkills()
            }
        }

        findViewById<Button>(R.id.btnAddLearn).setOnClickListener {
            val skill = findViewById<EditText>(R.id.etLearnSkill).text.toString().trim()
            if (skill.isNotEmpty()) {
                learnSkills.add(skill)
                findViewById<EditText>(R.id.etLearnSkill).text.clear()
                renderSkills()
            }
        }

        findViewById<Button>(R.id.btnSave).setOnClickListener { saveProfile() }
    }

    override fun onResume() {
        super.onResume()
        applyTheme()
    }

    private fun renderSkills() {
        renderSkillList(
            container = findViewById(R.id.llTeachSkills),
            skills = teachSkills
        )
        renderSkillList(
            container = findViewById(R.id.llLearnSkills),
            skills = learnSkills
        )
    }

    private fun renderSkillList(container: LinearLayout, skills: MutableList<String>) {
        container.removeAllViews()
        val textPrimary = ThemeManager.parseColor(ThemeManager.getTextPrimary())
        val primary = ThemeManager.parseColor(ThemeManager.getPrimary())

        skills.forEachIndexed { index, skill ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(0, dpToPx(4), 0, dpToPx(4))
            }

            val tvSkill = TextView(this).apply {
                text = skill
                textSize = 15f
                setTextColor(textPrimary)
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                )
            }

            val btnEdit = Button(this).apply {
                text = "Edit"
                textSize = 12f
                setTextColor(primary)
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setOnClickListener {
                    val input = EditText(context)
                    input.setText(skill)
                    input.setSelection(input.text.length)
                    AlertDialog.Builder(context)
                        .setTitle("Edit skill")
                        .setView(input)
                        .setPositiveButton("Save") { _, _ ->
                            val edited = input.text.toString().trim()
                            if (edited.isNotEmpty()) {
                                skills[index] = edited
                                renderSkills()
                            }
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            }

            val btnDelete = Button(this).apply {
                text = "Remove"
                textSize = 12f
                setTextColor(android.graphics.Color.parseColor("#CC0000"))
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setOnClickListener {
                    AlertDialog.Builder(context)
                        .setTitle("Remove skill")
                        .setMessage("Remove \"$skill\"?")
                        .setPositiveButton("Remove") { _, _ ->
                            skills.removeAt(index)
                            renderSkills()
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            }

            row.addView(tvSkill)
            row.addView(btnEdit)
            row.addView(btnDelete)
            container.addView(row)
        }
    }

    private fun setupScrollBehavior() {
        val scrollView = findViewById<ScrollView>(R.id.scrollView)

        findViewById<EditText>(R.id.etTeachSkill).setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                scrollView.postDelayed({
                    val location = IntArray(2)
                    view.getLocationInWindow(location)
                    scrollView.smoothScrollTo(0, location[1] + scrollView.scrollY - dpToPx(100))
                }, 300)
            }
        }

        findViewById<EditText>(R.id.etLearnSkill).setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                scrollView.postDelayed({
                    val location = IntArray(2)
                    view.getLocationInWindow(location)
                    scrollView.smoothScrollTo(0, location[1] + scrollView.scrollY - dpToPx(100))
                }, 300)
            }
        }

        findViewById<EditText>(R.id.etBio).setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                scrollView.postDelayed({
                    scrollView.smoothScrollTo(0, 0)
                }, 300)
            }
        }
    }

    private fun setupInsets() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
            val rootView = findViewById<android.view.View>(android.R.id.content)
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, insets ->
                val navBar = insets.getInsets(
                    androidx.core.view.WindowInsetsCompat.Type.navigationBars()
                ).bottom
                val ime = insets.getInsets(
                    androidx.core.view.WindowInsetsCompat.Type.ime()
                ).bottom
                findViewById<ScrollView>(R.id.scrollView)
                    ?.setPadding(0, 0, 0, maxOf(ime, navBar))
                insets
            }
        } else {
            androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, true)
        }
    }

    private fun applyTheme() {
        try {
            val bg = ThemeManager.parseColor(ThemeManager.getBackground())
            val surface = ThemeManager.parseColor(ThemeManager.getSurface())
            val textPrimary = ThemeManager.parseColor(ThemeManager.getTextPrimary())
            val textSecondary = ThemeManager.parseColor(ThemeManager.getTextSecondary())

            findViewById<ScrollView>(R.id.scrollView)?.setBackgroundColor(bg)

            findViewById<EditText>(R.id.etBio)?.apply {
                setTextColor(textPrimary)
                setHintTextColor(textSecondary)
                setBackgroundColor(surface)
            }

            findViewById<EditText>(R.id.etTeachSkill)?.apply {
                setTextColor(textPrimary)
                setHintTextColor(textSecondary)
            }

            findViewById<EditText>(R.id.etLearnSkill)?.apply {
                setTextColor(textPrimary)
                setHintTextColor(textSecondary)
            }

            listOf(R.id.tvTitle, R.id.tvBioLabel, R.id.tvTeachLabel, R.id.tvLearnLabel)
                .forEach { id ->
                    findViewById<TextView>(id)?.setTextColor(textPrimary)
                }

            if (profileLoaded) renderSkills()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sanitizeSkills(skills: List<String>): List<String> {
        return skills
            .map { it.trim().replace(Regex("<[^>]*>"), "").take(50) }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(20)
    }

    private fun loadProfile() {
        if (profileLoaded) return
        val uid = Firebase.auth.currentUser?.uid ?: return
        Firebase.firestore.collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                val user = doc.toObject(User::class.java) ?: return@addOnSuccessListener
                val bioField = findViewById<EditText>(R.id.etBio)
                bioField.setText(user.bio)
                bioField.setTextColor(ThemeManager.parseColor(ThemeManager.getTextPrimary()))
                bioField.setHintTextColor(ThemeManager.parseColor(ThemeManager.getTextSecondary()))
                bioField.setBackgroundColor(ThemeManager.parseColor(ThemeManager.getSurface()))
                teachSkills.clear()
                teachSkills.addAll(user.skillsToTeach)
                learnSkills.clear()
                learnSkills.addAll(user.skillsToLearn)
                profileLoaded = true
                renderSkills()
            }
    }

    private fun saveProfile() {
        val uid = Firebase.auth.currentUser?.uid ?: return
        val bio = findViewById<EditText>(R.id.etBio).text.toString()
            .trim()
            .replace(Regex("<[^>]*>"), "")
            .take(500)

        val sanitizedTeach = sanitizeSkills(teachSkills)
        val sanitizedLearn = sanitizeSkills(learnSkills)

        Firebase.firestore.collection("users").document(uid)
            .update(
                "bio", bio,
                "skillsToTeach", sanitizedTeach,
                "skillsToLearn", sanitizedLearn
            )
            .addOnSuccessListener {
                Toast.makeText(this, "Profile saved!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save", Toast.LENGTH_SHORT).show()
            }
    }
}