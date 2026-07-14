/*
 * Copyright (c) 2026 eetufin92 <eetufin92@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.file.cache

import android.os.Parcelable
import java8.nio.file.attribute.FileTime
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.WriteWith
import me.zhanghai.android.files.provider.common.AbstractBasicFileAttributes
import me.zhanghai.android.files.provider.common.BasicFileType
import me.zhanghai.android.files.provider.common.FileTimeParceler
import me.zhanghai.android.files.util.ParcelableParceler

@Parcelize
data class FileCacheAttributes(
    override val lastModifiedTime: @WriteWith<FileTimeParceler> FileTime,
    override val lastAccessTime: @WriteWith<FileTimeParceler> FileTime,
    override val creationTime: @WriteWith<FileTimeParceler> FileTime,
    override val type: BasicFileType,
    override val size: Long,
    override val fileKey: @WriteWith<ParcelableParceler> Parcelable
) : AbstractBasicFileAttributes()
