# Retrofit / Gson DTOs are reflective.
-keep class com.tvsencilla.iptv.data.xtream.dto.** { *; }
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*

-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**

# Media3 keeps its own rules, but the extractor set is looked up reflectively.
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
