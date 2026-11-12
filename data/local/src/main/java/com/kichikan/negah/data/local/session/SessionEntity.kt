package com.kichikan.negah.data.local.session

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kichikan.negah.domain.session.Session

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val sessionDateEpochDay: Long,
    val notes: String?,
    val createdAtEpochMillis: Long,
    val topic: String? = null,
    val location: String? = null,
    @ColumnInfo(defaultValue = "0") val isCompleted: Boolean = false,
) {
    fun toDomain() = Session(
        id = id,
        title = title,
        sessionDateEpochDay = sessionDateEpochDay,
        notes = notes,
        createdAtEpochMillis = createdAtEpochMillis,
        topic = topic,
        location = location,
        isCompleted = isCompleted,
    )

    companion object {
        fun fromDomain(session: Session) = SessionEntity(
            id = session.id,
            title = session.title,
            sessionDateEpochDay = session.sessionDateEpochDay,
            notes = session.notes,
            createdAtEpochMillis = session.createdAtEpochMillis,
            topic = session.topic,
            location = session.location,
            isCompleted = session.isCompleted,
        )
    }
}
