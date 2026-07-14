/*
 * Copyright (c) 2019 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * Copyright (c) 2026 eetufin92 <eetufin92@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.filelist

import android.os.AsyncTask
import java8.nio.file.Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.zhanghai.android.files.R
import me.zhanghai.android.files.app.AppDatabase
import me.zhanghai.android.files.file.FileItem
import me.zhanghai.android.files.file.cache.getServerId
import me.zhanghai.android.files.file.cache.toEntity
import me.zhanghai.android.files.file.cache.toFileItem
import me.zhanghai.android.files.file.loadFileItem
import me.zhanghai.android.files.provider.common.search
import me.zhanghai.android.files.provider.common.search.SearchFilter
import me.zhanghai.android.files.settings.Settings
import me.zhanghai.android.files.util.CloseableLiveData
import me.zhanghai.android.files.util.Failure
import me.zhanghai.android.files.util.Loading
import me.zhanghai.android.files.util.SearchLoading
import me.zhanghai.android.files.util.Stateful
import me.zhanghai.android.files.util.Success
import me.zhanghai.android.files.util.valueCompat
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future

class SearchFileListLiveData(
    private val path: Path,
    private val query: String
) : CloseableLiveData<Stateful<List<FileItem>>>() {
    private var future: Future<Unit>? = null

    init {
        loadValue()
    }

    fun loadValue() {
        future?.cancel(true)
        value = Loading(emptyList())
        val serverId = path.getServerId()
        val isRemoteIndexingEnabled = path.isRemotePath && Settings.REMOTE_FILE_INDEXING.valueCompat
            && serverId != null

        @Suppress("OPT_IN_USAGE")
        GlobalScope.launch(Dispatchers.Main) {
            val fileList = mutableListOf<FileItem>()
            val seenPaths = mutableSetOf<String>()

            if (isRemoteIndexingEnabled) {
                postValue(SearchLoading(emptyList(), R.string.search_state_searching_local_cache))
                val filters = SearchFilter.parseQuery(query)
                val nameQuery = filters.filterIsInstance<SearchFilter.Name>()
                    .firstOrNull { it.positive }?.text ?: ""
                val cachedEntities = withContext(Dispatchers.IO) {
                    AppDatabase.getInstance().fileCacheDao().searchByName(
                        serverId!!, if (nameQuery.isNotEmpty()) "$nameQuery*" else "*", path.toString()
                    )
                }
                for (entity in cachedEntities) {
                    if (filters.all { it.accept(entity.name, entity.size, entity.lastModifiedTime) }) {
                        val item = entity.toFileItem(path)
                        fileList.add(item)
                        seenPaths.add(item.path.toString())
                    }
                }
                if (fileList.isNotEmpty()) {
                    postValue(SearchLoading(fileList.toList(), R.string.search_state_searching_remote_server))
                }
            } else {
                postValue(SearchLoading(emptyList(), R.string.search_state_searching_remote_server))
            }

            future = (AsyncTask.THREAD_POOL_EXECUTOR as ExecutorService).submit<Unit> {
                try {
                    path.search(query, INTERVAL_MILLIS) { paths: List<Path> ->
                        val batchEntities = mutableListOf<me.zhanghai.android.files.file.cache.FileCacheEntity>()
                        for (path in paths) {
                            val fileItem = try {
                                path.loadFileItem()
                            } catch (e: IOException) {
                                e.printStackTrace()
                                continue
                            }
                            if (seenPaths.add(fileItem.path.toString())) {
                                fileList.add(fileItem)
                            } else {
                                // Update unverified item with verified one
                                val index = fileList.indexOfFirst { it.path.toString() == fileItem.path.toString() }
                                if (index != -1) {
                                    fileList[index] = fileItem
                                }
                            }
                            if (isRemoteIndexingEnabled) {
                                batchEntities.add(fileItem.toEntity(serverId!!))
                            }
                        }
                        if (batchEntities.isNotEmpty()) {
                            @Suppress("OPT_IN_USAGE")
                            GlobalScope.launch(Dispatchers.IO) {
                                AppDatabase.getInstance().fileCacheDao().insertAll(batchEntities)
                            }
                        }
                        postValue(SearchLoading(fileList.toList(), R.string.search_state_searching_remote_server))
                    }
                    postValue(Success(fileList))
                } catch (e: Exception) {
                    postValue(Failure(valueCompat.value, e))
                }
            }
        }
    }

    override fun close() {
        future?.cancel(true)
    }

    companion object {
        private const val INTERVAL_MILLIS = 500L
    }
}
