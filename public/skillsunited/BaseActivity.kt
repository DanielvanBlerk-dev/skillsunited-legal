package com.dkvb.skillswap

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

open class BaseActivity : AppCompatActivity() {

    private var notificationBanner: NotificationBanner? = null
    private var bannerContainer: FrameLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.init(this)
    }

    override fun onResume() {
        super.onResume()
        applyThemeToRoot()
        applyStatusBarColor()
        setupSettingsButton()
        setupInboxButton()
        setupBannerContainer()
        setupMessageListener()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun setupBannerContainer() {
        android.util.Log.d("BannerDebug", "setupBannerContainer called in ${this::class.simpleName}")
        val rootView = findViewById<ViewGroup>(android.R.id.content)
            .getChildAt(0) as? FrameLayout
        android.util.Log.d("BannerDebug", "rootView type = ${rootView?.javaClass?.simpleName}")

        if (rootView == null) {
            android.util.Log.e("BannerDebug", "rootView is NOT a FrameLayout — banner container cannot be created")
            return
        }

        if (rootView.findViewById<FrameLayout>(R.id.baseBannerContainer) != null) {
            android.util.Log.d("BannerDebug", "Banner container already exists — reusing")
            bannerContainer = rootView.findViewById(R.id.baseBannerContainer)
            notificationBanner = NotificationBanner(this)
            return
        }

        val container = FrameLayout(this).apply {
            id = R.id.baseBannerContainer
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }
        rootView.addView(container)
        bannerContainer = container
        notificationBanner = NotificationBanner(this)
        android.util.Log.d("BannerDebug", "Banner container created successfully")
    }

    protected fun checkTermsAccepted(onAccepted: () -> Unit) {
        if (!ThemeManager.hasAcceptedTerms(this)) {
            TermsDialogHelper.showTermsDialog(
                context = this,
                onAccepted = onAccepted,
                onDeclined = {
                    Toast.makeText(
                        this,
                        "You must accept the Terms of Service to use Skills United",
                        Toast.LENGTH_LONG
                    ).show()
                    Firebase.auth.signOut()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finishAffinity()
                }
            )
        } else {
            onAccepted()
        }
    }

    /**
     * Checks the chatEnabled feature flag in Firestore before launching ChatActivity.
     * To disable chat: set config/features.chatEnabled = false in the Firebase Console.
     * To re-enable: set it back to true.
     */
    protected fun openChatIfEnabled(userId: String, userName: String) {
        Firebase.firestore.collection("config").document("features")
            .get()
            .addOnSuccessListener { doc ->
                val chatEnabled = doc.getBoolean("chatEnabled") ?: true
                if (chatEnabled) {
                    val intent = Intent(this, ChatActivity::class.java)
                    intent.putExtra("userId", userId)
                    intent.putExtra("userName", userName)
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Chat is temporarily unavailable.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                // If the flag can't be read, fail open so chat still works
                val intent = Intent(this, ChatActivity::class.java)
                intent.putExtra("userId", userId)
                intent.putExtra("userName", userName)
                startActivity(intent)
            }
    }

    private fun setupMessageListener() {
        android.util.Log.d("BannerDebug", "setupMessageListener called in ${this::class.simpleName}")
        android.util.Log.d("BannerDebug", "bannerContainer = $bannerContainer")

        val currentActivity = this

        // Match request banner
        MessageListenerService.onNewMatchRequest = { fromName ->
            android.util.Log.d("MatchRequest", "onNewMatchRequest received from $fromName")
            runOnUiThread {
                android.util.Log.d("MatchRequest", "bannerContainer=$bannerContainer")
                val container = bannerContainer ?: run {
                    android.util.Log.e("MatchRequest", "bannerContainer is NULL — cannot show banner")
                    return@runOnUiThread
                }
                val banner = notificationBanner ?: NotificationBanner(currentActivity)
                android.util.Log.d("MatchRequest", "Showing match request banner for $fromName")
                banner.show(
                    container = container,
                    senderName = fromName,
                    messagePreview = "wants to start a chat with you",
                    onAccept = {
                        android.util.Log.d("MatchRequest", "User accepted match request banner")
                        startActivity(Intent(currentActivity, MatchRequestActivity::class.java))
                    },
                    onDismiss = {
                        android.util.Log.d("MatchRequest", "User dismissed match request banner")
                    }
                )
            }
        }

        // New message banner
        MessageListenerService.onNewMessage = { senderId, senderName, message, chatId ->
            android.util.Log.d("BannerDebug", "onNewMessage fired! sender=$senderName message=$message")
            runOnUiThread {
                android.util.Log.d("BannerDebug", "Running on UI thread, container=$bannerContainer")
                val container = bannerContainer
                if (container == null) {
                    android.util.Log.e("BannerDebug", "bannerContainer is NULL — banner cannot show")
                    return@runOnUiThread
                }
                val banner = notificationBanner ?: NotificationBanner(currentActivity)
                android.util.Log.d("BannerDebug", "Showing banner for $senderName")
                banner.show(
                    container = container,
                    senderName = senderName,
                    messagePreview = message,
                    onAccept = {
                        openChatIfEnabled(senderId, senderName)
                    },
                    onDismiss = { }
                )
            }
        }

        MessageListenerService.startListening()
    }

    protected open fun setupSettingsButton() {
        val rootView = findViewById<ViewGroup>(android.R.id.content)
            .getChildAt(0) as? FrameLayout ?: return

        if (rootView.findViewById<ImageButton>(R.id.btnSettings) != null) {
            rootView.findViewById<ImageButton>(R.id.btnSettings)
                .setOnClickListener {
                    if (this !is SettingsActivity) {
                        startActivity(Intent(this, SettingsActivity::class.java))
                    }
                }
            return
        }

        if (rootView is FrameLayout) {
            val settingsBtn = ImageButton(this).apply {
                id = R.id.btnSettings
                setImageResource(android.R.drawable.ic_menu_preferences)
                val attrs = intArrayOf(android.R.attr.selectableItemBackgroundBorderless)
                val typedArray = obtainStyledAttributes(attrs)
                background = typedArray.getDrawable(0)
                typedArray.recycle()
                alpha = 0.5f
                contentDescription = "Settings"
            }
            val settingsParams = FrameLayout.LayoutParams(dpToPx(40), dpToPx(40)).apply {
                gravity = android.view.Gravity.TOP or android.view.Gravity.END
                topMargin = dpToPx(30)
                marginEnd = dpToPx(12)
            }
            rootView.addView(settingsBtn, settingsParams)
            settingsBtn.setOnClickListener {
                if (this !is SettingsActivity) {
                    startActivity(Intent(this, SettingsActivity::class.java))
                }
            }

            if (this is ChatActivity) {
                androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, insets ->
                    val statusBar = insets.getInsets(
                        androidx.core.view.WindowInsetsCompat.Type.statusBars()
                    ).top
                    val sParams = settingsBtn.layoutParams as FrameLayout.LayoutParams
                    sParams.topMargin = statusBar + dpToPx(12)
                    settingsBtn.layoutParams = sParams

                    rootView.findViewById<ImageButton>(R.id.btnInbox)?.let { inboxBtn ->
                        val iParams = inboxBtn.layoutParams as FrameLayout.LayoutParams
                        iParams.topMargin = statusBar + dpToPx(12)
                        inboxBtn.layoutParams = iParams
                    }
                    insets
                }
            }
        }
    }

    protected open fun setupInboxButton() {
        val rootView = findViewById<ViewGroup>(android.R.id.content)
            .getChildAt(0) as? FrameLayout ?: return

        MessageListenerService.onUnreadCountChanged = { count ->
            runOnUiThread { updateBadge(rootView, count) }
        }

        if (rootView.findViewById<ImageButton>(R.id.btnInbox) != null) {
            rootView.findViewById<ImageButton>(R.id.btnInbox)
                .setOnClickListener {
                    if (this !is InboxActivity) {
                        MessageListenerService.markAllRead()
                        updateBadge(rootView, 0)
                        startActivity(Intent(this, InboxActivity::class.java))
                    }
                }
            android.util.Log.d("BadgeDebug", "setupInboxButton called in ${this::class.simpleName}")
            android.util.Log.d("BadgeDebug", "current unreadCount=${MessageListenerService.unreadCount}")
            updateBadge(rootView, MessageListenerService.unreadCount)
            return
        }

        if (rootView is FrameLayout) {
            val inboxBtn = ImageButton(this).apply {
                id = R.id.btnInbox
                setImageResource(android.R.drawable.sym_action_chat)
                val attrs = intArrayOf(android.R.attr.selectableItemBackgroundBorderless)
                val typedArray = obtainStyledAttributes(attrs)
                background = typedArray.getDrawable(0)
                typedArray.recycle()
                alpha = 0.5f
                contentDescription = "Inbox"
            }

            val inboxParams = FrameLayout.LayoutParams(dpToPx(40), dpToPx(40)).apply {
                gravity = android.view.Gravity.TOP or android.view.Gravity.END
                topMargin = dpToPx(30)
                marginEnd = dpToPx(56)
            }
            rootView.addView(inboxBtn, inboxParams)

            val badge = TextView(this).apply {
                id = R.id.inboxBadge
                text = "!"
                textSize = 9f
                setTextColor(android.graphics.Color.WHITE)
                gravity = android.view.Gravity.CENTER
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(android.graphics.Color.RED)
                    setStroke(dpToPx(1), android.graphics.Color.WHITE)
                }
                visibility = View.GONE
            }

            val badgeParams = FrameLayout.LayoutParams(dpToPx(18), dpToPx(18)).apply {
                gravity = android.view.Gravity.TOP or android.view.Gravity.END
                topMargin = dpToPx(24)
                marginEnd = dpToPx(50)
            }
            rootView.addView(badge, badgeParams)

            inboxBtn.setOnClickListener {
                if (this !is InboxActivity) {
                    MessageListenerService.markAllRead()
                    updateBadge(rootView, 0)
                    startActivity(Intent(this, InboxActivity::class.java))
                }
            }

            if (this is ChatActivity) {
                androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, insets ->
                    val statusBar = insets.getInsets(
                        androidx.core.view.WindowInsetsCompat.Type.statusBars()
                    ).top
                    val iParams = inboxBtn.layoutParams as FrameLayout.LayoutParams
                    iParams.topMargin = statusBar + dpToPx(12)
                    inboxBtn.layoutParams = iParams

                    val bParams = badge.layoutParams as FrameLayout.LayoutParams
                    bParams.topMargin = statusBar + dpToPx(8)
                    badge.layoutParams = bParams

                    rootView.findViewById<ImageButton>(R.id.btnSettings)?.let { settingsBtn ->
                        val sParams = settingsBtn.layoutParams as FrameLayout.LayoutParams
                        sParams.topMargin = statusBar + dpToPx(12)
                        settingsBtn.layoutParams = sParams
                    }
                    insets
                }
            }

            updateBadge(rootView, MessageListenerService.unreadCount)
        }
    }

    private fun updateBadge(rootView: FrameLayout, count: Int) {
        android.util.Log.d("BadgeDebug", "updateBadge called count=$count")
        val badge = rootView.findViewById<TextView>(R.id.inboxBadge)
        android.util.Log.d("BadgeDebug", "badge view found=${badge != null}")
        if (badge == null) return
        if (count > 0) {
            badge.visibility = View.VISIBLE
            badge.text = if (count > 9) "9+" else count.toString()
            android.util.Log.d("BadgeDebug", "badge shown with count=$count")
        } else {
            badge.visibility = View.GONE
            android.util.Log.d("BadgeDebug", "badge hidden")
        }
    }

    protected fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    fun applyThemeToRoot() {
        val root = findViewById<ViewGroup>(android.R.id.content)
        applyThemeToView(root)
    }

    private fun applyThemeToView(view: View) {
        try {
            when (view) {
                is ViewGroup -> {
                    when (view) {
                        is ScrollView -> view.setBackgroundColor(
                            ThemeManager.parseColor(ThemeManager.getBackground()))
                        is RecyclerView -> view.setBackgroundColor(
                            ThemeManager.parseColor(ThemeManager.getBackground()))
                        is CardView -> view.setCardBackgroundColor(
                            ThemeManager.parseColor(ThemeManager.getCardBackground()))
                        else -> {
                            if (view.background != null) {
                                view.setBackgroundColor(
                                    ThemeManager.parseColor(ThemeManager.getSurface()))
                            }
                        }
                    }
                    for (i in 0 until view.childCount) {
                        applyThemeToView(view.getChildAt(i))
                    }
                }
                is ImageButton -> { }
                is Button -> {
                    val parent = view.parent
                    if (parent !is RecyclerView && parent?.parent !is RecyclerView) {
                        view.setBackgroundColor(
                            ThemeManager.parseColor(ThemeManager.getPrimary()))
                        view.setTextColor(
                            ThemeManager.parseColor(ThemeManager.getButtonText()))
                    }
                }
                is EditText -> {
                    view.setTextColor(
                        ThemeManager.parseColor(ThemeManager.getTextPrimary()))
                    view.setHintTextColor(
                        ThemeManager.parseColor(ThemeManager.getTextSecondary()))
                }
                is TextView -> {
                    view.setTextColor(
                        ThemeManager.parseColor(ThemeManager.getTextPrimary()))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    protected fun applyStatusBarColor() {
        val primaryColor = ThemeManager.parseColor(ThemeManager.getPrimary())
        @Suppress("DEPRECATION")
        window.statusBarColor = primaryColor
        val windowInsetsController = androidx.core.view.WindowCompat
            .getInsetsController(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = isColorLight(primaryColor)
    }

    private fun isColorLight(color: Int): Boolean {
        val r = android.graphics.Color.red(color)
        val g = android.graphics.Color.green(color)
        val b = android.graphics.Color.blue(color)
        val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255
        return luminance > 0.5
    }
}