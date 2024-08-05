package com.microsoft.notes.platform.files

import com.microsoft.notes.sync.IPersist
import com.microsoft.notes.utils.logging.EventMarkers
import com.microsoft.notes.utils.logging.NotesLogger
import com.microsoft.notes.utils.logging.NotesSDKTelemetryKeys
import java.io.Closeable
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InvalidClassException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.OptionalDataException
import java.io.StreamCorruptedException

class AtomicFileBackup<T>(
    private val filesDir: File?,
    private val fileName: String,
    private val notesLogger: NotesLogger? = null
) : IPersist<T> {

    private fun closeSilently(stream: Closeable?) {
        try {
            stream?.close()
        } catch (closeException: IOException) {
            // Do nothing
        }
    }

    override fun load(): T? {
        var fileInputStream: AtomicInputStream? = null
        var ois: ObjectInputStream? = null
        return try {
            fileInputStream = AtomicInputStream(File(filesDir, fileName))
            ois = ObjectInputStream(fileInputStream)
            @Suppress("UNCHECKED_CAST", "UnsafeCast")
            val objectFromDisk: T = ois.readObject() as T
            notesLogger?.i(message = "File loaded successfully")
            objectFromDisk
        } catch (e: Exception) {
            catchReadObjectException(e)
        } finally {
            closeSilently(ois)
            closeSilently(fileInputStream)
        }
    }

    private fun catchReadObjectException(e: Exception): T? {
        notesLogger?.e(message = "File load failed ${e.javaClass.name}")
        return when (e) {
            is FileNotFoundException -> {
                /*
                * On first boot this file is not yet created and load occurs. Hence not recording the event
                */
                null
            }
            is ClassNotFoundException, is InvalidClassException, is StreamCorruptedException, is OptionalDataException, is IOException -> {
                recordSyncCorruptedOutboundQueueBackupTelemetry(e)
                null
            }
            else -> {
                throw e
            }
        }
    }

    private fun recordSyncCorruptedOutboundQueueBackupTelemetry(e: java.lang.Exception) {
        notesLogger?.recordTelemetry(
            EventMarkers.SyncCorruptedOutboundQueueBackup,
            Pair(NotesSDKTelemetryKeys.SyncProperty.EXCEPTION_TYPE, e.javaClass.simpleName)
        )
    }

    override fun persist(objectToPersist: T) {
        var fileOutputStream: AtomicFileOutStream? = null
        var oos: ObjectOutputStream? = null
        try {
            fileOutputStream = AtomicFileOutStream(File(filesDir, fileName))
            oos = ObjectOutputStream(fileOutputStream)
            oos.writeObject(objectToPersist)
            oos.flush()
            fileOutputStream.writeSuccessfullyCompleted = true
            notesLogger?.i(message = "Persist file successful")
        } catch (e: IOException) {
            notesLogger?.e(message = "Persist file failed ${e.javaClass.name}")
        } finally {
            closeSilently(oos)
            // No-op if already closed
            closeSilently(fileOutputStream)
        }
    }
}
