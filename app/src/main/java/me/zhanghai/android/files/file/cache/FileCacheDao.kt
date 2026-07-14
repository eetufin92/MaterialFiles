/*
 * Copyright (c) 2026 eetufin92 <eetufin92@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.file.cache

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface FileCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<FileCacheEntity>)

    @Query("DELETE FROM file_cache WHERE server_id = :serverId AND parent_path = :parentPath")
    suspend fun deleteByParentPath(serverId: Long, parentPath: String)

    @Transaction
    suspend fun replaceDirectory(serverId: Long, parentPath: String, entities: List<FileCacheEntity>) {
        deleteByParentPath(serverId, parentPath)
        insertAll(entities)
    }

    @Query("SELECT * FROM file_cache WHERE path = :path")
    suspend fun getByPath(path: String): FileCacheEntity?

    @Query("DELETE FROM file_cache WHERE server_id = :serverId")
    suspend fun deleteByServerId(serverId: Long)

    @Query("DELETE FROM file_cache WHERE path = :path")
    suspend fun deleteByPath(path: String)

    @Query("""
        SELECT fc.* FROM file_cache AS fc
        JOIN file_search AS fs ON fc.id = fs.rowid
        WHERE fs.name MATCH :query
        AND fc.server_id = :serverId
        AND fc.path LIKE :pathPrefix || '%'
    """)
    suspend fun searchByName(serverId: Long, query: String, pathPrefix: String): List<FileCacheEntity>

    @Query("DELETE FROM file_cache")
    suspend fun clear()
}
