package com.microsoft.notes.sync

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.microsoft.notes.utils.logging.EventMarkers
import com.microsoft.notes.utils.logging.NotesLogger
import com.microsoft.notes.utils.logging.NotesSDKTelemetryKeys.SyncProperty.EXCEPTION_TYPE

interface IPersist<T> {
    fun load(): T?
    fun persist(objectToPersist: T)
}

class PersistentPriorityQueue(
    private val persistenceFileName: String = "queue.list",
    val createBackupFile: (String) -> IPersist<String>,
    val notesLogger: NotesLogger? = null
) : BackupQueue {

    private val backUpFile = createBackupFile(persistenceFileName)

    private val jsonParser: Gson by lazy { Gson() }

    override fun load(): List<ApiRequestOperation> = loadQueueFromDisk()

    override fun persist(queue: List<ApiRequestOperation>) {
        val map = mapOf("SCHEMA_VERSION" to ApiRequestOperation.SCHEMA_VERSION, "queue" to queue)
        backUpFile.persist(jsonParser.toJson(map))
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadQueueFromDisk(): List<ApiRequestOperation> {
        val jsonMap: String = backUpFile.load() ?: return emptyList()
        val savedMap: Map<String, Any>
        try {
            savedMap = jsonParser.fromJson<Map<String, Any>>(
                jsonMap,
                object : TypeToken<Map<String, Any>>() {}.type
            )
        } catch (e: JsonSyntaxException) {
            notesLogger?.d(message = "OutboundQueue json syntax exception ${e.message}")
            notesLogger?.recordTelemetry(
                EventMarkers.SyncCorruptedOutboundQueueBackup,
                Pair(EXCEPTION_TYPE, e.javaClass.canonicalName ?: "JsonSyntaxException")
            )
            return emptyList()
        }
        val savedSchemaVersion = (savedMap.get("SCHEMA_VERSION") as? Double)?.toInt() ?: return emptyList()
        val savedQueueJson: List<Map<String, Any>> = savedMap.get("queue") as? List<Map<String, Any>> ?: emptyList()
        val migratedQueueJson = migrateQueueJsonIfRequired(savedQueueJson, savedSchemaVersion)
        val queue: List<ApiRequestOperation> = migratedQueueJson.mapNotNull { operation: Map<String, Any> ->
            ApiRequestOperation.fromMap(operation)
        }
        return queue
    }

    @Suppress("UNCHECKED_CAST")
    private fun migrateQueueJsonIfRequired(
        savedQueue: List<Map<String, Any>>,
        savedSchemaVersion: Int
    ): List<Map<String, Any>> {
        if (savedSchemaVersion < ApiRequestOperation.SCHEMA_VERSION) {
            notesLogger?.recordTelemetry(eventMarker = EventMarkers.OutboundQueueMigrationTriggered)
            val migratedQueue = savedQueue.mapNotNull { operation: Map<String, Any> ->
                ApiRequestOperation.migrate(
                    operation, old = savedSchemaVersion,
                    new = ApiRequestOperation.SCHEMA_VERSION
                ) as? Map<String, Any>
            }
            return migratedQueue
        } else {
            return savedQueue
        }
    }
}
