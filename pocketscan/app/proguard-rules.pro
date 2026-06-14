-dontwarn org.jetbrains.annotations.**

# Room: keep entities + DAOs (annotations + generated impls reference these by name)
-keep class com.pocketscan.app.data.** { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# Compose / Kotlin metadata
-keepclassmembers class kotlin.Metadata { *; }
-dontwarn kotlin.Unit

# ML Kit document scanner — keep entry points reflected at runtime
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.mlkit.** { *; }
-dontwarn com.google.mlkit.**
