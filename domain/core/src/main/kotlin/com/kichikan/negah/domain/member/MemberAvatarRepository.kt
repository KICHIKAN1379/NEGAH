package com.kichikan.negah.domain.member

import java.io.InputStream

interface MemberAvatarRepository {
    suspend fun saveAvatar(memberId: String, input: InputStream, mimeType: String)
    suspend fun loadAvatar(memberId: String): ByteArray?
}
