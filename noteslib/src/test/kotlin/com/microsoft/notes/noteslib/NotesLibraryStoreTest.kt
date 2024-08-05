package com.microsoft.notes.noteslib

import android.content.Context
import android.os.Build
import com.microsoft.notes.sync.RequestPriority
import com.microsoft.notes.utils.utils.Constants
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import org.hamcrest.CoreMatchers.`is` as iz

@Ignore("Ignore this test.We will enable it again once we move to androidX and update robolectric version")
@RunWith(RobolectricTestRunner::class)
@Config(
    constants = BuildConfig::class,
    sdk = intArrayOf(Build.VERSION_CODES.LOLLIPOP)
)
class NotesLibraryStoreTest {

    @Mock
    lateinit var context: Context

    @Mock
    lateinit var file: File

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun should_enable_persistence() {
        val notesLibraryStore = NotesLibraryStore("test app", isDebugMode = true)

        with(notesLibraryStore) {
            assertThat(sideEffects.isEmpty(), iz(true))
            enablePersistence(context, Constants.DATABASE_NAME, multiAccountEnabled = false)

            assertThat(sideEffects.isNotEmpty(), iz(true))
            assertThat(sideEffects.size, iz(1))
        }
    }

    @Test
    fun should_enable_sync() {
        val notesLibraryStore = NotesLibraryStore("test app", isDebugMode = true)
        val experimentFeatureFlags = ExperimentFeatureFlags()
        with(notesLibraryStore) {
            assertThat(sideEffects.isEmpty(), iz(true))

            whenever(context.filesDir).then { File("") }
            enableSync(
                context, context.filesDir, "", experimentFeatureFlags,
                autoDiscoverHost = "https://outlook.office365.com"
            ) { RequestPriority.foreground }

            assertThat(sideEffects.isNotEmpty(), iz(true))
            assertThat(sideEffects.size, iz(1))
        }
    }
}
