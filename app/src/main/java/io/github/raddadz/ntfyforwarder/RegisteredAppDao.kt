package io.github.raddadz.ntfyforwarder

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RegisteredAppDao {

    @Query("SELECT * FROM registered_apps ORDER BY label ASC")
    fun getAllFlow(): Flow<List<RegisteredAppEntity>>

    @Query("SELECT * FROM registered_apps WHERE enabled = 1 ORDER BY label ASC")
    fun getEnabledFlow(): Flow<List<RegisteredAppEntity>>

    @Query("SELECT * FROM registered_apps WHERE packageName = :pkg LIMIT 1")
    suspend fun getByPackage(pkg: String): RegisteredAppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: RegisteredAppEntity): Long

    @Update
    suspend fun update(app: RegisteredAppEntity)

    @Query("UPDATE registered_apps SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("DELETE FROM registered_apps WHERE id = :id")
    suspend fun delete(id: Long)
}
