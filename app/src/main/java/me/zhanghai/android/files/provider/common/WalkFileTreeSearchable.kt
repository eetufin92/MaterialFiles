/*
 * Copyright (c) 2019 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.provider.common

import android.util.Log
import java.io.IOException
import java.io.InterruptedIOException
import java.time.LocalDate
import java.time.ZoneId
import java8.nio.file.DirectoryIteratorException
import java8.nio.file.FileVisitOption
import java8.nio.file.FileVisitResult
import java8.nio.file.FileVisitor
import java8.nio.file.Files
import java8.nio.file.LinkOption
import java8.nio.file.Path
import java8.nio.file.attribute.BasicFileAttributes

object WalkFileTreeSearchable {
    private val TAG = WalkFileTreeSearchable::class.java.simpleName

    private val DATE_FILTER_REGEX = Regex("date(:?[<>])(.+)", RegexOption.IGNORE_CASE)
    private val SIZE_FILTER_REGEX = Regex("size(:?[<>])(.+)", RegexOption.IGNORE_CASE)

    @Throws(IOException::class)
    fun search(
        directory: Path,
        query: String,
        intervalMillis: Long,
        listener: (List<Path>) -> Unit
    ) {
        val allTerms = query.split(Regex("\\s+")).filter { it.isNotEmpty() }
        val filters = allTerms.mapNotNull { term ->
            val dateMatch = DATE_FILTER_REGEX.matchEntire(term)
            if (dateMatch != null) {
                val operator = dateMatch.groupValues[1]
                val value = dateMatch.groupValues[2]
                val timeMillis = parseDate(value)
                if (timeMillis != null) {
                    return@mapNotNull DateFilter(timeMillis, operator.endsWith(">"))
                }
            }
            val sizeMatch = SIZE_FILTER_REGEX.matchEntire(term)
            if (sizeMatch != null) {
                val operator = sizeMatch.groupValues[1]
                val value = sizeMatch.groupValues[2]
                val size = parseSize(value)
                if (size != null) {
                    return@mapNotNull SizeFilter(size, operator.endsWith(">"))
                }
            }
            if (term.startsWith("not:", true)) {
                val name = term.substring(4)
                if (name.isNotEmpty()) {
                    return@mapNotNull NameFilter(name, false)
                }
            }
            NameFilter(term, true)
        }
        val paths = mutableListOf<Path>()
        // We cannot use Files.find() or Files.walk() because it cannot ignore exceptions.
        walkFileTreeForSearch(directory, object : FileVisitor<Path> {
            private var lastProgressMillis = System.currentTimeMillis()

            @Throws(InterruptedIOException::class)
            override fun preVisitDirectory(
                directory: Path,
                attributes: BasicFileAttributes
            ): FileVisitResult {
                visit(directory, attributes)
                throwIfInterrupted()
                return FileVisitResult.CONTINUE
            }

            @Throws(InterruptedIOException::class)
            override fun visitFile(file: Path, attributes: BasicFileAttributes): FileVisitResult {
                visit(file, attributes)
                throwIfInterrupted()
                return FileVisitResult.CONTINUE
            }

            @Throws(InterruptedIOException::class)
            override fun visitFileFailed(file: Path, exception: IOException): FileVisitResult {
                if (exception is InterruptedIOException) {
                    throw exception
                }
                Log.e(TAG, "visitFileFailed for $file", exception)
                visit(file, null)
                throwIfInterrupted()
                return FileVisitResult.CONTINUE
            }

            @Throws(InterruptedIOException::class)
            override fun postVisitDirectory(
                directory: Path,
                exception: IOException?
            ): FileVisitResult {
                if (exception is InterruptedIOException) {
                    throw exception
                }
                if (exception != null) {
                    Log.e(TAG, "postVisitDirectory failed for $directory", exception)
                }
                throwIfInterrupted()
                return FileVisitResult.CONTINUE
            }

            private fun visit(path: Path, attributes: BasicFileAttributes?) {
                // Exclude the directory being searched.
                if (path == directory) {
                    return
                }
                if (filters.all { it.accept(path, attributes) }) {
                    paths.add(path)
                }
                if (paths.isNotEmpty()) {
                    val currentTimeMillis = System.currentTimeMillis()
                    if (currentTimeMillis >= lastProgressMillis + intervalMillis) {
                        listener(paths.toList())
                        lastProgressMillis = currentTimeMillis
                        paths.clear()
                    }
                }
            }
        })
        if (paths.isNotEmpty()) {
            listener(paths.toList())
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

    private abstract class Filter {
        abstract fun accept(path: Path, attributes: BasicFileAttributes?): Boolean
    }

    private class NameFilter(val text: String, val positive: Boolean) : Filter() {
        override fun accept(path: Path, attributes: BasicFileAttributes?): Boolean {
            val fileName = path.fileName?.toString() ?: return false
            val contains = fileName.contains(text, ignoreCase = true)
            return if (positive) contains else !contains
        }
    }

    private class SizeFilter(val size: Long, val greater: Boolean) : Filter() {
        override fun accept(path: Path, attributes: BasicFileAttributes?): Boolean {
            val actualSize = attributes?.size() ?: return false
            return if (greater) actualSize > size else actualSize < size
        }
    }

    private class DateFilter(val timeMillis: Long, val greater: Boolean) : Filter() {
        override fun accept(path: Path, attributes: BasicFileAttributes?): Boolean {
            val lastModifiedTime = attributes?.lastModifiedTime()?.toMillis() ?: return false
            return if (greater) lastModifiedTime > timeMillis else lastModifiedTime < timeMillis
        }
    }

    // This method traverses the first level first, before diving into child directories.
    // FileVisitResult returned from visitor may be ignored and always considered CONTINUE.
    @Throws(IOException::class)
    private fun walkFileTreeForSearch(start: Path, visitor: FileVisitor<in Path>): Path {
        fun readAttributes(path: Path): BasicFileAttributes? {
            return try {
                path.readAttributes(BasicFileAttributes::class.java)
            } catch (ignored: IOException) {
                try {
                    path.readAttributes(BasicFileAttributes::class.java, LinkOption.NOFOLLOW_LINKS)
                } catch (e: IOException) {
                    visitor.visitFileFailed(path, e)
                    null
                }
            }
        }

        val attributes = readAttributes(start) ?: return start
        if (!attributes.isDirectory) {
            visitor.visitFile(start, attributes)
            return start
        }
        val directoryStream = try {
            start.newDirectoryStream()
        } catch (e: IOException) {
            visitor.visitFileFailed(start, e)
            return start
        }
        val directories = mutableListOf<Path>()
        directoryStream.use {
            visitor.preVisitDirectory(start, attributes)
            try {
                for (path in directoryStream) {
                    val childAttributes = readAttributes(path) ?: continue
                    visitor.visitFile(path, childAttributes)
                    if (childAttributes.isDirectory) {
                        directories.add(path)
                    }
                }
            } catch (e: DirectoryIteratorException) {
                visitor.postVisitDirectory(start, e.cause)
                return start
            }
        }
        for (path in directories) {
            Files.walkFileTree(
                path, setOf(FileVisitOption.FOLLOW_LINKS), Int.MAX_VALUE,
                object : FileVisitor<Path> {
                    @Throws(InterruptedIOException::class)
                    override fun preVisitDirectory(
                        directory: Path,
                        attributes: BasicFileAttributes
                    ): FileVisitResult {
                        if (directory == path) {
                            return FileVisitResult.CONTINUE
                        }
                        return visitor.preVisitDirectory(directory, attributes)
                    }

                    @Throws(InterruptedIOException::class)
                    override fun visitFile(
                        file: Path,
                        attributes: BasicFileAttributes
                    ): FileVisitResult {
                        if (file == path) {
                            return FileVisitResult.CONTINUE
                        }
                        return visitor.visitFile(file, attributes)
                    }

                    @Throws(InterruptedIOException::class)
                    override fun visitFileFailed(
                        file: Path,
                        exception: IOException
                    ): FileVisitResult {
                        if (file == path) {
                            // We are searching and ignoring errors, so just log it.
                            Log.e(TAG, "visitFileFailed for $file", exception)
                            return FileVisitResult.CONTINUE
                        }
                        return visitor.visitFileFailed(file, exception)
                    }

                    @Throws(InterruptedIOException::class)
                    override fun postVisitDirectory(
                        directory: Path,
                        exception: IOException?
                    ): FileVisitResult {
                        if (directory == path) {
                            // We are searching and ignoring errors, so just log it.
                            if (exception != null) {
                                Log.e(TAG, "postVisitDirectory failed for $directory", exception)
                            }
                            return FileVisitResult.CONTINUE
                        }
                        return visitor.postVisitDirectory(directory, exception)
                    }
                }
            )
        }
        visitor.postVisitDirectory(start, null)
        return start
    }

    @Throws(InterruptedIOException::class)
    private fun throwIfInterrupted() {
        if (Thread.interrupted()) {
            throw InterruptedIOException()
        }
    }
}
