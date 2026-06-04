package com.dkvb.skillswap

data class User(
    val uid: String = "",
    val name: String = "",
    val username: String = "",
    val email: String = "",
    val bio: String = "",
    val skillsToTeach: List<String> = emptyList(),
    val skillsToLearn: List<String> = emptyList(),
    val createdAt: Long = 0
)