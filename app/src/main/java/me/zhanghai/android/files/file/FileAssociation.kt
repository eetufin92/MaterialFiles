/*
 * Copyright (c) 2024 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.file

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class FileAssociation(
    val extension: String,
    val componentName: String?
) : Parcelable
