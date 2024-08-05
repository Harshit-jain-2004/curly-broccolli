# Code style

There are two files used for code style in [android_studio settings](../androidstudio_settings)

- `stickynotes-codestyle.jar`
- `stickynotes-save-refactor-imports-macro.jar`

The first file has the default code style used in the project, the second one has a macro that can be associated with any shortcut so that it will organize imports, adjust the code to the code style settings and save the changes.

The settings can be imported in Android Studio by going to:
`File -> Settings -> Import Settings`

Once that the files are imported and Android Studio restarted, a shortcut can be assigned to the macro so it can be used quickly. For instance, `Ctrl+S`. To associate the macro with a shortcut use the following menu in Android Studio:
`Edit -> Macros -> Play saved macros`

# Automation

Our styles are enforced by ktlint. Pull requests will be run through a ktlint VSTS task to ensure that styles are correct. Similarly, gradle's linting task will run static analysis to ensure code is safe.
Most ktlint issues can be fixed automatically by the running `./gradlew ktlintFormat`.