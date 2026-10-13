# Add project specific ProGuard rules here.
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.deathchat.app.data.api.models.** { *; }
-keep class io.socket.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
