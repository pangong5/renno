package com.example.data

import kotlinx.coroutines.flow.Flow

class SoundRecordRepository(private val soundRecordDao: SoundRecordDao) {
    val allRecords: Flow<List<SoundMeasurementRecord>> = soundRecordDao.getAllRecords()
    val recordCount: Flow<Int> = soundRecordDao.getRecordCount()
    val maxDbEver: Flow<Float?> = soundRecordDao.getMaxDbEver()

    fun getRecordsByTag(tag: String): Flow<List<SoundMeasurementRecord>> {
        return if (tag == "Semua") {
            soundRecordDao.getAllRecords()
        } else {
            soundRecordDao.getRecordsByTag(tag)
        }
    }

    fun getRecordById(id: Long): Flow<SoundMeasurementRecord?> = soundRecordDao.getRecordById(id)

    suspend fun insert(record: SoundMeasurementRecord): Long = soundRecordDao.insertRecord(record)

    suspend fun delete(record: SoundMeasurementRecord) = soundRecordDao.deleteRecord(record)

    suspend fun deleteById(id: Long) = soundRecordDao.deleteById(id)

    suspend fun clearAll() = soundRecordDao.clearAll()
}
