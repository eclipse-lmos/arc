// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.scripting

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.eclipse.lmos.arc.core.closeWith
import org.eclipse.lmos.arc.core.onFailure
import org.eclipse.lmos.arc.core.result
import org.eclipse.lmos.arc.scripting.agents.ScriptingAgentLoader
import org.eclipse.lmos.arc.scripting.functions.ScriptingLLMFunctionLoader
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.nio.file.FileSystems
import java.nio.file.StandardWatchEventKinds.*
import java.nio.file.WatchService
import java.util.concurrent.Executors.newCachedThreadPool
import java.util.concurrent.Executors.newFixedThreadPool
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration

/**
 * Watches a directory for file changes and reloads scripts when changes are detected.
 */
class ScriptHotReload(
    private val scriptAgentLoader: ScriptingAgentLoader,
    private val scriptFunctionLoader: ScriptingLLMFunctionLoader,
    private val fallbackInterval: Duration,
) : Closeable {
    private val log = LoggerFactory.getLogger(this.javaClass)

    private val fileWatcher = lazy {
        try {
            val watchService = FileSystems.getDefault().newWatchService()
            WatchServiceFileWatcher(watchService)
        } catch (e: UnsupportedOperationException) {
            log.warn("Falling back to PollingFileWatcher...")
            PollingFileWatcher(fallbackInterval)
        }
    }

    fun start(directory: File) {
        log.debug("Starting hot-reload of agents from ${directory.absoluteFile}(${directory.listFiles()?.size})")
        fileWatcher.value.watch(directory) { event ->
            result<Unit, Exception> {
                when (event) {
                    is FileEvent.Created -> {
                        if (event.file.isDirectory) return@watch
                        scriptAgentLoader.loadAgents(event.file)
                        scriptFunctionLoader.loadFunctions(event.file)
                    }

                    is FileEvent.Modified -> {
                        if (event.file.isDirectory) return@watch
                        scriptAgentLoader.loadAgents(event.file)
                        scriptFunctionLoader.loadFunctions(event.file)
                    }

                    is FileEvent.Deleted -> {
                        // TODO
                    }
                }
            }.onFailure { logError(it) }
        }
    }

    private fun logError(ex: Exception) {
        log.error("Unexpected Error while hot-loading Agent scripts!", ex)
    }

    override fun close() {
        fileWatcher.value.close()
    }
}

internal sealed class FileEvent {
    data class Created(val file: File) : FileEvent()
    data class Modified(val file: File) : FileEvent()
    data class Deleted(val file: File) : FileEvent()
}

private interface FileWatcher {
    fun watch(directory: File, onEvent: (FileEvent) -> Unit)
    fun close()
}

private class WatchServiceFileWatcher(private val watchService: WatchService) : FileWatcher {

    private val scope = CoroutineScope(SupervisorJob() + newCachedThreadPool().asCoroutineDispatcher())

    override fun watch(directory: File, onEvent: (FileEvent) -> Unit) {
        scope.launch {
            result<Unit, Exception> {
                directory.toPath()
                    .register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE) closeWith { it.cancel() }
                while (true) {
                    val watchKey = watchService.take()
                    watchKey.pollEvents().forEach { event ->
                        val kind = event.kind()
                        val file = File(directory, event.context().toString())
                        when (kind) {
                            ENTRY_CREATE -> onEvent(FileEvent.Created(file))
                            ENTRY_MODIFY -> onEvent(FileEvent.Modified(file))
                            ENTRY_DELETE -> onEvent(FileEvent.Deleted(file))
                        }
                    }
                    if (!watchKey.reset()) {
                        watchKey.cancel()
                        break
                    }
                }
            }
        }
    }

    override fun close() {
        result<Unit, IOException> { watchService.close() }
    }
}

private class PollingFileWatcher(private val interval: Duration) : FileWatcher {

    private val running = AtomicBoolean(false)
    private val scope = CoroutineScope(SupervisorJob() + newFixedThreadPool(1).asCoroutineDispatcher())

    override fun watch(directory: File, onEvent: (FileEvent) -> Unit) {
        scope.launch {
            result<Unit, Exception> {
                running.set(true)
                var lastFiles = directory.walk().filter { it.isFile }.toList()
                var lastModified = lastFiles.associate { it.name to it.lastModified() }

                while (running.get()) {
                    val currentFiles = (directory.listFiles()?.toList() ?: emptyList())
                    val created = currentFiles.filter { !lastFiles.contains(it) }
                    val deleted = lastFiles.filter { !currentFiles.contains(it) }
                    val modified = currentFiles.filter {
                        it.lastModified() > (lastModified[it.name] ?: it.lastModified())
                    }
                    created.forEach { onEvent(FileEvent.Created(it)) }
                    deleted.forEach { onEvent(FileEvent.Deleted(it)) }
                    modified.forEach { onEvent(FileEvent.Modified(it)) }
                    lastFiles = currentFiles
                    lastModified = lastFiles.associate { it.name to it.lastModified() }
                    delay(interval)
                }
            }
        }
    }

    override fun close() {
        running.set(false)
    }
}
