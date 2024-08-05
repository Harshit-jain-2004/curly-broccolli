package com.microsoft.notes.sideeffect.persistence.handler

import android.database.sqlite.SQLiteBlobTooBigException
import com.microsoft.notes.sideeffect.persistence.Note
import com.microsoft.notes.sideeffect.persistence.NotesDatabase
import com.microsoft.notes.sideeffect.persistence.PreferenceKeys
import com.microsoft.notes.sideeffect.persistence.extensions.getNoteTelemetryAttributes
import com.microsoft.notes.sideeffect.persistence.mapper.toStoreMeetingNoteList
import com.microsoft.notes.sideeffect.persistence.mapper.toStoreNoteList
import com.microsoft.notes.sideeffect.persistence.mapper.toStoreNoteReferenceList
import com.microsoft.notes.store.action.Action
import com.microsoft.notes.store.action.ReadAction
import com.microsoft.notes.store.action.ReadAction.FetchAllNotesAction
import com.microsoft.notes.store.action.ReadAction.NotesLoadedAction
import com.microsoft.notes.store.action.ReadAction.RetrieveDeltaTokensForAllNoteTypes
import com.microsoft.notes.ui.extensions.isSamsungNote
import com.microsoft.notes.utils.logging.EventMarkers
import com.microsoft.notes.utils.logging.NoteColor
import com.microsoft.notes.utils.logging.NoteType
import com.microsoft.notes.utils.logging.NotesLogger
import com.microsoft.notes.utils.logging.NotesSDKTelemetryKeys.NoteProperty
import com.microsoft.notes.utils.logging.NotesSDKTelemetryKeys.SyncProperty
import com.microsoft.notes.utils.logging.Percentile
import com.microsoft.notes.utils.logging.SNMarker
import com.microsoft.notes.utils.logging.SNMarkerConstants
import com.microsoft.notes.utils.utils.UserInfo
import com.microsoft.notes.models.MeetingNote as StoreMeetingNote
import com.microsoft.notes.models.Note as StoreNote
import com.microsoft.notes.models.NoteReference as StoreNoteReference

object ReadHandler : ActionHandler<ReadAction> {
    private const val FIRST_BATCH_MAX = 10
    internal var notesDBPageSize = 50

    override fun handle(
        action: ReadAction,
        notesDB: NotesDatabase,
        notesLogger: NotesLogger?,
        findNote: (String) -> StoreNote?,
        actionDispatcher: (Action) -> Unit
    ) =
        when (action) {
            is FetchAllNotesAction -> {
                SNMarker.logMarker(SNMarkerConstants.NotesFetchUIStart)
                fetchAllNotes(notesDB, actionDispatcher, notesLogger, action.userID, action.isSamsungNotesSyncEnabled)
            }
            is RetrieveDeltaTokensForAllNoteTypes -> fetchDeltaTokensForAllNoteTypes(notesDB, actionDispatcher, notesLogger, action.userInfo)
            else -> {
            }
        }

    private fun fetchAllNotes(
        notesDB: NotesDatabase,
        actionDispatcher: (Action) -> Unit,
        notesLogger: NotesLogger?,
        userID: String,
        isSamsungNotesSyncEnabled: Boolean
    ) {
        fetchAllNotesFromDBIn2Steps(notesDB, notesLogger) { allNotesLoaded, notesCollection,
            noteReferencesCollection, meetingNotesCollection ->
            val (samsungNotes, stickyNotes) = if (isSamsungNotesSyncEnabled) {
                notesCollection.partition { it.isSamsungNote() }
            } else {
                emptyList<StoreNote>() to notesCollection
            }
            actionDispatcher.invoke(
                NotesLoadedAction(
                    allNotesLoaded, stickyNotes, samsungNotes,
                    noteReferencesCollection, meetingNotesCollection, userID
                )
            )
        }
    }

    private fun fetchAllNotesFromDBAsPages(
        notesDB: NotesDatabase
    ): List<Note> {
        val dao = notesDB.noteDao()
        var notes = dao.getFirstOrderByDocumentModifiedAt(notesDBPageSize)
        var currentSize = notes.size
        var lastNotes = notes
        // (documentModifiedAt, exclusionIds)
        var lastMeta: Pair<Long, List<String>>? = null

        while (currentSize == notesDBPageSize) {
            // last() will never be null given the above page size check
            notes.last().let { lastNote ->
                val threshold = lastNote.documentModifiedAt
                val lastIds = lastNotes.map { it.id }
                // only track ids for the current threshold rather than all known note ids
                // sql lite does have a SQLITE_MAX_SQL_LENGTH for a query, so be responsible about it
                val currentMeta = lastMeta?.let {
                    val (lastMetaThreshold, lastMetaIds) = it
                    if (lastMetaThreshold == threshold) {
                        Pair(threshold, (lastMetaIds + lastIds).distinct())
                    } else {
                        Pair(threshold, lastIds)
                    }
                } ?: Pair(threshold, lastIds)

                val (lastModified, exclusionIds) = currentMeta
                val nextNotes = dao.getNextOrderByDocumentModifiedAt(notesDBPageSize, lastModified, exclusionIds)

                lastMeta = currentMeta
                lastNotes = nextNotes
                notes = notes + nextNotes
                currentSize = nextNotes.size
            }
        }

        return notes
    }

    // Recovers the user from having too-large note entries in the DB. Expected to run once only.
    @Suppress("LoopWithTooManyJumpStatements")
    private fun fetchAllNotesIndividuallyAndDeleteTooLargeNotes(
        notesDB: NotesDatabase
    ): List<Note> {
        val dao = notesDB.noteDao()
        var offset = 0
        val notes = mutableListOf<Note>()
        var noMoreNotesFound = false
        do {
            try {
                val newNotes = dao.getNotes(1, offset)
                noMoreNotesFound = newNotes.isEmpty()
                notes.addAll(newNotes)
                offset += 1
            } catch (e: IllegalStateException) {
                // The note is unable to be read because it was too large
                dao.deleteNotes(1, offset)
                continue
            } catch (e: SQLiteBlobTooBigException) {
                // The note is unable to be read because it was too large
                dao.deleteNotes(1, offset)
                continue
            } catch (e: OutOfMemoryError) {
                // The note is unable to be read because it was too large
                dao.deleteNotes(1, offset)
                continue
            }
        } while (!noMoreNotesFound)

        notes.sortByDescending { it.documentModifiedAt }
        return notes
    }

    internal fun fetchAllNotesFromDBIn2Steps(
        notesDB: NotesDatabase,
        notesLogger: NotesLogger? = null,
        batchProcessor: (
            allNotesLoaded: Boolean,
            notesCollection: List<StoreNote>,
            noteReferencesCollection: List<StoreNoteReference>,
            meetingNotesCollection: List<StoreMeetingNote>
        ) -> Unit
    ) {
        SNMarker.logMarker(SNMarkerConstants.NotesFetchDBStart)

        var largeNoteDeletionDone = false
        val persistenceNotesCollection = try {
            fetchAllNotesFromDBAsPages(notesDB)
        } catch (ex: Exception) {
            when (ex) {
                is IllegalStateException, is SQLiteBlobTooBigException, is OutOfMemoryError -> {
                    largeNoteDeletionDone = true
                    fetchAllNotesIndividuallyAndDeleteTooLargeNotes(notesDB)
                }
                is IllegalArgumentException -> {
                    notesLogger?.recordTelemetry(
                        EventMarkers.PersistenceNotesFetchException,
                        Pair(SyncProperty.EXCEPTION_TYPE, ex.javaClass.simpleName)
                    )
                    fetchAllNotesIndividuallyAndDeleteTooLargeNotes(notesDB)
                }
                else -> throw ex
            }
        }

        notesLogger?.recordTelemetry(
            EventMarkers.PersistedNotesOnBoot,
            Pair(NoteProperty.NOTES_COUNT, persistenceNotesCollection.size.toString()),
            Pair(NoteProperty.LARGE_NOTE_DELETION_DONE, largeNoteDeletionDone.toString())
        )

        val noteReferencesCollection = notesDB.noteReferenceDao().getAll().toStoreNoteReferenceList()
        val meetingNotesCollection = notesDB.meetingNoteDao().getAll().toStoreMeetingNoteList()
        SNMarker.logMarker(SNMarkerConstants.NotesFetchDBEnd)

        val batchSize = if (persistenceNotesCollection.size > FIRST_BATCH_MAX) FIRST_BATCH_MAX
        else persistenceNotesCollection.size

        val isOnlyOneBatch = batchSize >= persistenceNotesCollection.size
        val firstBatch = getFirstBatchOfNotes(persistenceNotesCollection, batchSize, notesLogger)
        batchProcessor.invoke(isOnlyOneBatch, firstBatch, noteReferencesCollection, meetingNotesCollection)

        val allStoreNotes = if (persistenceNotesCollection.size > FIRST_BATCH_MAX) {
            val secondBatch = getRemainingBatchOfNotes(persistenceNotesCollection, batchSize, notesLogger)
            batchProcessor.invoke(true, secondBatch, emptyList(), emptyList())
            firstBatch + secondBatch
        } else {
            firstBatch
        }

        logStoredNotesOnBoot(allStoreNotes, notesLogger, noteReferencesCollection.size)
    }

    private fun fetchDeltaTokensForAllNoteTypes(
        notesDB: NotesDatabase,
        actionDispatcher: (Action) -> Unit,
        notesLogger: NotesLogger?,
        userInfo: UserInfo
    ) {
        val deltaToken = notesDB.preferencesDao().get(PreferenceKeys.deltaToken)
        val samsungDeltaToken = notesDB.preferencesDao().get(PreferenceKeys.samsungNotesDeltaToken)
        val noteReferencesDeltaToken = notesDB.preferencesDao().get(PreferenceKeys.noteReferencesDeltaToken)

        notesLogger?.recordTelemetry(EventMarkers.PersistedNoteDeltaTokensRetrieved)

        actionDispatcher.invoke(ReadAction.DeltaTokenLoadedAction(deltaToken, samsungDeltaToken, noteReferencesDeltaToken, userInfo))
    }

    private fun getFirstBatchOfNotes(notesListToBatch: List<Note>, batchSize: Int, notesLogger: NotesLogger?): List<StoreNote> =
        notesListToBatch.subList(0, batchSize).toStoreNoteList(notesLogger)

    private fun getRemainingBatchOfNotes(notesListToBatch: List<Note>, batchSize: Int, notesLogger: NotesLogger?): List<StoreNote> =
        if (notesListToBatch.size > batchSize) notesListToBatch.subList(
            batchSize,
            notesListToBatch.size
        ).toStoreNoteList(notesLogger) else arrayListOf()

    private fun initializeColorMap(): MutableMap<String, Int> {
        val colorMap = hashMapOf<String, Int>().toMutableMap()
        NoteColor.values().forEach {
            colorMap[it.name] = 0
        }

        return colorMap
    }

    private fun initializeNoteTypeMap(): MutableMap<String, Int> {
        val noteTypeMap = hashMapOf<String, Int>().toMutableMap()
        NoteType.values().forEach {
            noteTypeMap[it.name] = 0
        }

        return noteTypeMap
    }

    private fun initializePercentileMap(): MutableMap<Int, Int> {
        val percentileMap = hashMapOf<Int, Int>().toMutableMap()
        Percentile.values().forEach {
            percentileMap[it.value] = 0
        }

        return percentileMap
    }

    private fun logStoredNotesOnBoot(
        notesCollection: List<StoreNote>,
        notesLogger: NotesLogger?,
        noteReferencesCollectionSize: Int
    ) {
        // For safety any exception will be ignored to avoid crash on app boot
        try {
            calculateTelemetryAttributes(
                notesCollection
            ) { colorMap, noteTypeMap, paragraphPercentileMap, imagePercentileMap ->
                notesLogger?.recordTelemetry(
                    EventMarkers.StoredNotesOnBoot,
                    Pair(NoteProperty.NOTES_COUNT, notesCollection.size.toString()),
                    Pair(NoteProperty.NOTES_BY_TYPE, noteTypeMap.toTelemetrySchema()),
                    Pair(NoteProperty.NOTES_BY_COLOR, colorMap.toTelemetrySchema()),
                    Pair(NoteProperty.PARAGRAPH_LENGTH_PERCENTILES, paragraphPercentileMap.toTelemetrySchema()),
                    Pair(NoteProperty.IMAGE_COUNT_PERCENTILES, imagePercentileMap.toTelemetrySchema()),
                    Pair(NoteProperty.NOTE_REFERENCE_COUNT, noteReferencesCollectionSize.toString())
                )
            }
        } catch (e: Exception) {
            notesLogger?.recordTelemetry(
                EventMarkers.StoredNotesOnBootException,
                Pair(SyncProperty.EXCEPTION_TYPE, e.javaClass.simpleName)
            )
        }
    }

    internal fun calculateTelemetryAttributes(
        notesCollection: List<StoreNote>,
        action: (
            colorMap: Map<String, Int>,
            noteTypeMap: Map<String, Int>,
            paragraphMap: Map<Int, Int>,
            imageMap: Map<Int, Int>
        ) -> Unit
    ) {
        val colorMap = initializeColorMap()
        val noteTypeMap = initializeNoteTypeMap()
        val paragraphCount = IntArray(notesCollection.size)
        val imageCount = mutableListOf<Int>()

        notesCollection.forEachIndexed { index, note ->
            val telemetryAttributes = note.getNoteTelemetryAttributes()
            colorMap.increment(telemetryAttributes.noteColor.name)
            noteTypeMap.increment(telemetryAttributes.noteType.name)
            paragraphCount[index] = telemetryAttributes.paragraphCount
            if (telemetryAttributes.imageCount > 0) {
                imageCount.add(telemetryAttributes.imageCount)
            }
        }

        val paragraphPercentileMap = calculatePercentile(paragraphCount)
        val imagePercentileMap = calculatePercentile(imageCount.toIntArray())

        action(colorMap, noteTypeMap, paragraphPercentileMap, imagePercentileMap)
    }

    internal fun calculatePercentile(values: IntArray): Map<Int, Int> {
        val stats = initializePercentileMap()
        if (values.isNotEmpty()) {
            val ordered = values.sortedArray()
            val divider = 100.0
            val length = (ordered.size - 1).toDouble()
            Percentile.values().forEach {
                val percentile = it.value
                val index = Math.floor(length * (percentile.toDouble() / divider))
                val value = ordered[index.toInt()]
                stats[percentile] = value
            }
        }

        return stats
    }

    // to be used for testing
    @Suppress("unused")
    internal fun setDBPageSize(pageSize: Int) {
        notesDBPageSize = pageSize
    }

    private fun MutableMap<String, Int>.increment(key: String) {
        this[key]?.let {
            this[key] = it + 1
        }
    }

    private fun <K, V> Map<K, V>.toTelemetrySchema(): String = this.toString().replace("=", ":")
}
