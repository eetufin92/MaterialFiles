/*
 * Copyright (c) 2026 eetufin92 <eetufin92@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.provider.common.search

import java.time.LocalDate
import java.time.ZoneId

sealed class SearchFilter {
    abstract fun accept(name: String, size: Long, lastModifiedTime: Long): Boolean

    class Name(val text: String, val positive: Boolean) : SearchFilter() {
        override fun accept(name: String, size: Long, lastModifiedTime: Long): Boolean {
            val contains = name.contains(text, ignoreCase = true)
            return if (positive) contains else !contains
        }
    }

    class Size(val size: Long, val greater: Boolean) : SearchFilter() {
        override fun accept(name: String, size: Long, lastModifiedTime: Long): Boolean {
            return if (greater) size > this.size else size < this.size
        }
    }

    class Date(val timeMillis: Long, val greater: Boolean) : SearchFilter() {
        override fun accept(name: String, size: Long, lastModifiedTime: Long): Boolean {
            return if (greater) lastModifiedTime > timeMillis else lastModifiedTime < timeMillis
        }
    }

    companion object {
        private val DATE_FILTER_REGEX = Regex("date(:?[<>])(.+)", RegexOption.IGNORE_CASE)
        private val SIZE_FILTER_REGEX = Regex("size(:?[<>])(.+)", RegexOption.IGNORE_CASE)

        fun parseQuery(query: String): List<SearchFilter> {
            val allTerms = query.split(Regex("\\s+")).filter { it.isNotEmpty() }
            return allTerms.mapNotNull { term ->
                val dateMatch = DATE_FILTER_REGEX.matchEntire(term)
                if (dateMatch != null) {
                    val operator = dateMatch.groupValues[1]
                    val value = dateMatch.groupValues[2]
                    val timeMillis = parseDate(value)
                    if (timeMillis != null) {
                        return@mapNotNull Date(timeMillis, operator.endsWith(">"))
                    }
                }
                val sizeMatch = SIZE_FILTER_REGEX.matchEntire(term)
                if (sizeMatch != null) {
                    val operator = sizeMatch.groupValues[1]
                    val value = sizeMatch.groupValues[2]
                    val size = parseSize(value)
                    if (size != null) {
                        return@mapNotNull Size(size, operator.endsWith(">"))
                    }
                }
                if (term.startsWith("not:", true)) {
                    val name = term.substring(4)
                    if (name.isNotEmpty()) {
                        return@mapNotNull Name(name, false)
                    }
                }
                Name(term, true)
            }
        }

        private fun parseDate(dateStr: String): Long? {
            val parts = dateStr.split("-")
            if (parts.isEmpty() || parts.size > 3) return null
            val year = parts[0].toIntOrNull() ?: return null
            val month = if (parts.size >= 2) parts[1].toIntOrNull() ?: return null else 1
            val day = if (parts.size >= 3) parts[2].toIntOrNull() ?: return null else 1
            return try {
                LocalDate.of(year, month, day)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
            } catch (e: Exception) {
                null
            }
        }

        private fun parseSize(sizeStr: String): Long? {
            val match = Regex("(\\d+(?:\\.\\d+)?)\\s*([a-zA-Z]*)").matchEntire(sizeStr) ?: return null
            val value = match.groupValues[1].toDouble()
            val unit = match.groupValues[2].uppercase()
            val factor = when (unit) {
                "K", "KB" -> 1024L
                "M", "MB" -> 1024L * 1024L
                "G", "GB" -> 1024L * 1024L * 1024L
                "T", "TB" -> 1024L * 1024L * 1024L * 1024L
                "P", "PB" -> 1024L * 1024L * 1024L * 1024L * 1024L
                "", "B" -> 1L
                else -> return null
            }
            return (value * factor).toLong()
        }
    }
}
