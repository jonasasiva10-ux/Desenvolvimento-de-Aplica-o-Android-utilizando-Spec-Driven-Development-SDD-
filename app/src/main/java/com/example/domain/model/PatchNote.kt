package com.example.domain.model

data class PatchNote(
    val gid: String,
    val appId: Int,
    val gameName: String,
    val title: String,
    val summary: String,
    val fullContent: String,
    val url: String,
    val author: String,
    val feedLabel: String,
    val publishedAtEpochSeconds: Long,
    val isOfficialPatch: Boolean,
)
