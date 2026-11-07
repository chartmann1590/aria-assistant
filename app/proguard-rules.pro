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

# ── Retrofit ──────────────────────────────────────────────────────────────────
-keepattributes Signature, InnerClasses, EnclosingMethod, Exceptions
-keepattributes *Annotation*, RuntimeVisibleAnnotations, RuntimeInvisibleAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# ── OkHttp / Okio ─────────────────────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**

# ── Kotlin Serialization ──────────────────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.aria.assistant.data.feedback.**$$serializer { *; }
-keepclassmembers class com.aria.assistant.data.feedback.** {
    *** Companion;
}
-keepclasseswithmembers class com.aria.assistant.data.feedback.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Amazon build fix (2026-11-07): a dependency bundles its own copy of framework classes under android.os,
# so R8 renamed android.os.Process members (crash on devices without Google Play services:
# NoSuchMethodError "No static method a()Z in class Landroid/os/Process" in GoogleApiHandler).
# Never rename or strip anything in the android.* namespace; the device's framework copy is the one that loads.
-keep class android.** { *; }
-keepnames class android.** { *; }

# LiteRT-LM (Gemma 4 chat): its native code calls the Java callback by name (onMessage/onDone);
# R8 renamed them and the first chat message aborted the app (NoSuchMethodError in nativeSendMessageAsync).
-keep class com.google.ai.edge.** { *; }
-dontwarn com.google.ai.edge.**
