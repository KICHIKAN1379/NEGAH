package com.kichikan.negah.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kichikan.negah.data.local.attendance.AttendanceEntity
import com.kichikan.negah.data.local.commitment.CommitmentEntity
import com.kichikan.negah.data.local.commitment.CommitmentRevisionEntity
import com.kichikan.negah.data.local.economy.EconomyTransactionEntity
import com.kichikan.negah.data.local.member.MemberEntity
import com.kichikan.negah.data.local.session.SessionEntity

@Database(
    entities = [
        MemberEntity::class,
        SessionEntity::class,
        AttendanceEntity::class,
        CommitmentEntity::class,
        CommitmentRevisionEntity::class,
        EconomyTransactionEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class NegahDatabase : RoomDatabase()
