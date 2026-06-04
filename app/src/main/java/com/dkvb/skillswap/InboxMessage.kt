package com.dkvb.skillswap

data class InboxMessage(
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: Long = 0,
    val isUnread: Boolean = false
)