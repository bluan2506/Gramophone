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
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
# -renamesourcefileattribute SourceFile

# enabling obfuscation would break some self-reflection in the app
-dontobfuscate

# reflection by androidx via theme attr viewInflaterClass
-keep class com.musicdownloader.musicfreeapp825v2.logic.ui.ViewCompatInflater { *; }

# reflection by lyric getter xposed
-keep class androidx.media3.common.util.Util {
    public static void setForegroundServiceNotification(...);
}

# JNI
-keep class org.nift4.gramophone.hificore.NativeTrack {
    onAudioDeviceUpdate(...);
    onUnderrun(...);
    onMarker(...);
    onNewPos(...);
    onStreamEnd(...);
    onNewIAudioTrack(...);
    onNewTimestamp(...);
    onLoopEnd(...);
    onBufferEnd(...);
    onMoreData(...);
    onCanWriteMoreData(...);
}

# Room 2.4.2 (transitive via androidx.work) only ships
# "-keep class * extends androidx.room.RoomDatabase", and R8 full mode does not implicitly
# keep the default constructor of a kept class. Room resolves <Db>_Impl reflectively and
# calls newInstance(), so the stripped no-arg ctor made WorkManager's startup initializer
# throw "Failed to create an instance of androidx.work.impl.WorkDatabase".
-keep class * extends androidx.room.RoomDatabase { <init>(); }

# không ghi log khi build release apk
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}

# Lua
-keep class com.videoapps.lib.abcdef {
    native <methods>;
    *;
}

-keep class com.music.searchapi.callback.** { *; }
-keep class com.music.searchapi.object.VideoEntity { *; }
-keep class com.music.searchapi.ApiServices { *; }

-keep class com.videoapps.lib.** { *; }

-keep class Version { *; }

-keep class org.keplerproject.luajava.** { *; }
-keep class org.luaj.** { *; }
