package com.dkvb.skillswap

import android.os.Bundle
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ChatActivity : BaseActivity() {

    private lateinit var adapter: MessageAdapter
    private val messages = mutableListOf<Message>()
    private lateinit var chatId: String
    private lateinit var otherUserName: String
    private lateinit var recyclerView: RecyclerView
    private var chatEnabledListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_SECURE,
            android.view.WindowManager.LayoutParams.FLAG_SECURE
        )

        val otherUserId = intent.getStringExtra("userId") ?: return
        otherUserName = intent.getStringExtra("userName") ?: "User"
        val currentUid = Firebase.auth.currentUser?.uid ?: return

        chatId = if (currentUid < otherUserId) {
            "${currentUid}_${otherUserId}"
        } else {
            "${otherUserId}_${currentUid}"
        }

        ensureUserChatsUpdated()
        updateLastRead()
        MessageListenerService.markChatAsRead(chatId)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }

        adapter = MessageAdapter(messages, currentUid)
        recyclerView.adapter = adapter

        setupInsets()
        applyTheme()

        Firebase.firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .get()
            .addOnSuccessListener { msgs ->
                msgs.documents.forEach { doc ->
                    MessageListenerService.markMessageAsSeen(doc.id)
                }
            }

        listenForMessages()
        listenForChatEnabled()

        recyclerView.addOnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
            if (bottom < oldBottom && messages.isNotEmpty()) {
                recyclerView.postDelayed({
                    recyclerView.scrollToPosition(messages.size - 1)
                }, 100)
            }
        }

        findViewById<Button>(R.id.btnSend).setOnClickListener {
            sendMessage()
        }
    }

    /**
     * Listens to config/features in real-time. If chatEnabled flips to false while
     * the user is in this screen, they are immediately kicked back to the previous screen.
     */
    private fun listenForChatEnabled() {
        chatEnabledListener = Firebase.firestore
            .collection("config")
            .document("features")
            .addSnapshotListener { doc, error ->
                if (error != null) return@addSnapshotListener
                val chatEnabled = doc?.getBoolean("chatEnabled") ?: true
                if (!chatEnabled) {
                    Toast.makeText(this, "Chat is temporarily unavailable.", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        chatEnabledListener?.remove()
    }

    private fun setupInsets() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
            val rootLayout = findViewById<LinearLayout>(R.id.chatRootLayout)
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { _, insets ->
                val statusBar = insets.getInsets(
                    androidx.core.view.WindowInsetsCompat.Type.statusBars()
                ).top
                val navBar = insets.getInsets(
                    androidx.core.view.WindowInsetsCompat.Type.navigationBars()
                ).bottom
                val ime = insets.getInsets(
                    androidx.core.view.WindowInsetsCompat.Type.ime()
                ).bottom

                rootLayout.setPadding(0, statusBar, 0, maxOf(ime, navBar))

                val titleBar = findViewById<TextView>(R.id.tvChatTitle)
                titleBar.setPadding(
                    resources.getDimensionPixelSize(R.dimen.chat_title_horizontal_padding),
                    resources.getDimensionPixelSize(R.dimen.chat_title_vertical_padding),
                    resources.getDimensionPixelSize(R.dimen.chat_title_horizontal_padding),
                    resources.getDimensionPixelSize(R.dimen.chat_title_vertical_padding)
                )

                applyTheme()
                insets
            }
        } else {
            androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, true)
        }
    }

    override fun onResume() {
        super.onResume()
        applyTheme()
    }

    private fun applyTheme() {
        val primary = ThemeManager.parseColor(ThemeManager.getPrimary())
        val buttonText = ThemeManager.parseColor(ThemeManager.getButtonText())
        val background = ThemeManager.parseColor(ThemeManager.getBackground())
        val surface = ThemeManager.parseColor(ThemeManager.getSurface())
        val textPrimary = ThemeManager.parseColor(ThemeManager.getTextPrimary())
        val textSecondary = ThemeManager.parseColor(ThemeManager.getTextSecondary())

        findViewById<TextView>(R.id.tvChatTitle)?.apply {
            text = otherUserName
            setBackgroundColor(primary)
            setTextColor(buttonText)
        }

        if (::recyclerView.isInitialized) {
            recyclerView.setBackgroundColor(background)
        }

        findViewById<LinearLayout>(R.id.inputBar)?.setBackgroundColor(surface)

        findViewById<EditText>(R.id.etMessage)?.apply {
            setTextColor(textPrimary)
            setHintTextColor(textSecondary)
            setBackgroundColor(surface)
        }

        findViewById<Button>(R.id.btnSend)?.apply {
            setBackgroundColor(primary)
            setTextColor(buttonText)
        }

        if (::adapter.isInitialized) {
            adapter.notifyDataSetChanged()
        }
    }

    private fun listenForMessages() {
        Firebase.firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                messages.clear()
                snapshot?.documents?.forEach { doc ->
                    val message = doc.toObject(Message::class.java)
                    if (message != null) messages.add(message)
                }
                adapter.notifyDataSetChanged()
                recyclerView.scrollToPosition(messages.size - 1)
            }
    }

    private fun sanitizeMessage(text: String): String {
        return text
            .trim()
            .replace(Regex("<[^>]*>"), "")
            .take(1000)
    }

    private fun sendMessage() {
        val input = findViewById<EditText>(R.id.etMessage)
        val rawText = input.text.toString()
        val text = sanitizeMessage(rawText)
        if (text.isEmpty()) return

        val currentUid = Firebase.auth.currentUser?.uid ?: return
        val message = hashMapOf(
            "senderId" to currentUid,
            "text" to text,
            "timestamp" to System.currentTimeMillis()
        )

        Firebase.firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .add(message)
            .addOnSuccessListener {
                input.text.clear()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show()
            }
    }

    private fun ensureUserChatsUpdated() {
        val currentUid = Firebase.auth.currentUser?.uid ?: return
        val otherUserId = intent.getStringExtra("userId") ?: return
        val db = Firebase.firestore
        val batch = db.batch()

        batch.set(
            db.collection("userChats").document(currentUid),
            mapOf("chatIds" to com.google.firebase.firestore.FieldValue.arrayUnion(chatId)),
            com.google.firebase.firestore.SetOptions.merge()
        )
        batch.set(
            db.collection("userChats").document(otherUserId),
            mapOf("chatIds" to com.google.firebase.firestore.FieldValue.arrayUnion(chatId)),
            com.google.firebase.firestore.SetOptions.merge()
        )

        batch.commit()
            .addOnSuccessListener {
                android.util.Log.d("UserChats", "userChats updated for both users")
            }
            .addOnFailureListener { e ->
                android.util.Log.e("UserChats", "Failed to update userChats: ${e.message}")
            }
    }

    private fun updateLastRead() {
        val currentUid = Firebase.auth.currentUser?.uid ?: return
        Firebase.firestore.collection("userChats")
            .document(currentUid)
            .set(
                mapOf("lastRead_$chatId" to System.currentTimeMillis()),
                com.google.firebase.firestore.SetOptions.merge()
            )
    }
}