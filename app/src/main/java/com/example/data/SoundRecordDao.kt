package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SoundRecordDao {
    @Query("SELECT * FROM sound_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<SoundMeasurementRecord>>

    @Query("SELECT * FROM sound_records WHERE locationTag = :tag ORDER BY timestamp DESC")
    fun getRecordsByTag(tag: String): Flow<List<SoundMeasurementRecord>>

    @Query("SELECT * FROM sound_records WHERE id = :id LIMIT 1")
    fun getRecordById(id: Long): Flow<SoundMeasurementRecord?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: SoundMeasurementRecord): Long

    @Delete
    suspend fun deleteRecord(record: SoundMeasurementRecord)

    @Query("DELETE FROM sound_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM sound_records")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM sound_records")
    fun getRecordCount(): Flow<Int>

    @Query("SELECT MAX(maxDb) FROM sound_records")
    fun getMaxDbEver(): Flow<Float?>
}
