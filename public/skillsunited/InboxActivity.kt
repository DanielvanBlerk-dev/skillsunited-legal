package com.dkvb.skillswap

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

class InboxActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private val inboxMessages = mutableListOf<InboxMessage>()
    private lateinit var adapter: InboxAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inbox)

        Firebase.firestore.collection("chats")
            .document("axYGOyfDRBRkyE2FDRqT8NO08A72_h27zppsz2eZ45XTPDv1db4GTLVy2")
            .get()
            .addOnSuccessListener { doc ->
                android.util.Log.d("InboxDebug", "Direct fetch: exists=${doc.exists()}")
                doc.reference.collection("messages").get()
                    .addOnSuccessListener { msgs ->
                        android.util.Log.d("InboxDebug", "Messages in chat: ${msgs.size()}")
                        msgs.documents.forEach { m ->
                            android.util.Log.d("InboxDebug", "Message: ${m.getString("text")} from ${m.getString("senderId")}")
                        }
                    }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("InboxDebug", "Direct fetch failed: ${e.message}")
            }

        recyclerView = findViewById(R.id.rvInbox)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = InboxAdapter(inboxMessages) { message ->
            // Update lastRead for this chat in Firestore
            val currentUid = Firebase.auth.currentUser?.uid ?: return@InboxAdapter
            Firebase.firestore.collection("userChats")
                .document(currentUid)
                .set(
                    mapOf("lastRead_${message.chatId}" to System.currentTimeMillis()),
                    com.google.firebase.firestore.SetOptions.merge()
                )
            // Navigate to chat (respects chatEnabled feature flag)
            openChatIfEnabled(message.senderId, message.senderName)
        }
        recyclerView.adapter = adapter

        findViewById<android.widget.TextView>(R.id.tvInboxTitle).setTextColor(
            ThemeManager.parseColor(ThemeManager.getTextPrimary())
        )

        loadInbox()

        // Temporary Firestore access test
        Firebase.firestore.collection("chats")
            .document("1vuEqlaA3UPoocGgcaRx4NoELQD2_h27zppsz2eZ45XTPDv1db4GTLVy2")
            .get()
            .addOnSuccessListener { doc ->
                android.util.Log.d("InboxDebug", "Direct fetch: exists=${doc.exists()} id=${doc.id}")
            }
            .addOnFailureListener { e ->
                android.util.Log.e("InboxDebug", "Direct fetch failed: ${e.message}")
            }
    }

    override fun setupInboxButton() {
        // Don't show inbox button on the inbox page
    }

    private val chatListeners = mutableListOf<com.google.firebase.firestore.ListenerRegistration>()

    private var userChatsListener: com.google.firebase.firestore.ListenerRegistration? = null

    private fun loadInbox() {
        val currentUid = Firebase.auth.currentUser?.uid ?: return

        chatListeners.forEach { it.remove() }
        chatListeners.clear()
        inboxMessages.clear()
        adapter.notifyDataSetChanged()

        userChatsListener?.remove()

        // Load userChats document which contains both chatIds and lastRead timestamps
        userChatsListener = Firebase.firestore
            .collection("userChats")
            .document(currentUid)
            .addSnapshotListener { doc, error ->
                if (error != null) return@addSnapshotListener

                @Suppress("UNCHECKED_CAST")
                val chatIds = doc?.get("chatIds") as? List<String> ?: emptyList()

                if (chatIds.isEmpty()) {
                    findViewById<TextView>(R.id.tvEmpty).visibility = View.VISIBLE
                    return@addSnapshotListener
                }

                // Extract all lastRead timestamps from document
                val lastReadMap = mutableMapOf<String, Long>()
                chatIds.forEach { chatId ->
                    lastReadMap[chatId] = doc?.getLong("lastRead_$chatId") ?: 0L
                }

                chatListeners.forEach { it.remove() }
                chatListeners.clear()

                val messageMap = mutableMapOf<String, InboxMessage>()
                val unreadMap = mutableMapOf<String, Boolean>()

                chatIds.forEach { chatId ->
                    val otherUid = chatId.replace(currentUid, "").replace("_", "")
                    val lastRead = lastReadMap[chatId] ?: 0L

                    Firebase.firestore.collection("users")
                        .document(otherUid)
                        .get()
                        .addOnSuccessListener { userDoc ->
                            val displayName = userDoc.getString("name") ?: "Someone"

                            val listener = Firebase.firestore
                                .collection("chats")
                                .document(chatId)
                                .collection("messages")
                                .orderBy("timestamp",
                                    com.google.firebase.firestore.Query.Direction.DESCENDING)
                                .limit(1)
                                .addSnapshotListener { messages, err ->
                                    if (err != null) return@addSnapshotListener

                                    val msgDoc = messages?.documents?.firstOrNull()
                                    if (msgDoc != null) {
                                        val senderId = msgDoc.getString("senderId") ?: ""
                                        val text = msgDoc.getString("text") ?: ""
                                        val timestamp = msgDoc.getLong("timestamp") ?: 0L

                                        // Unread if message is newer than lastRead
                                        // and was sent by someone else
                                        val isUnread = timestamp > lastRead && senderId != currentUid
                                        unreadMap[chatId] = isUnread

                                        android.util.Log.d("BoldDebug",
                                            "chatId=$chatId timestamp=$timestamp lastRead=$lastRead isUnread=$isUnread")

                                        messageMap[chatId] = InboxMessage(
                                            chatId = chatId,
                                            senderId = otherUid,
                                            senderName = displayName,
                                            text = if (senderId == currentUid) "You: $text" else text,
                                            timestamp = timestamp,
                                            isUnread = isUnread
                                        )
                                    } else {
                                        messageMap.remove(chatId)
                                        unreadMap.remove(chatId)
                                    }

                                    inboxMessages.clear()
                                    inboxMessages.addAll(
                                        messageMap.values.sortedByDescending { it.timestamp }
                                    )
                                    adapter.notifyDataSetChanged()
                                    findViewById<TextView>(R.id.tvEmpty).visibility =
                                        if (inboxMessages.isEmpty()) View.VISIBLE else View.GONE
                                }
                            chatListeners.add(listener)
                        }
                }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        userChatsListener?.remove()
        chatListeners.forEach { it.remove() }
        chatListeners.clear()
    }

    private fun applyTheme() {
        val bg = ThemeManager.parseColor(ThemeManager.getBackground())
        val textPrimary = ThemeManager.parseColor(ThemeManager.getTextPrimary())
        val textSecondary = ThemeManager.parseColor(ThemeManager.getTextSecondary())
        val primary = ThemeManager.parseColor(ThemeManager.getPrimary())

        // Root background
        findViewById<android.widget.LinearLayout>(R.id.inboxRoot)?.setBackgroundColor(bg)

        // Title
        findViewById<TextView>(R.id.tvInboxTitle)?.setTextColor(primary)

        // Empty message
        findViewById<TextView>(R.id.tvEmpty)?.setTextColor(textSecondary)

        // Refresh list items
        if (::adapter.isInitialized) adapter.notifyDataSetChanged()
    }

    override fun onResume() {
        super.onResume()
        applyTheme()
        loadInbox()
    }
}