package com.kichikan.negah.data.local.economy

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kichikan.negah.data.local.member.MemberEntity

@Entity(
    tableName = "economy_transactions",
    foreignKeys = [ForeignKey(entity = MemberEntity::class, parentColumns = ["id"], childColumns = ["memberId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index("memberId"), Index("createdAtEpochMillis")],
)
data class EconomyTransactionEntity(
    @PrimaryKey val id: String,
    val memberId: String,
    val currency: String,
    val amount: Long,
    val transactionType: String,
    val sourceType: String,
    val sourceId: String?,
    val createdAtEpochMillis: Long,
)
