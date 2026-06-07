package com.fenlight.companion.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RdDeviceCode(
    @Json(name = "device_code") val deviceCode: String,
    @Json(name = "user_code") val userCode: String,
    @Json(name = "verification_url") val verificationUrl: String,
    @Json(name = "direct_verification_url") val directVerificationUrl: String? = null,
    @Json(name = "expires_in") val expiresIn: Int,
    val interval: Int,
)

@JsonClass(generateAdapter = true)
data class RdCredentials(
    @Json(name = "client_id") val clientId: String,
    @Json(name = "client_secret") val clientSecret: String,
)

@JsonClass(generateAdapter = true)
data class RdToken(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "refresh_token") val refreshToken: String,
    @Json(name = "expires_in") val expiresIn: Long,
    @Json(name = "token_type") val tokenType: String,
)

@JsonClass(generateAdapter = true)
data class RdTorrent(
    val id: String,
    val filename: String,
    val hash: String,
    val bytes: Long,
    val status: String,
    val progress: Int,
    val links: List<String>,
    val added: String,
)

@JsonClass(generateAdapter = true)
data class RdTorrentInfo(
    val id: String,
    val filename: String,
    val hash: String,
    val bytes: Long,
    val status: String,
    val files: List<RdFile>,
    val links: List<String>,
)

@JsonClass(generateAdapter = true)
data class RdFile(
    val id: Int,
    val path: String,
    val bytes: Long,
    val selected: Int,
)

@JsonClass(generateAdapter = true)
data class RdDownload(
    val id: String,
    val filename: String,
    val mimeType: String?,
    val filesize: Long,
    val link: String,
    val download: String,
    val generated: String,
)

@JsonClass(generateAdapter = true)
data class RdUnrestrict(
    val id: String,
    val filename: String,
    val mimeType: String,
    val filesize: Long,
    val link: String,
    val download: String,
    val streamable: Int,
)

@JsonClass(generateAdapter = true)
data class RdUser(
    val id: Long,
    val username: String,
    val email: String? = null,
)
