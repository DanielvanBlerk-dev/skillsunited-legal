package com.dkvb.skillswap

import android.animation.ObjectAnimator
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.FrameLayout

class NotificationBanner(private val context: Context) {

    private var bannerView: View? = null

    fun show(
        container: FrameLayout,
        senderName: String,
        messagePreview: String,
        onAccept: () -> Unit,
        onDismiss: () -> Unit
    ) {
        // Dismiss any existing banner first
        dismiss(container)

        val view = LayoutInflater.from(context)
            .inflate(R.layout.view_notification_banner, container, false)

        view.findViewById<TextView>(R.id.tvBannerTitle).text = "New message from $senderName"
        view.findViewById<TextView>(R.id.tvBannerMessage).text = messagePreview

        view.findViewById<Button>(R.id.btnAccept).setOnClickListener {
            onAccept()
            dismiss(container)
        }

        view.findViewById<Button>(R.id.btnDismiss).setOnClickListener {
            onDismiss()
            dismiss(container)
        }

        container.addView(view)
        bannerView = view

        // Slide in from top
        view.translationY = -300f
        ObjectAnimator.ofFloat(view, "translationY", -300f, 0f).apply {
            duration = 300
            start()
        }

        // Auto dismiss after 6 seconds
        view.postDelayed({ dismiss(container) }, 6000)
    }

    fun dismiss(container: FrameLayout) {
        bannerView?.let { view ->
            ObjectAnimator.ofFloat(view, "translationY", 0f, -300f).apply {
                duration = 300
                start()
            }
            view.postDelayed({ container.removeView(view) }, 300)
            bannerView = null
        }
    }
}