/*
 * Copyright (c) 2026 eetufin92 <eetufin92@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.file.cache

import android.os.Parcelable
import java8.nio.file.Path
import me.zhanghai.android.files.file.FileItem
import me.zhanghai.android.files.file.MimeType
import me.zhanghai.android.files.file.asMimeType
import me.zhanghai.android.files.file.lastModifiedInstant
import me.zhanghai.android.files.filelist.getCollationKeyForFileName
import me.zhanghai.android.files.filelist.name
import me.zhanghai.android.files.provider.common.AbstractBasicFileAttributes
import me.zhanghai.android.files.provider.common.BasicFileType
import me.zhanghai.android.files.settings.Settings
import me.zhanghai.android.files.util.valueCompat
import java.text.Collator
import java8.nio.file.attribute.FileTime
import java.time.Instant

fun FileItem.toEntity(serverId: Long): FileCacheEntity =
    FileCacheEntity(
        serverId = serverId,
        path = path.toString(),
        parentPath = path.parent?.toString() ?: "",
        name = path.fileName?.toString() ?: "",
        size = attributes.size(),
        lastModifiedTime = attributes.lastModifiedInstant.toEpochMilli(),
        isDirectory = attributes.isDirectory,
        isHidden = isHidden,
        mimeType = mimeType.value
    )

fun FileCacheEntity.toFileItem(referencePath: Path): FileItem {
    val path = referencePath.fileSystem.getPath(this.path)
    val nameCollationKey = Collator.getInstance().getCollationKeyForFileName(path.name)
    val lastModifiedFileTime = FileTime.from(Instant.ofEpochMilli(this.lastModifiedTime))
    val attributes = FileCacheAttributes(
        lastModifiedTime = lastModifiedFileTime,
        lastAccessTime = lastModifiedFileTime,
        creationTime = lastModifiedFileTime,
        type = if (isDirectory) BasicFileType.DIRECTORY else BasicFileType.REGULAR_FILE,
        size = this.size,
        fileKey = path as Parcelable
    )
    return FileItem(
        path = path,
        nameCollationKey = nameCollationKey,
        attributesNoFollowLinks = attributes,
        symbolicLinkTarget = null,
        symbolicLinkTargetAttributes = null,
        isHidden = this.isHidden,
        mimeType = mimeType.asMimeType(),
        isVerified = false
    )
}

fun Path.getServerId(): Long? {
    val storages = Settings.STORAGES.valueCompat
    for (storage in storages) {
        val storagePath = storage.path ?: continue
        if (this.startsWith(storagePath)) {
            return storage.id
        }
    }
    return null
}

fun Path.isSensitivePath(): Boolean {
    val pathString = toString()
    // Exclude hidden security-related directories
    if (pathString.contains("/.ssh") || 
        pathString.contains("/.gnupg") || 
        pathString.contains("/.aws") ||
        pathString.contains("/.config")) {
        return true
    }
    // Exclude virtual paths inside archives (already handled by ArchiveFileSystemProvider)
    if (pathString.startsWith("archive:")) {
        return true
    }
    return false
}
