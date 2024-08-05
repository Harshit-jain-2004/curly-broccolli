package com.microsoft.notes.sampleapp

import android.os.Environment
import android.util.Log
import com.microsoft.notes.utils.logging.SNMarkerConstants
import com.microsoft.notes.utils.logging.SNMarkerListener
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.concurrent.Executors

class SampleAppSNMarkerListener : SNMarkerListener {

    val LOG_TAG: String = "SAMarkerListener"
    private var uiFetchNotesStart =  DEFAULT_VALUE
    private var fetchNotesStart = DEFAULT_VALUE
    private val executorService = Executors.newSingleThreadExecutor()
    private var isUIFetchMarkerLogged = false
    private var isFetchNotesMarkerLogged = false
    companion object {
        private const val PERF_DATA_FILE_NAME = "PerfData.csv"
        private const val FETCH_NOTES_UI_EVENT_NAME = "FetchNotesUI"
        private const val FETCH_NOTES_DB_EVENT_NAME = "FetchNotesDB"
        private const val DEFAULT_VALUE = -1L
    }
    override fun handleEvent(markerConstant: SNMarkerConstants) {
        Log.d(LOG_TAG, "EventMarker:"+ markerConstant.value)
        val time = System.currentTimeMillis()
        val runnable = Runnable {
            when (markerConstant) {
                SNMarkerConstants.NotesFetchUIStart ->
                    if (uiFetchNotesStart == DEFAULT_VALUE)
                        uiFetchNotesStart = time
                SNMarkerConstants.NotesFetchUIEnd ->
                    if (!isUIFetchMarkerLogged) {
                        logPerfData(time-uiFetchNotesStart, FETCH_NOTES_UI_EVENT_NAME)
                        isUIFetchMarkerLogged = true
                    }
                SNMarkerConstants.NotesFetchDBStart ->
                    if (fetchNotesStart == DEFAULT_VALUE)
                        fetchNotesStart = time
                SNMarkerConstants.NotesFetchDBEnd ->
                    if (!isFetchNotesMarkerLogged) {
                        logPerfData(time-fetchNotesStart, FETCH_NOTES_DB_EVENT_NAME)
                        isFetchNotesMarkerLogged = true
                    }
            }
        }
        executorService.execute(runnable)
    }

    private fun logPerfData(timeTaken: Long, eventName: String) {
        val directory = Environment.getExternalStorageDirectory()

        var writer: BufferedWriter? = null
        try {
            val file = getFile(directory, PERF_DATA_FILE_NAME)
            val fileWriter = FileWriter(file, true)
            writer = BufferedWriter(fileWriter)
            writer.append(eventName)
            writer.append(",")
            writer.append(timeTaken.toString())
            writer.append("\r\n")
            Log.d(LOG_TAG, "Event:$eventName TimeTaken:$timeTaken")
        }
        catch (e: IOException) {
            Log.d(LOG_TAG, e.message ?: "")
        }
        finally {
            try {
                writer?.close()
            } catch (e: IOException) {
                Log.d(LOG_TAG, e.message ?: "")
            }
        }
    }

    private fun getFile(dir: File, fileName: String): File {
        val file = File(dir, fileName)

        if (!file.exists()) {
            val result = file.createNewFile()
            Log.d(LOG_TAG, "File creation result:" + result)
        }
        return file
    }
}