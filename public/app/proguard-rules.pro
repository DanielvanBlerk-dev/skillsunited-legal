# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

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

# Keep Firebase classes
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Keep data classes
-keep class com.dkvb.skillswap.User { *; }
-keep class com.dkvb.skillswap.Message { *; }
-keep class com.dkvb.skillswap.InboxMessage { *; }

# Keep Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep Skydoves color picker
-keep class com.skydoves.colorpickerview.** { *; }

# Keep ThemeManager
-keep class com.dkvb.skillswap.ThemeManager { *; }

# Prevent stripping of Firestore model classes
-keepclassmembers class com.dkvb.skillswap.** {
    public <init>();
    *;
}