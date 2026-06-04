package com.dkvb.skillswap

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MatchRequestActivity : BaseActivity() {

    private lateinit var incomingAdapter: MatchRequestAdapter
    private lateinit var outgoingAdapter: MatchRequestAdapter
    private val incomingRequests = mutableListOf<MatchRequest>()
    private val outgoingRequests = mutableListOf<MatchRequest>()
    private var currentTab = Tab.INCOMING

    // Real-time listener for the current user's profile
    private var currentUserProfileListener: ListenerRegistration? = null

    // Real-time listeners for each sender's profile on incoming requests
    // keyed by fromUid so we can remove them individually
    private val senderProfileListeners = mutableMapOf<String, ListenerRegistration>()

    enum class Tab { INCOMING, OUTGOING }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_match_request)

        android.util.Log.d("MatchRequest",
            "Current user UID = ${Firebase.auth.currentUser?.uid}")

        applyTheme()
        setupInsets()
        setupTabs()
        setupRecyclerViews()
        loadIncomingRequests()
        loadOutgoingRequests()
        listenToCurrentUserProfile()
        showTab(Tab.INCOMING)
    }

    override fun onResume() {
        super.onResume()
        applyTheme()
    }

    override fun onDestroy() {
        super.onDestroy()
        currentUserProfileListener?.remove()
        senderProfileListeners.values.forEach { it.remove() }
        senderProfileListeners.clear()
    }

    private fun applyTheme() {
        val bg = ThemeManager.parseColor(ThemeManager.getBackground())
        val primary = ThemeManager.parseColor(ThemeManager.getPrimary())
        val textSecondary = ThemeManager.parseColor(ThemeManager.getTextSecondary())

        findViewById<LinearLayout>(R.id.matchRequestRoot)?.setBackgroundColor(bg)
        findViewById<TextView>(R.id.tvTitle)?.setTextColor(primary)
        findViewById<TextView>(R.id.tvEmpty)?.setTextColor(textSecondary)

        // Re-bind all visible cards so they pick up the current theme colours
        if (::incomingAdapter.isInitialized) incomingAdapter.notifyDataSetChanged()
        if (::outgoingAdapter.isInitialized) outgoingAdapter.notifyDataSetChanged()
    }

    /**
     * Listens to the current user's profile in real time.
     * When their skills change, updates all request cards that show "Your Skills".
     *
     * Incoming requests: toSkillsToTeach / toSkillsToLearn (current user is recipient)
     * Outgoing requests: fromSkillsToTeach / fromSkillsToLearn (current user is sender)
     */
    private fun listenToCurrentUserProfile() {
        val currentUid = Firebase.auth.currentUser?.uid ?: return
        currentUserProfileListener = Firebase.firestore
            .collection("users")
            .document(currentUid)
            .addSnapshotListener { doc, error ->
                if (error != null || doc == null) return@addSnapshotListener
                val user = doc.toObject(User::class.java) ?: return@addSnapshotListener

                // Update "Your Skills" on incoming requests (current user is toUid)
                var incomingChanged = false
                incomingRequests.forEachIndexed { index, request ->
                    if (request.toUid == currentUid) {
                        incomingRequests[index] = request.copy(
                            toSkillsToTeach = user.skillsToTeach,
                            toSkillsToLearn = user.skillsToLearn
                        )
                        incomingChanged = true
                    }
                }
                if (incomingChanged) incomingAdapter.notifyDataSetChanged()

                // Update "Your Skills" on outgoing requests (current user is fromUid)
                var outgoingChanged = false
                outgoingRequests.forEachIndexed { index, request ->
                    if (request.fromUid == currentUid) {
                        outgoingRequests[index] = request.copy(
                            fromSkillsToTeach = user.skillsToTeach,
                            fromSkillsToLearn = user.skillsToLearn
                        )
                        outgoingChanged = true
                    }
                }
                if (outgoingChanged) outgoingAdapter.notifyDataSetChanged()
            }
    }

    /**
     * For each incoming request, listens to the sender's profile in real time.
     * When their skills change, updates the "Their Skills" section on the card.
     */
    private fun listenToSenderProfile(fromUid: String) {
        if (senderProfileListeners.containsKey(fromUid)) return
        val listener = Firebase.firestore
            .collection("users")
            .document(fromUid)
            .addSnapshotListener { doc, error ->
                if (error != null || doc == null) return@addSnapshotListener
                val user = doc.toObject(User::class.java) ?: return@addSnapshotListener

                var changed = false
                incomingRequests.forEachIndexed { index, request ->
                    if (request.fromUid == fromUid) {
                        incomingRequests[index] = request.copy(
                            fromSkillsToTeach = user.skillsToTeach,
                            fromSkillsToLearn = user.skillsToLearn,
                            fromBio = user.bio
                        )
                        changed = true
                    }
                }
                if (changed) incomingAdapter.notifyDataSetChanged()
            }
        senderProfileListeners[fromUid] = listener
    }

    private fun setupInsets() {
        val root = findViewById<LinearLayout>(R.id.matchRequestRoot) ?: return
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val navBar = insets.getInsets(
                androidx.core.view.WindowInsetsCompat.Type.navigationBars()
            ).bottom
            val statusBar = insets.getInsets(
                androidx.core.view.WindowInsetsCompat.Type.statusBars()
            ).top
            view.setPadding(0, statusBar, 0, navBar)
            insets
        }
    }

    private fun setupTabs() {
        findViewById<Button>(R.id.btnTabIncoming).setOnClickListener {
            showTab(Tab.INCOMING)
        }
        findViewById<Button>(R.id.btnTabOutgoing).setOnClickListener {
            showTab(Tab.OUTGOING)
        }
    }

    private fun showTab(tab: Tab) {
        currentTab = tab

        val primary = ThemeManager.parseColor(ThemeManager.getPrimary())
        val surface = ThemeManager.parseColor(ThemeManager.getSurface())
        val buttonText = ThemeManager.parseColor(ThemeManager.getButtonText())
        val textSecondary = ThemeManager.parseColor(ThemeManager.getTextSecondary())

        val btnIncoming = findViewById<Button>(R.id.btnTabIncoming)
        val btnOutgoing = findViewById<Button>(R.id.btnTabOutgoing)
        val rvIncoming = findViewById<RecyclerView>(R.id.rvIncoming)
        val rvOutgoing = findViewById<RecyclerView>(R.id.rvOutgoing)
        val tvEmpty = findViewById<TextView>(R.id.tvEmpty)

        tvEmpty.setTextColor(textSecondary)

        when (tab) {
            Tab.INCOMING -> {
                btnIncoming.setBackgroundColor(primary)
                btnIncoming.setTextColor(buttonText)
                btnOutgoing.setBackgroundColor(surface)
                btnOutgoing.setTextColor(primary)
                rvIncoming.visibility = View.VISIBLE
                rvOutgoing.visibility = View.GONE
                tvEmpty.text = "No incoming chat requests"
                tvEmpty.visibility =
                    if (incomingRequests.isEmpty()) View.VISIBLE else View.GONE
            }
            Tab.OUTGOING -> {
                btnOutgoing.setBackgroundColor(primary)
                btnOutgoing.setTextColor(buttonText)
                btnIncoming.setBackgroundColor(surface)
                btnIncoming.setTextColor(primary)
                rvIncoming.visibility = View.GONE
                rvOutgoing.visibility = View.VISIBLE
                tvEmpty.text = "No outgoing chat requests"
                tvEmpty.visibility =
                    if (outgoingRequests.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun setupRecyclerViews() {
        incomingAdapter = MatchRequestAdapter(
            requests = incomingRequests,
            showAcceptDecline = true,
            onAccept = { request -> acceptRequest(request) },
            onDecline = { request -> declineRequest(request) }
        )
        findViewById<RecyclerView>(R.id.rvIncoming).apply {
            layoutManager = LinearLayoutManager(this@MatchRequestActivity)
            adapter = incomingAdapter
        }

        outgoingAdapter = MatchRequestAdapter(
            requests = outgoingRequests,
            showAcceptDecline = false,
            onAccept = { },
            onDecline = { request -> cancelRequest(request) }
        )
        findViewById<RecyclerView>(R.id.rvOutgoing).apply {
            layoutManager = LinearLayoutManager(this@MatchRequestActivity)
            adapter = outgoingAdapter
        }
    }

    private fun loadIncomingRequests() {
        val currentUid = Firebase.auth.currentUser?.uid ?: return
        Firebase.firestore.collection("matchRequests")
            .whereEqualTo("toUid", currentUid)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("MatchRequest", "Incoming error: ${error.message}")
                    return@addSnapshotListener
                }
                incomingRequests.clear()

                // Remove sender listeners for uids no longer in the list
                val activeFromUids = snapshot?.documents
                    ?.mapNotNull { it.getString("fromUid") }?.toSet() ?: emptySet()
                val staleUids = senderProfileListeners.keys - activeFromUids
                staleUids.forEach { uid ->
                    senderProfileListeners[uid]?.remove()
                    senderProfileListeners.remove(uid)
                }

                snapshot?.documents?.forEach { doc ->
                    val request = doc.toObject(MatchRequest::class.java)
                    if (request != null) {
                        incomingRequests.add(request.copy(id = doc.id))
                        listenToSenderProfile(request.fromUid)
                    }
                }
                incomingAdapter.notifyDataSetChanged()
                if (currentTab == Tab.INCOMING) {
                    findViewById<TextView>(R.id.tvEmpty).visibility =
                        if (incomingRequests.isEmpty()) View.VISIBLE else View.GONE
                }
                android.util.Log.d("MatchRequest",
                    "Incoming requests loaded: ${incomingRequests.size}")
            }
    }

    private fun loadOutgoingRequests() {
        val currentUid = Firebase.auth.currentUser?.uid ?: return
        Firebase.firestore.collection("matchRequests")
            .whereEqualTo("fromUid", currentUid)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("MatchRequest", "Outgoing error: ${error.message}")
                    return@addSnapshotListener
                }
                outgoingRequests.clear()
                snapshot?.documents?.forEach { doc ->
                    val request = doc.toObject(MatchRequest::class.java)
                    if (request != null) {
                        outgoingRequests.add(request.copy(id = doc.id))
                    }
                }
                outgoingAdapter.notifyDataSetChanged()
                if (currentTab == Tab.OUTGOING) {
                    findViewById<TextView>(R.id.tvEmpty).visibility =
                        if (outgoingRequests.isEmpty()) View.VISIBLE else View.GONE
                }
                android.util.Log.d("MatchRequest",
                    "Outgoing requests loaded: ${outgoingRequests.size}")
            }
    }

    private fun acceptRequest(request: MatchRequest) {
        Firebase.firestore.collection("matchRequests")
            .document(request.id)
            .update("status", "accepted")
            .addOnSuccessListener {
                openChatIfEnabled(request.fromUid, request.fromName)
                Toast.makeText(
                    this,
                    "Chat started with ${request.fromName}!",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to accept request",
                    Toast.LENGTH_SHORT).show()
            }
    }

    private fun declineRequest(request: MatchRequest) {
        Firebase.firestore.collection("matchRequests")
            .document(request.id)
            .update("status", "declined")
            .addOnSuccessListener {
                Toast.makeText(this, "Request declined", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to decline request",
                    Toast.LENGTH_SHORT).show()
            }
    }

    private fun cancelRequest(request: MatchRequest) {
        android.app.AlertDialog.Builder(this)
            .setTitle("Cancel Request")
            .setMessage("Cancel your chat request to ${request.toName}?")
            .setPositiveButton("Cancel Request") { _, _ ->
                Firebase.firestore.collection("matchRequests")
                    .document(request.id)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Request cancelled",
                            Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to cancel request",
                            Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Keep", null)
            .show()
    }
}