# SDK Handover

Please refer to [README](../README.md) for additional development documentation, covering building the project, release process, localisation, testing, etc.

## Partners

The following is the list of the current partners, the state of integration and contact information.
Requests by partners are tagged with the labels in the issues list, linked below.

### OneNote

Integrated, using SDK releases.

Contacts:

- Parag Palekar <ppalekar@microsoft.com> - EM
- Amrita Rout <amritar@microsoft.com> - PM
- Tarun Singal <tasing@microsoft.com> - developer
- Nimisha Tantuway <Nimisha.Tantuway@microsoft.com> - developer
- Abhishek Deswal <abdes@microsoft.com> - developer

[OneNote issues](https://github.com/microsoft-notes/notes-android-sdk/issues?q=is%3Aopen+is%3Aissue+label%3A%22%F0%9F%92%9C+OneNote%22)

### Union

Integrated, using SDK releases.

Union integration is managed by OneNote, so OneNote contacts can be used for Union related questions as well.

Contacts:

- Anakar Parida <Anakar.Parida@microsoft.com> - developer

[Union issues](https://github.com/microsoft-notes/notes-android-sdk/issues?q=is%3Aopen+is%3Aissue+label%3A%22%E2%9D%A4%EF%B8%8F+Union%22)

### Launcher

Integrated, using an older fork the SDK without UI elements support. Expected to switch to using SDK releases and start using SDK fully, but current integration timeline not known.

Contacts:

- Ezra Park <Ezra.Park@microsoft.com> - PM
- Gorden Lin <gorden.lin@microsoft.com> - developer lead
- Yuli Wang <yuliwa@microsoft.com> - developer

[Launcher issues](https://github.com/microsoft-notes/notes-android-sdk/issues?q=is%3Aopen+is%3Aissue+label%3A%22%F0%9F%92%99+Launcher%22)

## Current state

[Q3 plan in OneNote](https://microsoft.sharepoint-df.com/teams/remember/_layouts/15/WopiFrame.aspx?sourcedoc={2099a33d-7472-4545-a3f1-9011f044f047}&action=edit&wd=target%28Product.one%7C06de3841-ce53-4eaa-bf55-7f0d4a4b962d%2FQ3%20Plan%7C524d542c-2921-4f23-890f-a60b06e79cd6%2F%29&wdorigin=703)

Work in progress:

- [Version numbers](https://github.com/microsoft-notes/notes-android-sdk/issues/717) - currently, there is a manual process to increment version numbers, improvements could be made to this.
- [Improve localisation](https://github.com/microsoft-notes/notes-android-sdk/issues/551) - currently, strings need to be checked in manually, better process could be set up for this.
- Realtime in Android SDK. This has been released in the SDK with a [feature flag](../noteslib/src/main/kotlin/com/microsoft/notes/noteslib/NotesLibraryConfiguration.kt#L38). The sample app uses realtime by default. Partners are expected to start using this feature, potentially as a staged roll out, however, this has not been committed to for any particular release - need to discuss further with partners and see when it can be rolled out to users.
- GSON parser roll out. This feature has been added to the SDK with a [feature flag](../noteslib/src/main/kotlin/com/microsoft/notes/noteslib/NotesLibraryConfiguration.kt#L33). This replaces old manual JSON parsing with using a GSON library instead. Partners should be rolling this out feature, and eventually feature flag can be removed. Need to follow up with partners on when this will be fully rolled out.

Q3 commitments:

- [Error UI in SDK](https://github.com/microsoft-notes/notes-android-sdk/issues/524)
- [Android widget](https://github.com/microsoft-notes/notes-android-sdk/issues/715)
- [Future notes UI](https://github.com/microsoft-notes/notes-android-sdk/issues/380)
- Improve feature flag interface (across clients) - this would allow SDK to roll out features without always needing the client to integrate specific feature flags and rollout.
- Docs page for partners (non-engineering) - ie, longer term roadmap, etc.

## Feature roll out

Feature roll out is controlled by partners through their processes. One of the commitments for Q3 is to make this better and allow SDK to control feature roll out, however, this needs to be coordinated with partners.

## Email channel

Sticky Notes SDK Partners <NotesSDKPartners@microsoft.com> is used for communicating releases.
