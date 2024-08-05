package com.microsoft.notes.noteslib

import android.content.Context
import com.microsoft.notes.utils.logging.Logger
import com.microsoft.notes.utils.logging.NotesLogger
import com.microsoft.notes.utils.logging.TelemetryLogger
import com.microsoft.notes.utils.threading.ThreadExecutor
import org.hamcrest.CoreMatchers.nullValue
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.hamcrest.CoreMatchers.`is` as iz

class NotesLibraryConfigurationTest {

    @Mock
    lateinit var context: Context
    @Mock
    var clientThreadMock: ThreadExecutor? = null
    @Mock
    val loggerMock: Logger? = null
    @Mock
    val telemetryLoggerMock: TelemetryLogger? = null

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        // Set the builder, is an object so its members are static to default
        NotesLibraryConfiguration.Builder
            .withUserAgent("")
            .withClientThread(null)
            .withLogger(null)
            .withTelemetryLogger(null)
            .withExperimentalOptions()
            .withUiOptions()
            .withTheme(null)
    }

    @Test
    fun `should create default NotesLibraryConfiguration`() {
        val notesLibraryConfiguration = NotesLibraryConfiguration.Builder
            .build(context, "test app", true)

        with(notesLibraryConfiguration) {
            assertThat(context, iz(context))
            assertThat(userAgent, iz(""))
            assertThat(clientThread, iz(nullValue()))
            assertThat(notesLogger, iz(NotesLogger(null, null)))
            assertThat(uiOptionFlags, iz(UiOptionFlags()))
            assertThat(experimentFeatureFlags, iz(ExperimentFeatureFlagsBuilder().build()))
            assertThat(store.createdByApp, iz("test app"))
            assertThat(theme, iz(NotesThemeOverride.default))
        }
    }

    @Test
    fun `should create NotesLibraryConfiguration with Persistence disabled`() {
        val notesLibraryConfiguration = NotesLibraryConfiguration.Builder
            .build(context, "test app", true)

        with(notesLibraryConfiguration) {
            assertThat(context, iz(context))
            assertThat(userAgent, iz(""))
            assertThat(clientThread, iz(nullValue()))
            assertThat(notesLogger, iz(NotesLogger(null, null)))
            assertThat(uiOptionFlags, iz(UiOptionFlags()))
            assertThat(experimentFeatureFlags, iz(ExperimentFeatureFlagsBuilder().build()))
            assertThat(store.createdByApp, iz("test app"))
            assertThat(theme, iz(NotesThemeOverride.default))
        }
    }

    @Test
    fun `should create NotesLibraryConfiguration with UserAgent`() {
        val notesLibraryConfiguration = NotesLibraryConfiguration.Builder
            .withUserAgent(userAgent = "userAgent")
            .build(context, appName = "test app", isDebugMode = true)

        with(notesLibraryConfiguration) {
            assertThat(context, iz(context))
            assertThat(userAgent, iz("userAgent"))
            assertThat(clientThread, iz(nullValue()))
            assertThat(notesLogger, iz(NotesLogger(null, null)))
            assertThat(uiOptionFlags, iz(UiOptionFlags()))
            assertThat(experimentFeatureFlags, iz(ExperimentFeatureFlagsBuilder().build()))
            assertThat(store.createdByApp, iz("test app"))
            assertThat(theme, iz(NotesThemeOverride.default))
        }
    }

    @Test
    fun `should create NotesLibraryConfiguration with client thread`() {
        val notesLibraryConfiguration = NotesLibraryConfiguration.Builder
            .withClientThread(clientThreadMock)
            .build(context, "test app", true)

        with(notesLibraryConfiguration) {
            assertThat(context, iz(context))
            assertThat(userAgent, iz(""))
            assertThat(clientThread, iz(clientThreadMock))
            assertThat(notesLogger, iz(NotesLogger(null, null)))
            assertThat(uiOptionFlags, iz(UiOptionFlags()))
            assertThat(experimentFeatureFlags, iz(ExperimentFeatureFlagsBuilder().build()))
            assertThat(store.createdByApp, iz("test app"))
            assertThat(theme, iz(NotesThemeOverride.default))
        }
    }

    @Test
    fun `should create NotesLibraryConfiguration with logger`() {
        val notesLibraryConfiguration = NotesLibraryConfiguration.Builder
            .withLogger(loggerMock)
            .build(context, "test app", true)

        with(notesLibraryConfiguration) {
            assertThat(context, iz(context))
            assertThat(userAgent, iz(""))
            assertThat(clientThread, iz(nullValue()))
            assertThat(notesLogger, iz(NotesLogger(loggerMock, null)))
            assertThat(uiOptionFlags, iz(UiOptionFlags()))
            assertThat(experimentFeatureFlags, iz(ExperimentFeatureFlagsBuilder().build()))
            assertThat(store.createdByApp, iz("test app"))
            assertThat(theme, iz(NotesThemeOverride.default))
        }
    }

    @Test
    fun `should create NotesLibraryConfiguration with telemetry logger`() {
        val notesLibraryConfiguration = NotesLibraryConfiguration.Builder
            .withTelemetryLogger(telemetryLoggerMock)
            .build(context, "test app", true)

        with(notesLibraryConfiguration) {
            assertThat(context, iz(context))
            assertThat(userAgent, iz(""))
            assertThat(clientThread, iz(nullValue()))
            assertThat(notesLogger, iz(NotesLogger(null, telemetryLoggerMock)))
            assertThat(uiOptionFlags, iz(UiOptionFlags()))
            assertThat(experimentFeatureFlags, iz(ExperimentFeatureFlagsBuilder().build()))
            assertThat(store.createdByApp, iz("test app"))
            assertThat(theme, iz(NotesThemeOverride.default))
        }
    }

    @Test
    fun `should create NotesLibraryConfiguration with logger and telemetry logger`() {
        val notesLibraryConfiguration = NotesLibraryConfiguration.Builder
            .withLogger(loggerMock)
            .withTelemetryLogger(telemetryLoggerMock)
            .build(context, "test app", true)

        with(notesLibraryConfiguration) {
            assertThat(context, iz(context))
            assertThat(userAgent, iz(""))
            assertThat(clientThread, iz(nullValue()))
            assertThat(
                notesLogger,
                iz(
                    NotesLogger(loggerMock, telemetryLoggerMock)
                )
            )
            assertThat(uiOptionFlags, iz(UiOptionFlags()))
            assertThat(experimentFeatureFlags, iz(ExperimentFeatureFlagsBuilder().build()))
            assertThat(store.createdByApp, iz("test app"))
            assertThat(theme, iz(NotesThemeOverride.default))
        }
    }

    @Test
    fun `should create NotesLibraryConfiguration with non-default UI options`() {
        val testUiOptionFlags = UiOptionFlags(
            showNotesListAddNoteButton = true,
            hideNoteOptionsFeedbackButton = true,
            hideNoteOptionsShareButton = true
        )
        val notesLibraryConfiguration = NotesLibraryConfiguration.Builder.withUiOptions(testUiOptionFlags).build(
            context,
            "test " +
                "app",
            true
        )

        with(notesLibraryConfiguration) {
            assertThat(context, iz(context))
            assertThat(userAgent, iz(""))
            assertThat(clientThread, iz(nullValue()))
            assertThat(notesLogger, iz(NotesLogger(null, null)))
            assertThat(uiOptionFlags, iz(testUiOptionFlags))
            assertThat(experimentFeatureFlags, iz(ExperimentFeatureFlagsBuilder().build()))
            assertThat(store.createdByApp, iz("test app"))
            assertThat(theme, iz(NotesThemeOverride.default))
        }
    }

    @Test
    fun `should create NotesLibraryConfiguration with a theme override`() {
        val themeOverride = NotesThemeOverride(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)
        val testUiOptionFlags = UiOptionFlags(
            showNotesListAddNoteButton = true,
            hideNoteOptionsFeedbackButton = true,
            hideNoteOptionsShareButton = true
        )

        val notesLibraryConfiguration = NotesLibraryConfiguration.Builder.withUiOptions(
            testUiOptionFlags
        )
            .withTheme(themeOverride)
            .build(context, "test app", true)

        with(notesLibraryConfiguration) {
            assertThat(context, iz(context))
            assertThat(userAgent, iz(""))
            assertThat(clientThread, iz(nullValue()))
            assertThat(notesLogger, iz(NotesLogger(null, null)))
            assertThat(uiOptionFlags, iz(testUiOptionFlags))
            assertThat(experimentFeatureFlags, iz(ExperimentFeatureFlagsBuilder().build()))
            assertThat(store.createdByApp, iz("test app"))
            assertThat(theme, iz(themeOverride))
        }
    }

    @Test
    fun `should create NotesLibraryConfiguration with microphone button`() {
        val callback = { _: Context -> Unit }
        val notesLibraryConfiguration = NotesLibraryConfiguration.Builder
            .withExperimentalOptions()
            .withMicrophoneButtonInEditOptions(callback)
            .build(context, "test app", true)

        with(notesLibraryConfiguration) {
            assertThat(context, iz(context))
            assertThat(userAgent, iz(""))
            assertThat(clientThread, iz(nullValue()))
            assertThat(notesLogger, iz(NotesLogger(null, null)))
            assertThat(uiOptionFlags, iz(UiOptionFlags()))
            assertThat(experimentFeatureFlags, iz(ExperimentFeatureFlagsBuilder().build()))
            assertThat(store.createdByApp, iz("test app"))
            assertThat(theme, iz(NotesThemeOverride.default))
            assertThat(onMicrophoneButtonClick, iz(callback))
        }
    }
}
