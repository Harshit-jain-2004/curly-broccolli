# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in D:\Code\Android/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep source file and line numbers for better crash logs
-keepattributes SourceFile,LineNumberTable

# Avoid throws declarations getting removed from retrofit service definitions
-keepattributes Exceptions

# AppCompat MenuBuilder to be able to show icons in menus
-keep class **.MenuBuilder {
    boolean mOptionalIconsVisible;
}

-dontwarn kotlin.**
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
}

# Architecture lib (used in Room)
-dontwarn android.arch.util.paging.CountedDataSource
-dontwarn android.arch.paging.PositionalDataSource
-dontwarn android.arch.persistence.room.paging.LimitOffsetDataSource

# Gson uses generic type information stored in a class file when working with fields. Proguard
# removes such information by default, so configure it to keep all of it.
-keepattributes Signature,InnerClasses
# Gson specific classes
-dontwarn sun.misc.Unsafe

# Okio has some stuff not available on Android.
-dontwarn java.nio.file.*
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

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

-keepclassmembers enum * { *; }

-dontwarn javax.**

-keep class kotlin.text.RegexOption { *; }
-keep class kotlin.jvm.internal.DefaultConstructorMarker { *; }

#-- We keep all our SDK classes --
-keep class com.microsoft.notes.** {*; }

#-keep class com.microsoft.notes.sync.ApiPromise { *;}
-dontwarn nl.komponents.kovenant.**

-dontnote android.net.http.*
-dontnote org.apache.commons.codec.**
-dontnote org.apache.http.**

-dontwarn com.google.android.material.R**
