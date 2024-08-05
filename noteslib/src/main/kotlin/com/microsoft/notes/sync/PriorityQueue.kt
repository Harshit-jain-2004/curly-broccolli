package com.microsoft.notes.sync

import com.microsoft.notes.utils.logging.NotesLogger

interface BackupQueue {
    fun load(): List<ApiRequestOperation>
    fun persist(queue: List<ApiRequestOperation>)
}

class PriorityQueue(
    private var queue: List<ApiRequestOperation>,
    private var persistenceOnDiskEnabled: Boolean = true,
    private val createBackupFile: (String) -> IPersist<String>,
    private val notesLogger: NotesLogger? = null
) {

    private var currentQueueInDisk: List<ApiRequestOperation> = queue
    private val backupQueue: BackupQueue = PersistentPriorityQueue(
        createBackupFile = createBackupFile,
        notesLogger = notesLogger
    )
    private val queueLock = Any()

    init {
        Thread {
            write {
                if (persistenceOnDiskEnabled) {
                    it.plus(loadQueue())
                } else {
                    it
                }
            }
        }.start()
    }

    fun clear() {
        write {
            emptyList()
        }
    }

    fun push(item: ApiRequestOperation) {
        write {
            val list = it.toMutableList()
            list.add(item)
            list.toList()
        }
    }

    fun peek(): ApiRequestOperation? = read().firstOrNull()

    val count get() = read().size

    fun toList(): List<ApiRequestOperation> = queue

    fun remove(item: ApiRequestOperation) {
        removeIf { i -> i.uniqueId == item.uniqueId }
    }

    fun removeIf(predicate: (ApiRequestOperation) -> Boolean) {
        write { it.filter { i -> !predicate(i) } }
    }

    fun replace(item: ApiRequestOperation) {
        map { if (it.uniqueId == item.uniqueId) item else it }
    }

    fun map(transform: (ApiRequestOperation) -> ApiRequestOperation) {
        write { it.map(transform) }
    }

    fun forEach(func: (ApiRequestOperation) -> Unit) {
        val queue = read()
        queue.forEach { func(it) }
    }

    fun synchronized(block: () -> Unit) {
        synchronized(lock = queueLock) {
            block()
        }
    }

    private fun write(transform: (List<ApiRequestOperation>) -> List<ApiRequestOperation>) {
        synchronized(
            lock = queueLock,
            block = {
                queue = transform(queue).sortedWith(
                    compareBy {
                        ApiRequestOperation.priority(it)
                    }
                )
                if (persistenceOnDiskEnabled) {
                    currentQueueInDisk = persistQueue(queue, currentQueueInDisk)
                }
            }
        )
    }

    private fun read(): List<ApiRequestOperation> = queue

    private fun persistQueue(
        queue: List<ApiRequestOperation>,
        currentQueueInDisk: List<ApiRequestOperation>
    ): List<ApiRequestOperation> {
        if (queue != currentQueueInDisk) {
            backupQueue.persist(queue)
        }
        return queue
    }

    private fun loadQueue(): List<ApiRequestOperation> = backupQueue.load()
}
