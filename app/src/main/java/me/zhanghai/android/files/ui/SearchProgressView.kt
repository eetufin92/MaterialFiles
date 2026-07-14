/*
 * Copyright (c) 2026 eetufin92 <eetufin92@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.view.isVisible
import me.zhanghai.android.files.databinding.SearchProgressViewBinding
import me.zhanghai.android.files.util.getQuantityString

class SearchProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {
    private val binding = SearchProgressViewBinding.inflate(LayoutInflater.from(context), this)

    fun show(message: CharSequence, count: Int? = null) {
        binding.text.text = message
        binding.countText.text = count?.let { context.getQuantityString(me.zhanghai.android.files.R.plurals.search_result_count_format, it, it) } ?: ""
        isVisible = true
    }

    fun show(messageRes: Int, count: Int? = null) {
        show(context.getString(messageRes), count)
    }

    fun hide() {
        isVisible = false
    }
}
