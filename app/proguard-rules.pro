# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preserve line number information for debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# Hide the original source file name.
-renamesourcefileattribute SourceFile

# RouteSMS data classes (Gson serialization)
-keep class com.routesms.data.FilterRule { *; }
-keep class com.routesms.data.FilterType { *; }
-keep class com.routesms.data.FilterTarget { *; }
-keep class com.routesms.data.ForwardedMessage { *; }

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Exceptions

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Slack webhook model
-keep class com.routesms.slack.** { *; }

# Keep BroadcastReceiver and Service
-keep class com.routesms.receiver.SMSReceiver { *; }
-keep class com.routesms.service.** { *; }
