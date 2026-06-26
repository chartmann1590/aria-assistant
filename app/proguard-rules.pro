# ── Kotlin ───────────────────────────────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions, EnclosingMethod
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings { <fields>; }
-keepclassmembers class kotlin.Lazy { *; }

# ── Coroutines ────────────────────────────────────────────────────────────────
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ── Hilt / Dagger ─────────────────────────────────────────────────────────────
-keep @dagger.hilt.android.HiltAndroidApp class *
-keep @dagger.hilt.android.AndroidEntryPoint class *
-keep @dagger.hilt.InstallIn class *
-keep @dagger.Module class * { *; }
-keep @dagger.hilt.migration.DisableInstallInCheck class *
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
    @javax.inject.Inject <fields>;
}
-dontwarn dagger.**

# ── Room ──────────────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.TypeConverters class * { *; }
-keepclassmembers @androidx.room.Entity class * { *; }

# ── Domain & data models (serialised by Room / DataStore) ────────────────────
-keep class com.aria.assistant.domain.model.** { *; }
-keep class com.aria.assistant.data.model.** { *; }
-keep class com.aria.assistant.data.database.** { *; }

# ── Play Billing ──────────────────────────────────────────────────────────────
-keep class com.android.billingclient.** { *; }
-dontwarn com.android.billingclient.**

# ── LiteRT / TFLite (Gemma on-device inference) ───────────────────────────────
-keep class com.google.mediapipe.** { *; }
-keep class org.tensorflow.lite.** { *; }
-dontwarn com.google.mediapipe.**
-dontwarn org.tensorflow.**

# ── ONNX Runtime (Whisper STT, wake word) ────────────────────────────────────
-keep class ai.onnxruntime.** { *; }
-dontwarn ai.onnxruntime.**

# ── Sherpa-ONNX (Piper TTS) ───────────────────────────────────────────────────
-keep class com.k2fsa.sherpa.** { *; }
-dontwarn com.k2fsa.sherpa.**

# ── OpenWakeWord ──────────────────────────────────────────────────────────────
-keep class com.ariya.openwakeword.** { *; }
-dontwarn com.ariya.openwakeword.**

# ── Android system-bound components (must be reachable by name) ───────────────
-keep class com.aria.assistant.AriaApplication
-keep class com.aria.assistant.presentation.MainActivity
-keep class com.aria.assistant.service.AriaForegroundService
-keep class com.aria.assistant.service.AriaBootReceiver
-keep class com.aria.assistant.permission.AriaNotificationListener
-keep class com.aria.assistant.permission.AriaAccessibilityService

# ── Parcelable ────────────────────────────────────────────────────────────────
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# ── Enums ─────────────────────────────────────────────────────────────────────
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ── Native methods ────────────────────────────────────────────────────────────
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# ── Compose (suppress R8 warnings from generated code) ───────────────────────
-dontwarn androidx.compose.**
-keep class androidx.compose.runtime.** { *; }
