# Direct (GitHub / charleshartmann.com) build only (-Pstore=direct), added 2026-11-07.
# A dependency bundles its own copy of framework classes under android.os, so R8 renamed android.os.Process members
# (crash on devices without Google Play services: NoSuchMethodError "No static method a()Z in class Landroid/os/Process"
# in GoogleApiHandler; seen in the Amazon build). Never rename or strip anything in the android.* namespace.
-keep class android.** { *; }
-keepnames class android.** { *; }

# LiteRT-LM (on-device Gemma): the native library calls back into JniMessageCallback/JniInferenceCallback by name over JNI.
# R8 stripped those interface methods (0 methods in classes.dex of build 8), so the first chat message would end in
# NoSuchMethodError (seen on the amazon branch). Keep the whole LiteRT/AI Edge namespace. Added 2026-11-07.
-keep class com.google.ai.edge.** { *; }
-keepnames class com.google.ai.edge.** { *; }
-keep interface com.google.ai.edge.** { *; }
