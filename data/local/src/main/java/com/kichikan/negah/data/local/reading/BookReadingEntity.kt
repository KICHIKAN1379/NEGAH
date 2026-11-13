package com.kichikan.negah.data.local.reading

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kichikan.negah.data.local.member.MemberEntity
import com.kichikan.negah.domain.reading.BookReading

@Entity(
    tableName = "book_readings",
    foreignKeys = [
        ForeignKey(
            entity = MemberEntity::class,
            parentColumns = ["id"],
            childColumns = ["memberId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("memberId"), Index("readAtEpochMillis")],
)
data class BookReadingEntity(
    @PrimaryKey val id: String,
    val memberId: String,
    val title: String,
    val readAtEpochMillis: Long,
    val level: Int,
    val createdAtEpochMillis: Long,
) {
    fun toDomain() = BookReading(id, memberId, title, readAtEpochMillis, level, createdAtEpochMillis)

    companion object {
        fun fromDomain(reading: BookReading) = BookReadingEntity(
            reading.id, reading.memberId, reading.title, reading.readAtEpochMillis,
            reading.level, reading.createdAtEpochMillis,
        )
    }
}
