/*
 * Copyright (c) 2024 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.settings

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.lifecycle.Observer
import androidx.preference.Preference
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import me.zhanghai.android.files.R
import me.zhanghai.android.files.file.FileAssociation
import me.zhanghai.android.files.util.createIntent
import me.zhanghai.android.files.util.valueCompat

class FileAssociationsPreference : Preference {
    private val observer = Observer<List<FileAssociation>> { onFileAssociationsChanged(it) }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, @AttrRes defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    )

    constructor(
        context: Context,
        attrs: AttributeSet?,
        @AttrRes defStyleAttr: Int,
        @StyleRes defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    init {
        isPersistent = false
    }

    override fun onAttached() {
        super.onAttached()
        Settings.FILE_ASSOCIATIONS.observeForever(observer)
    }

    override fun onDetached() {
        super.onDetached()
        Settings.FILE_ASSOCIATIONS.removeObserver(observer)
    }

    private fun onFileAssociationsChanged(fileAssociations: List<FileAssociation>) {
        summary = if (fileAssociations.isNotEmpty()) {
            fileAssociations.joinToString(", ") { it.extension }
        } else {
            context.getString(R.string.settings_file_associations_empty)
        }
    }

    override fun onClick() {
        context.startActivity(FileAssociationListActivity::class.createIntent())
    }
}
