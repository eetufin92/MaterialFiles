/*
 * Copyright (c) 2026 eetufin92 <eetufin92@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.file.cache

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "file_cache",
    indices = [
        Index(value = ["path"], unique = true),
        Index(value = ["parent_path"]),
        Index(value = ["server_id"])
    ]
)
data class FileCacheEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "server_id")
    val serverId: Long,
    @ColumnInfo(name = "path")
    val path: String,
    @ColumnInfo(name = "parent_path")
    val parentPath: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "size")
    val size: Long,
    @ColumnInfo(name = "last_modified_time")
    val lastModifiedTime: Long,
    @ColumnInfo(name = "is_directory")
    val isDirectory: Boolean,
    @ColumnInfo(name = "is_hidden")
    val isHidden: Boolean,
    @ColumnInfo(name = "mime_type")
    val mimeType: String
)

@Fts4(
    contentEntity = FileCacheEntity::class,
    tokenizer = "unicode61",
    tokenizerArgs = ["tokenchars=_-"]
)
@Entity(tableName = "file_search")
data class FileSearchEntity(
    @ColumnInfo(name = "name")
    val name: String
)
