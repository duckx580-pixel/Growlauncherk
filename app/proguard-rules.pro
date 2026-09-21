# libgrowtopia.so resolves these by name at runtime, so nothing they expose may
# be renamed or stripped.

# Classes the engine looks up with FindClass / calls back into.
-keep class com.rtsoft.growtopia.** { *; }
-keep class com.ubisoft.** { *; }
-keep class com.gentz.launcher.App { *; }

# Every native method and anything annotated for JNI.
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# UsercentricsManager_OnConsentFetchedSuccess reads these two INSTANCE FIELDS
# directly via GetFieldID("templateId", "Ljava/lang/String;") and
# GetFieldID("status", "Z"). If R8 renames them the engine gets a null field id,
# builds an empty consent vector, and UserConsentController never leaves the
# screen — i.e. the game hangs after "Play Online".
-keep class com.usercentrics.sdk.UsercentricsServiceConsent { *; }
-keep class com.usercentrics.sdk.UsercentricsConsentHistoryEntry { *; }
-keep class com.usercentrics.sdk.models.settings.UsercentricsConsentType { *; }
