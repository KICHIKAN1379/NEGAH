package com.kichikan.negah.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kichikan.negah.data.local.attendance.AttendanceDao
import com.kichikan.negah.data.local.attendance.AttendanceEntity
import com.kichikan.negah.data.local.attendance.AttendanceStatusConverter
import com.kichikan.negah.data.local.commitment.CommitmentDao
import com.kichikan.negah.data.local.commitment.CommitmentEntity
import com.kichikan.negah.data.local.commitment.CommitmentRevisionEntity
import com.kichikan.negah.data.local.economy.EconomyTransactionDao
import com.kichikan.negah.data.local.economy.EconomyTransactionEntity
import com.kichikan.negah.data.local.member.MemberDao
import com.kichikan.negah.data.local.member.MemberEntity
import com.kichikan.negah.data.local.session.SessionDao
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
@TypeConverters(AttendanceStatusConverter::class)
abstract class NegahDatabase : RoomDatabase() {
    abstract fun memberDao(): MemberDao
    abstract fun sessionDao(): SessionDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun commitmentDao(): CommitmentDao
    abstract fun economyTransactionDao(): EconomyTransactionDao
}
