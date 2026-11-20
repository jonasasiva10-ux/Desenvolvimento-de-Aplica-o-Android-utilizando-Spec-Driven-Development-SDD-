package com.example.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.data.local.dao.GameDao
import com.example.data.local.dao.PatchNoteDao
import com.example.data.local.dao.PlayerDao
import com.example.data.local.dao.SyncStatusDao
import com.example.data.local.entity.GameRankEntity
import com.example.data.local.entity.PatchNoteEntity
import com.example.data.local.entity.PlayerMatchEntity
import com.example.data.local.entity.PlayerStatsEntity
import com.example.data.local.entity.ProPlayerEntity
import com.example.data.local.entity.SyncStatusEntity

@Database(
    entities = [
        GameRankEntity::class,
        PatchNoteEntity::class,
        ProPlayerEntity::class,
        PlayerStatsEntity::class,
        PlayerMatchEntity::class,
        SyncStatusEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class PatchPulseDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
    abstract fun patchNoteDao(): PatchNoteDao
    abstract fun playerDao(): PlayerDao
    abstract fun syncStatusDao(): SyncStatusDao

    companion object {
        const val DATABASE_NAME = "patchpulse_offline.db"
    }
}
