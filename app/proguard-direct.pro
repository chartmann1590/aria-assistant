# Direct (GitHub / charleshartmann.com) build only (-Pstore=direct), added 2026-11-07.
# A dependency bundles its own copy of framework classes under android.os, so R8 renamed android.os.Process members
# (crash on devices without Google Play services: NoSuchMethodError "No static method a()Z in class Landroid/os/Process"
# in GoogleApiHandler; seen in the Amazon build). Never rename or strip anything in the android.* namespace.
-keep class android.** { *; }
-keepnames class android.** { *; }
