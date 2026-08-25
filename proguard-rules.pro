# Keep Firebase RTDB model classes (reflection-based deserialization)
-keep class com.zedge.automation.data.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
