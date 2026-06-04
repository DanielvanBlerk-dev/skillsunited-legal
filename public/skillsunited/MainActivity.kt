package com.dkvb.skillswap

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: UserAdapter
    private val userList = mutableListOf<User>()
    private val filteredList = mutableListOf<User>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkTermsAccepted {
            setupUI()
        }
    }

    private fun setupUI() {
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = UserAdapter(filteredList) { user ->
            val intent = Intent(this, UserProfileActivity::class.java)
            intent.putExtra("userId", user.uid)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        findViewById<Button>(R.id.btnRequests).setOnClickListener {
            startActivity(Intent(this, MatchRequestActivity::class.java))
        }

        findViewById<Button>(R.id.btnProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            Firebase.auth.signOut()
            ThemeManager.clearTermsAccepted(this)
            MessageListenerService.stopListening()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        findViewById<TextInputEditText>(R.id.etSearch)
            .addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    filterUsers(s.toString())
                }
                override fun afterTextChanged(s: android.text.Editable?) {}
            })

        setupInsets()
        applyThemeToSearch()
        migrateExistingChats()
        loadUsers()
    }

    private fun setupInsets() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
            val bottomButtons = findViewById<android.widget.LinearLayout>(R.id.bottomButtons)
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(bottomButtons) { view, insets ->
                val navBar = insets.getInsets(
                    androidx.core.view.WindowInsetsCompat.Type.navigationBars()
                ).bottom
                view.setPadding(
                    view.paddingLeft,
                    view.paddingTop,
                    view.paddingRight,
                    navBar + dpToPx(16)
                )
                insets
            }
        } else {
            androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, true)
        }
    }

    private fun applyThemeToSearch() {
        val textPrimary = ThemeManager.parseColor(ThemeManager.getTextPrimary())
        val surfaceColor = ThemeManager.parseColor(ThemeManager.getSurface())
        val primary = ThemeManager.parseColor(ThemeManager.getPrimary())
        val hintColor = ThemeManager.parseColor(ThemeManager.getTextSecondary())

        findViewById<TextView>(R.id.tvBrowseTitle)?.setTextColor(textPrimary)

        val searchField = findViewById<TextInputEditText>(R.id.etSearch) ?: return
        val searchLayout = searchField.parent.parent
                as? com.google.android.material.textfield.TextInputLayout ?: return

        searchField.setTextColor(textPrimary)
        searchField.setHintTextColor(hintColor)
        searchLayout.boxBackgroundColor = surfaceColor
        searchLayout.setBoxStrokeColor(primary)
        searchLayout.defaultHintTextColor =
            android.content.res.ColorStateList.valueOf(hintColor)

        if (::adapter.isInitialized) adapter.notifyDataSetChanged()
    }

    private fun filterUsers(query: String) {
        filteredList.clear()
        if (query.isEmpty()) {
            filteredList.addAll(userList)
        } else {
            val tags = query.lowercase().trim().split("\\s+".toRegex())
                .filter { it.isNotEmpty() }
            filteredList.addAll(userList.filter { user ->
                tags.any { tag ->
                    user.skillsToTeach.any { it.lowercase().contains(tag) } ||
                            user.skillsToLearn.any { it.lowercase().contains(tag) }
                }
            })
        }
        adapter.notifyDataSetChanged()
    }

    private fun loadUsers() {
        val currentUid = Firebase.auth.currentUser?.uid ?: return
        Firebase.firestore.collection("users")
            .get()
            .addOnSuccessListener { result ->
                userList.clear()
                for (doc in result) {
                    val user = doc.toObject(User::class.java)
                    if (user.uid != currentUid) userList.add(user)
                }
                filterUsers(
                    findViewById<TextInputEditText>(R.id.etSearch).text.toString()
                )
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load users: ${e.message}",
                    Toast.LENGTH_SHORT).show()
            }
    }

    override fun onResume() {
        super.onResume()
        if (::adapter.isInitialized) {
            loadUsers()
            applyThemeToSearch()
        }
    }

    private fun migrateExistingChats() {
        val currentUid = Firebase.auth.currentUser?.uid ?: return
        android.util.Log.d("Migration", "Starting migration for uid=$currentUid")

        val prefs = getSharedPreferences("migration", MODE_PRIVATE)
        if (prefs.getBoolean("userChats_migrated", false)) {
            android.util.Log.d("Migration", "Already migrated, skipping")
            return
        }

        Firebase.firestore.collection("userChats")
            .document(currentUid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    android.util.Log.d("Migration", "userChats already exists, skipping")
                    prefs.edit().putBoolean("userChats_migrated", true).apply()
                    return@addOnSuccessListener
                }

                Firebase.firestore.collection("users")
                    .get()
                    .addOnSuccessListener { users ->
                        val chatIds = users.documents
                            .filter { it.id != currentUid }
                            .map { userDoc ->
                                val otherUid = userDoc.id
                                if (currentUid < otherUid) "${currentUid}_${otherUid}"
                                else "${otherUid}_${currentUid}"
                            }

                        android.util.Log.d("Migration", "Potential chat IDs: $chatIds")

                        if (chatIds.isEmpty()) {
                            prefs.edit().putBoolean("userChats_migrated", true).apply()
                            return@addOnSuccessListener
                        }

                        var checkedCount = 0
                        val activeChatIds = mutableListOf<String>()

                        chatIds.forEach { chatId ->
                            Firebase.firestore.collection("chats")
                                .document(chatId)
                                .collection("messages")
                                .limit(1)
                                .get()
                                .addOnSuccessListener { msgs ->
                                    checkedCount++
                                    if (!msgs.isEmpty) {
                                        activeChatIds.add(chatId)
                                        android.util.Log.d("Migration", "Active chat found: $chatId")
                                    }
                                    if (checkedCount == chatIds.size) {
                                        if (activeChatIds.isNotEmpty()) {
                                            Firebase.firestore.collection("userChats")
                                                .document(currentUid)
                                                .set(
                                                    mapOf("chatIds" to activeChatIds),
                                                    com.google.firebase.firestore.SetOptions.merge()
                                                )
                                                .addOnSuccessListener {
                                                    android.util.Log.d("Migration",
                                                        "Migration complete: $activeChatIds")
                                                    prefs.edit()
                                                        .putBoolean("userChats_migrated", true)
                                                        .apply()
                                                }
                                        } else {
                                            prefs.edit()
                                                .putBoolean("userChats_migrated", true)
                                                .apply()
                                        }
                                    }
                                }
                                .addOnFailureListener { checkedCount++ }
                        }
                    }
            }
    }
}