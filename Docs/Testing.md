# Testing

The project has two different types of tests. Tests run when new PR is open, but running locally before pushing commits will save time.

## Unit Testing

Unit tests are implemented using junit. They can be run with the following command:

`./gradlew testDebugUnitTest`

## Instrumentation Testing

Instrumentation tests use Android dependencies and need a device or an emulator. They can be run with the following command:

`./gradlew connectedDebugAndroidTest`

### Espresso Tests

UI automation using Espresso has been configured with tests in `sample-app/src/androidTest/kotlin`. To run in Android Studio, right-click the `kotlin` folder and select `Run 'All Tests'`. Alternatively, you can run individual tests like you would a unit test. The following instructions must be followed to run the tests successfully on a physical device:

- Enable developer mode and USB debugging
- In developer options, set `Window animation scale`, `Transition animation scale`, and `Animator duration scale` to `Off`
- Ensure that the phone is not asleep
- Turn on airplane mode
- Sign into a valid account such that when opening `sample-app`, the notes list is in view (e.g., and not the sign in screen)

## Static Analysis (detekt)

[detekt](https://github.com/arturbosch/detekt) is a static analyser for Android and can be used to find a variety of issues in the code. The configuration is found in `noteslib\detekt.yml`.

`./gradlew detekt`

You can also configure Android Studio/IntelliJ to add error highlighting for violated rules via detekt's official [plugin](https://github.com/arturbosch/detekt-intellij-plugin)