package io.github.raddadz.ntfyforwarder

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LogEntryDao {
    @Query("SELECT * FROM log_entries ORDER BY timestamp DESC LIMIT 200")
    fun getAll(): Flow<List<LogEntryEntity>>

    @Query("SELECT * FROM log_entries WHERE status = 'failed' ORDER BY timestamp DESC")
    fun getFailedEntries(): Flow<List<LogEntryEntity>>

    @Insert
    suspend fun insert(entry: LogEntryEntity): Long

    @Query("DELETE FROM log_entries WHERE id NOT IN (SELECT id FROM log_entries ORDER BY timestamp DESC LIMIT 200)")
    suspend fun pruneOld()

    @Query("DELETE FROM log_entries")
    suspend fun deleteAll()

    @Query("DELETE FROM log_entries WHERE timestamp < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)

    @Query("DELETE FROM log_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE log_entries SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)
}
