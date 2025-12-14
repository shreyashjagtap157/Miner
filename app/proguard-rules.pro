# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preserve the line number information for debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# Hide the original source file name in stack traces.
-renamesourcefileattribute SourceFile

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Room database entities
-keep class com.meetmyartist.miner.data.model.** { *; }
-keep class com.meetmyartist.miner.data.local.** { *; }

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ComponentSupplier { *; }

# Keep Firebase classes
-keep class com.google.firebase.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# Keep Google Sign-In & Credential Manager
-keep class com.google.android.gms.auth.api.signin.** { *; }
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }

# Keep Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Keep Gson model classes
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep Compose
-keep class androidx.compose.** { *; }

# Keep Vico chart library
-keep class com.patrykandpatrick.vico.** { *; }

# Keep crypto mining algorithms
-keep class com.meetmyartist.miner.mining.** { *; }
-keep class com.meetmyartist.miner.network.** { *; }

# Prevent obfuscation of JNI native methods
-keepclassmembers class com.meetmyartist.miner.mining.NativeMiner {
    native <methods>;
}