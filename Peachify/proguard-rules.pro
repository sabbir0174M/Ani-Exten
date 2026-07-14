# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# signAndroidApp task. For more details, see
# http://developer.android.com/guide/developing/tools/proguard.html

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

# CloudStream
-keep class com.lagradost.cloudstream3.* { *; }
-keepclassmembers class com.lagradost.cloudstream3.* { *; }

# Keep extension classes
-keep class com.lagradost.cloudstream3.Peachify { *; }
-keep class com.lagradost.cloudstream3.extractors.Peachify { *; }
-keep class com.lagradost.cloudstream3.PeachifyDecrypt { *; }

# Preserve line numbers for debugging
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
