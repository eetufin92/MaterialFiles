/*
 * Copyright (c) 2019 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * Copyright (c) 2026 eetufin92 <eetufin92@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.provider.common

import android.util.Log
import java.io.IOException
import java.io.InterruptedIOException
import java.time.ZoneId
import java8.nio.file.DirectoryIteratorException
import java8.nio.file.FileVisitOption
import java8.nio.file.FileVisitResult
import java8.nio.file.FileVisitor
import java8.nio.file.Files
import java8.nio.file.LinkOption
import java8.nio.file.Path
import java8.nio.file.attribute.BasicFileAttributes
import me.zhanghai.android.files.provider.common.search.SearchFilter

object WalkFileTreeSearchable {
    private val TAG = WalkFileTreeSearchable::class.java.simpleName

    @Throws(IOException::class)
    fun search(
        directory: Path,
        query: String,
        intervalMillis: Long,
        listener: (List<Path>) -> Unit
    ) {
        val filters = SearchFilter.parseQuery(query)
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
                val name = path.fileName?.toString() ?: ""
                val size = attributes?.size() ?: 0L
                val lastModifiedTime = attributes?.lastModifiedTime()?.toMillis() ?: 0L
                if (filters.all { it.accept(name, size, lastModifiedTime) }) {
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
