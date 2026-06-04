package com.dkvb.skillswap

import android.util.Log
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.*

object MessageListenerService {

    private val listeners = mutableListOf<ListenerRegistration>()
    private val seenMessageIds = mutableSetOf<String>()
    private val watchedChatIds = mutableSetOf<String>()
    private var isListening = false
    private var pollJob: Job? = null
    private val readChatIds = mutableSetOf<String>()
    private val unreadPerChat = mutableMapOf<String, Int>()
    private val unreadChatIds = mutableSetOf<String>()

    var unreadCount: Int = 0
        private set
    var onNewMessage: ((senderId: String, senderName: String, message: String, chatId: String) -> Unit)? = null
    var onUnreadCountChanged: ((Int) -> Unit)? = null
    var onNewMatchRequest: ((fromName: String) -> Unit)? = null

    fun markChatAsRead(chatId: String) {
        readChatIds.add(chatId)
        unreadChatIds.remove(chatId)
        val chatUnread = unreadPerChat.getOrDefault(chatId, 0)
        unreadCount = maxOf(0, unreadCount - chatUnread)
        unreadPerChat.remove(chatId)
        onUnreadCountChanged?.invoke(unreadCount)
    }

    fun markAllRead() {
        android.util.Log.d("BadgeDebug", "markAllRead called")
        unreadCount = 0
        unreadPerChat.clear()
        unreadChatIds.clear()
        readChatIds.clear()
        onUnreadCountChanged?.invoke(0)
    }

    fun isChatRead(chatId: String): Boolean {
        return !unreadChatIds.contains(chatId)
    }

    fun markMessageAsSeen(messageId: String) {
        seenMessageIds.add(messageId)
    }

    fun markChatAsUnread(chatId: String) {
        unreadChatIds.add(chatId)
        android.util.Log.d("BoldDebug", "markChatAsUnread: $chatId unreadChatIds=$unreadChatIds")
    }

    fun startListening() {
        val currentUid = Firebase.auth.currentUser?.uid
        Log.d("MessageListener", "startListening called uid=$currentUid isListening=$isListening")
        if (currentUid == null) return
        if (isListening) return
        isListening = true

        startRealtimeListener(currentUid)
        listenForMatchRequests(currentUid)

        pollJob = CoroutineScope(Dispatchers.IO).launch {
            while (isListening) {
                delay(15_000)
                Log.d("MessageListener", "Polling for new messages...")
                checkForNewMessages(currentUid)
            }
        }
    }

    private fun startRealtimeListener(currentUid: String) {
        Log.d("MessageListener", "Starting realtime listener for uid=$currentUid")
        val listener = Firebase.firestore
            .collectionGroup("messages")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("MessageListener", "Realtime error: ${error.message}")
                    return@addSnapshotListener
                }
                Log.d("MessageListener", "Realtime snapshot received docs=${snapshot?.size()}")
                snapshot?.documentChanges?.forEach { change ->
                    if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                        processMessage(change.document, currentUid)
                    }
                }
            }
        listeners.add(listener)
    }

    private fun checkForNewMessages(currentUid: String) {
        val cutoff = System.currentTimeMillis() - 60_000
        Firebase.firestore
            .collectionGroup("messages")
            .whereGreaterThan("timestamp", cutoff)
            .get()
            .addOnSuccessListener { snapshot ->
                Log.d("MessageListener", "Poll found ${snapshot.size()} recent messages")
                snapshot.documents.forEach { doc ->
                    processMessage(doc, currentUid)
                }
            }
            .addOnFailureListener { e ->
                Log.e("MessageListener", "Poll failed: ${e.message}")
            }
    }

    private fun processMessage(
        doc: com.google.firebase.firestore.DocumentSnapshot,
        currentUid: String
    ) {
        val messageId = doc.id
        val senderId = doc.getString("senderId") ?: return
        val text = doc.getString("text") ?: return
        val timestamp = doc.getLong("timestamp") ?: 0L
        val age = System.currentTimeMillis() - timestamp
        val chatId = doc.reference.parent.parent?.id ?: return

        Log.d("MessageListener", "Processing message id=$messageId senderId=$senderId age=${age}ms chatId=$chatId")

        if (age > 60_000) {
            Log.d("MessageListener", "Too old, skipping")
            return
        }
        if (senderId == currentUid) {
            Log.d("MessageListener", "Own message, skipping")
            return
        }
        if (seenMessageIds.contains(messageId)) {
            Log.d("MessageListener", "Already seen, skipping")
            return
        }
        if (!chatId.contains(currentUid)) {
            Log.d("MessageListener", "Chat doesn't involve current user, skipping")
            return
        }

        seenMessageIds.add(messageId)
        Log.d("MessageListener", "NEW MESSAGE from $senderId in $chatId")

        // Only increment unread if this chat hasn't been read
        if (!readChatIds.contains(chatId)) {
            unreadPerChat[chatId] = unreadPerChat.getOrDefault(chatId, 0) + 1
            unreadChatIds.add(chatId)
            unreadCount++
            android.util.Log.d("BadgeDebug", "processMessage: incrementing unreadCount to $unreadCount")
            onUnreadCountChanged?.invoke(unreadCount)
            android.util.Log.d("BadgeDebug", "firing onUnreadCountChanged with count=$unreadCount")
        }

        Firebase.firestore.collection("users")
            .document(senderId)
            .get()
            .addOnSuccessListener { userDoc ->
                val senderName = userDoc.getString("name") ?: "Someone"
                Log.d("MessageListener", "Firing onNewMessage for $senderName text=$text")
                onNewMessage?.invoke(senderId, senderName, text, chatId)
            }
            .addOnFailureListener { e ->
                Log.e("MessageListener", "Failed to fetch user: ${e.message}")
            }
    }

    private fun listenForMatchRequests(currentUid: String) {
        android.util.Log.d("MatchRequest", "Starting match request listener for uid=$currentUid")

        // Track which request IDs we've already notified about
        val notifiedRequestIds = mutableSetOf<String>()

        val listener = Firebase.firestore.collection("matchRequests")
            .whereEqualTo("toUid", currentUid)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("MatchRequest", "Listener error: ${error.message}")
                    return@addSnapshotListener
                }

                android.util.Log.d("MatchRequest", "Snapshot received, docs=${snapshot?.size()} changes=${snapshot?.documentChanges?.size}")

                // Process ALL pending documents not yet notified
                snapshot?.documents?.forEach { doc ->
                    val requestId = doc.id
                    val fromName = doc.getString("fromName") ?: "Someone"
                    val timestamp = doc.getLong("timestamp") ?: 0L
                    val age = System.currentTimeMillis() - timestamp

                    android.util.Log.d("MatchRequest", "Found request id=$requestId fromName=$fromName age=${age}ms alreadyNotified=${notifiedRequestIds.contains(requestId)}")

                    // Only notify for requests newer than 60 seconds
                    // and ones we haven't already shown a banner for
                    if (!notifiedRequestIds.contains(requestId) && age < 60_000) {
                        notifiedRequestIds.add(requestId)
                        android.util.Log.d("MatchRequest", "Firing callback for new request from $fromName")
                        onNewMatchRequest?.invoke(fromName)
                    } else if (notifiedRequestIds.contains(requestId)) {
                        android.util.Log.d("MatchRequest", "Already notified for $requestId, skipping")
                    } else {
                        android.util.Log.d("MatchRequest", "Request too old (${age}ms), skipping banner")
                    }
                }
            }
        listeners.add(listener)
    }

    fun stopListening() {
        Log.d("MessageListener", "stopListening called")
        listeners.forEach { it.remove() }
        listeners.clear()
        watchedChatIds.clear()
        pollJob?.cancel()
        pollJob = null
        isListening = false
    }
}