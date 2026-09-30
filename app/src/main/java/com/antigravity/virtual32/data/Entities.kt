package com.antigravity.virtual32.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.ColumnInfo

@Entity(tableName = "batches")
data class Batch(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val source: String, // "ESP", "SIM", "GALLERY"
    val photoPath: String?,
    val galleryUri: String?,
    val status: String,
    val provider: String,
    val model: String,
    val promptName: String,
    val promptHash: String,
    val latencyMs: Long,
    val rawResponse: String?,
    val superseded: Boolean = false
)

@Entity(
    tableName = "answers",
    foreignKeys = [
        ForeignKey(
            entity = Batch::class,
            parentColumns = ["id"],
            childColumns = ["batchId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["batchId"])]
)
data class AnswerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val batchId: Long,
    val q: Int,
    val choice: String,
    val conf: String,
    val edited: Boolean = false,
    val note: String? = null
)

@Entity(tableName = "cycle_state")
data class CycleState(
    @PrimaryKey val id: Int = 1,
    val activeBatchIds: String, // comma separated list
    val cursor: Int = 0
)
