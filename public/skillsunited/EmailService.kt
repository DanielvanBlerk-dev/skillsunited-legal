package com.dkvb.skillswap

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

object EmailService {
    private const val EMAILJS_SERVICE_ID = BuildConfig.EMAILJS_SERVICE_ID
    private const val EMAILJS_TEMPLATE_ID = BuildConfig.EMAILJS_TEMPLATE_ID
    private const val EMAILJS_PUBLIC_KEY = BuildConfig.EMAILJS_PUBLIC_KEY
    private const val EMAILJS_URL = "https://api.emailjs.com/api/v1.0/email/send"
    private val client = OkHttpClient()

    fun sendReportEmail(
        reportedName: String,
        reportedUid: String,
        reporterUid: String,
        reason: String,
        timestamp: String,
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        val json = JSONObject().apply {
            put("service_id", EMAILJS_SERVICE_ID)
            put("template_id", EMAILJS_TEMPLATE_ID)
            put("user_id", EMAILJS_PUBLIC_KEY)
            put("template_params", JSONObject().apply {
                put("reported_name", reportedName)
                put("reported_uid", reportedUid)
                put("reporter_uid", reporterUid)
                put("reason", reason)
                put("timestamp", timestamp)
            })
        }

        val body = json.toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(EMAILJS_URL)
            .post(body)
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("EmailService", "Failed to send report email: ${e.message}")
                onFailure(e.message ?: "Unknown error")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    Log.d("EmailService", "Report email sent successfully")
                    onSuccess()
                } else {
                    Log.e("EmailService", "Email failed: ${response.code} ${response.body?.string()}")
                    onFailure("Email failed with code ${response.code}")
                }
            }
        })
    }
}