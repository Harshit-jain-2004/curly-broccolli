-dontwarn kotlin.**
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
}

# Gson uses generic type information stored in a class file when working with fields. Proguard
# removes such information by default, so configure it to keep all of it.
-keepattributes Signature,InnerClasses
# Gson specific classes
-dontwarn sun.misc.Unsafe

# Okio has some stuff not available on Android.
-dontwarn java.nio.file.*

# Oltu has some stuff not available on Android (javax.servlet), we don't use (slf4j)
# and not included because it is available on Android (json).
-dontwarn javax.servlet.http.**
-dontwarn org.json.**
-dontwarn org.slf4j.**

# okio
-dontwarn okio.**

# Moshi
-keepclassmembers class ** {
    @com.squareup.moshi.FromJson *;
    @com.squareup.moshi.ToJson *;
}

-dontwarn javax.annotation.**
-keepclasseswithmembers class * {
    @com.squareup.moshi.* <methods>;
}
-keep @com.squareup.moshi.JsonQualifier interface *
-keep class **JsonAdapter {
    <init>(...);
    <fields>;
}
-keepnames @com.squareup.moshi.JsonClass class *

#kotlin
-keep class kotlin.text.RegexOption { *; }
-keep class kotlin.jvm.internal.DefaultConstructorMarker { *; }
-keep class kotlin.reflect.jvm.internal.impl.types.Variance { *; }
-keep class kotlin.reflect.jvm.internal.impl.builtins.PrimitiveType { *; }
-keep class kotlin.reflect.jvm.internal.impl.protobuf.WireFormat { *; }

#notes
-keep class com.microsoft.notes.richtext.scheme.** { *; }
-keep class com.microsoft.notes.models.** { *; }

-dontwarn com.microsoft.notes.richtext.scheme.**
-dontwarn com.microsoft.notes.richtext.editor.**
-keep class com.microsoft.notes.notesview.recyclerview.NoteItemComponent { *; }

-keep class com.microsoft.notes.sync.ApiPromise { *;}

-keep class com.microsoft.notes.platform.glide.NotesGlideModule

#others
-dontnote android.net.http.*
-dontnote org.apache.commons.codec.**
-dontnote org.apache.http.**
-keepclassmembers enum * { *; }
-dontwarn javax.**

#okhttp
-dontwarn sun.misc.**
-dontwarn okhttp3.**
-dontwarn org.conscrypt.**
# A resource is loaded with a relative path so the package of this class must be preserved.
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

#Sync server error response
-keep class com.microsoft.notes.sync.AutoDiscoverErrorDetailsJsonAdapter { *; }
-keep class com.microsoft.notes.sync.Error { *; }
-keep class com.microsoft.notes.sync.ErrorDetails { *; }
-keep class com.microsoft.notes.sync.ErrorDetailsJsonAdapter { *; }

#Sync Outbound queue Persistence files
-keep class com.microsoft.notes.sync.models.** { *; }
-keep class com.microsoft.notes.sync.ApiRequestOperation { *; }
-keep class com.microsoft.notes.sync.ApiRequestOperation$ValidApiRequestOperation$Sync { *; }
-keep class com.microsoft.notes.sync.ApiRequestOperation$ValidApiRequestOperation$CreateNote { *; }
-keep class com.microsoft.notes.sync.ApiRequestOperation$ValidApiRequestOperation$UpdateNote { *; }
-keep class com.microsoft.notes.sync.ApiRequestOperation$ValidApiRequestOperation$DeleteNote { *; }
-keep class com.microsoft.notes.sync.ApiRequestOperation$ValidApiRequestOperation$DeleteSamsungNote { *; }
-keep class com.microsoft.notes.sync.ApiRequestOperation$ValidApiRequestOperation$GetNoteForMerge { *; }
-keep class com.microsoft.notes.sync.ApiRequestOperation$ValidApiRequestOperation$UploadMedia { *; }
-keep class com.microsoft.notes.sync.ApiRequestOperation$ValidApiRequestOperation$DownloadMedia { *; }
-keep class com.microsoft.notes.sync.ApiRequestOperation$ValidApiRequestOperation$DeleteMedia { *; }
-keep class com.microsoft.notes.sync.ApiRequestOperation$ValidApiRequestOperation$UpdateMediaAltText { *; }
-keep class com.microsoft.notes.sync.ApiRequestOperation$InvalidApiRequestOperation$InvalidUpdateNote { *; }
-keep class com.microsoft.notes.sync.ApiRequestOperation$InvalidApiRequestOperation$InvalidDeleteNote { *; }
-keep class com.microsoft.notes.sync.ApiRequestOperation$InvalidApiRequestOperation$InvalidUploadMedia { *; }
-keep class com.microsoft.notes.sync.ApiRequestOperation$InvalidApiRequestOperation$InvalidDeleteMedia { *; }
-keep class com.microsoft.notes.sync.ApiRequestOperation$InvalidApiRequestOperation$InvalidUpdateMediaAltText { *; }
