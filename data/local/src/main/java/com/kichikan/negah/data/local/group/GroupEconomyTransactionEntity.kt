package com.kichikan.negah.data.local.group

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kichikan.negah.domain.group.GroupEconomyCurrency
import com.kichikan.negah.domain.group.GroupEconomyTransaction

@Entity(
    tableName = "group_economy_transactions",
    foreignKeys = [
        ForeignKey(
            entity = MemberGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("groupId"), Index("createdAtEpochMillis")],
)
data class GroupEconomyTransactionEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val currency: String,
    val amountDelta: Long,
    val reason: String,
    val createdAtEpochMillis: Long,
) {
    fun toDomain() = GroupEconomyTransaction(
        id = id,
        groupId = groupId,
        currency = GroupEconomyCurrency.valueOf(currency),
        amountDelta = amountDelta,
        reason = reason,
        createdAtEpochMillis = createdAtEpochMillis,
    )

    companion object {
        fun fromDomain(transaction: GroupEconomyTransaction) = GroupEconomyTransactionEntity(
            id = transaction.id,
            groupId = transaction.groupId,
            currency = transaction.currency.name,
            amountDelta = transaction.amountDelta,
            reason = transaction.reason,
            createdAtEpochMillis = transaction.createdAtEpochMillis,
        )
    }
}
