# Getting Started

The SDK is setup as an Android Studio project with two modules - `noteslib`, containing SDK code, and `sample-app`, containing an example app which shows a basic SDK integration. The SDK is split into various packages for UI, sync, persistence, etc. The main files to start with are:

- [NotesLibrary.kt](../noteslib/src/main/kotlin/com/microsoft/notes/noteslib/NotesLibrary.kt) which contains the client facing interfaces and is the main entry point for the integrations.
- [sample-app code](../sample-app) which shows how SDK is used.

**Unidirectional data flow** architecture is used in the SDK. The [slides](https://speakerdeck.com/cesarvaliente/unidirectional-data-flow-on-android-using-kotlin) and the [blog post series](https://proandroiddev.com/unidirectional-data-flow-on-android-the-blog-post-part-1-cadcf88c72f5) will help with getting familar with this concept.

## Starting steps:

1. Download [Android Studio](https://developer.android.com/studio/).
2. Clone the project.
3. Open the project in Android Studio.
   - Start Android Studio.
   - Choose `Open an existing Android Studio project` option.
   - Choose the folder where the project is cloned.
4. The build will start automatically. You might be asked to install additional dependencies/accept Android licences during the process.
5. Attach the device via USB or install an emulator via `Tools -> AVD Manager`.
6. Run the sample-app via `Run -> Run 'sample-app'`.

Sample app supports AAD accounts. These can be generated in [Microsoft Demos](https://demos.microsoft.com/environments). 

1. Login with your work account.
2. Select `Create Tenant`
3. Select `Quick Tenant` (period can be any)
4. Select `Microsoft 365 Enterprise Demo Content`
5. A test account with username and password will be generated and can be used in the sample app.

## Using the SDK in your app

Integrating the StickyNotesSDK into your app should be easy enough as just adding some dependencies and start using it.

### Instructions

1- In your **root** `build.gradle` file, you should have our artifact repository as the snippet below shows:
```
allprojects {
    repositories {
        mavenCentral()
        google()

        //This repository is needed to fetch the stickynotes sdk from the microsoft maven repo
        maven {
            url 'https://office.pkgs.visualstudio.com/_packaging/stickynotes-android-sdk/maven/v1'
            credentials {
                username "AZURE_ARTIFACTS"
                password System.getenv("AZURE_ARTIFACTS_ENV_ACCESS_TOKEN") ?: "${azureArtifactsGradleAccessToken}"
            }
        }
    }    
}
```

As you can see here, we use two constants:
- `AZURE_ARTIFACTS_ENV_ACCESS_TOKEN`: it should be used by your **build machine** when building your app so it's able to download our SDK. This can be set as an environmental variable.
- `azureArtifactsGradleAccessToken`: it should be added to a **local file**, so you are able to build your app and get our SDK from your local machine.

The last property has to be added to your local `gradle.properties` file. This file **is not meant to be uploaded to our repo**, is just for you, a local file.

An example of how the `gradle.properties` file looks like is:
```
azureArtifactsGradleAccessToken=YOUR_TOKEN
```

To get a token that you can use here to download our SDK you can go [here](https://office.visualstudio.com/_usersSettings/tokens) and create a token under `office` account with the permission `Packaging (Read)`


2- In your **app** `build.gradle` file, you should add in the `dependencies` node:

`implementation(group: 'com.microsoft.stickynotes', name: 'android', version: 'x.x')`

Being `x.x` the version of the SDK you want to use.

Now you will be able to get the SDK.

3- Our SDK does not contain the dependencies it uses itself, so you have to download them as well by adding to your 
**app** `build.gradle`  in the `dependencies` node:

```
    implementation "androidx.fragment:fragment:1.0.0"
    implementation "androidx.annotation:annotation:1.0.0"
    implementation "androidx.legacy:legacy-support-core-ui:1.0.0"
    implementation "androidx.appcompat:appcompat:1.0.0"
    implementation "androidx.recyclerview:recyclerview:1.0.0"
    implementation "androidx.cardview:cardview:1.0.0"
    implementation "com.google.android.material:material:1.0.0"
    implementation "androidx.constraintlayout:constraintlayout:1.0.0"

    implementation "com.github.bumptech.glide:glide:4.13.2"
    implementation "androidx.room:room-runtime:2.0.0"
    implementation "com.google.code.gson:gson:2.8.9"
    implementation "com.squareup.okio:okio:1.15.0"
    implementation "com.squareup.okhttp3:okhttp:3.10.0"
    implementation "org.jdeferred:jdeferred-core:1.2.3"
    implementation "com.squareup.moshi:moshi:1.6.0"
```

4- If you are not using Kotlin in your project yet, you maybe need to add as well the **Kotlin** dependencies:

```
    implementation "org.jetbrains.kotlin:kotlin-stdlib:1.2.51"
```

And that's all! now you should be able to start using our SDK.
As mentioned before in this document, a good way to start is by having a look at the [sample-app](../sample-app) we have.