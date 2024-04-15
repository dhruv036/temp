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
-keepclassmembers class io.stempedia.pictoblox.** {
      *;
    }
-keepclassmembers class com.jiangdg.** {
      *;
    }

 -keepclassmembers class com.esafirm.** {
       *;
     }

 -keep public class com.google.firebase.** { *; }
 -keep class com.google.android.gms.internal.** { *; }
 -keepclasseswithmembers class com.google.firebase.FirebaseException

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile


-keep public class io.stempedia.pictoblox.connectivity.CommunicationHandlerWithPictoBloxWeb$PictobloxChromeClient
-keep public class * implements io.stempedia.pictoblox.connectivity.CommunicationHandlerWithPictoBloxWeb$PictobloxChromeClient
-keepclassmembers class io.stempedia.pictoblox.connectivity.CommunicationHandlerWithPictoBloxWeb$PictobloxChromeClient {
    <methods>;
}
-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken
-keep public class * implements java.lang.reflect.Type



-keepattributes JavascriptInterface

#-printusage <output-dir>/usage.txt
