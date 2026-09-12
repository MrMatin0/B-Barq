# ============================================================================
# B-Barq R8 configuration
#
# `isMinifyEnabled = true` on release, so everything below is load-bearing.
# Rules are grouped per library with a note on WHY each one exists; delete a
# group only when the library it names is gone from the build.
#
# Most AndroidX/Square libraries ship consumer rules inside their AAR/JAR, and
# AGP's proguard-android-optimize.txt covers the platform basics. The rules here
# are the ones nobody else can write for us (this app's own classes) plus
# explicit belt-and-braces keeps for the reflective parts of the stack.
# ============================================================================

# ----------------------------------------------------------------------------
# Crash reports
#
# Without these two, every stack trace from a shipped build is line-number-free
# and cannot be deobfuscated at all. mapping.txt is uploaded as a build artifact
# by .github/workflows/release.yml; keep it for as long as the build is live.
# ----------------------------------------------------------------------------
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Generic signatures (Retrofit return types, kotlinx-serialization type
# arguments), annotations (both libraries read them at runtime) and the
# enclosing-method info Kotlin lambdas and inner classes need.
-keepattributes Signature
-keepattributes InnerClasses,EnclosingMethod
-keepattributes *Annotation*,AnnotationDefault
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keepattributes RuntimeVisibleTypeAnnotations

# ----------------------------------------------------------------------------
# kotlinx.serialization
#
# The compiler plugin generates a `$$serializer` object per @Serializable class
# and looks it up through the Companion. R8 cannot see those references, so
# without these rules API responses fail to decode ONLY in release builds.
# Source: kotlinx.serialization README, adapted to this app's package.
# ----------------------------------------------------------------------------
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep `serializer()` and the Companion of every @Serializable class.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# This app's generated serializers, kept with their descriptors so field names
# survive obfuscation (the JSON keys ARE the field names).
-keep,includedescriptorclasses class com.aliJafari.bbarq.**$$serializer { *; }
-keepclassmembers class com.aliJafari.bbarq.** {
    *** Companion;
}

# Enum values used as serialized names, and enums in general (valueOf is
# reflective).
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ----------------------------------------------------------------------------
# Retrofit 2 + OkHttp 5
#
# Retrofit builds implementations of the API interfaces via a dynamic proxy and
# reads their generic return types and annotations at runtime.
# ----------------------------------------------------------------------------
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# Suspend functions return Continuation-typed parameters that Retrofit inspects.
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Retrofit's optional integrations that this app does not ship.
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**

# OkHttp references optional TLS providers reflectively; none are bundled.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ----------------------------------------------------------------------------
# Room
#
# Room instantiates the generated *_Impl classes by name and reads @Entity /
# @Dao metadata. The migration tests additionally load exported schemas.
# ----------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase {
    <init>();
}
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.paging.**

# ----------------------------------------------------------------------------
# Hilt / Dagger
#
# Hilt ships consumer rules, but the injected constructors and fields of this
# app's own classes still need to survive.
# ----------------------------------------------------------------------------
-keepclasseswithmembernames,includedescriptorclasses class * {
    @javax.inject.Inject <init>(...);
}
-keepclassmembers,allowobfuscation class * {
    @javax.inject.Inject <fields>;
    @javax.inject.Inject <methods>;
}
-keep,allowobfuscation @interface dagger.hilt.**
-keep,allowobfuscation @interface javax.inject.**

# ----------------------------------------------------------------------------
# WorkManager
#
# Workers are instantiated reflectively by class name (androidx.hilt:hilt-work
# covers the injected ones, but not the plain ones).
# ----------------------------------------------------------------------------
-keep class * extends androidx.work.ListenableWorker {
    public <init>(...);
}
-keep class * extends androidx.work.WorkerFactory { *; }

# ----------------------------------------------------------------------------
# Glance app widget
#
# OutageWidgetReceiver is referenced only from AndroidManifest.xml (AGP keeps
# manifest-declared components), but Glance also reaches its GlanceAppWidget and
# ActionCallback implementations reflectively across process restarts.
# ----------------------------------------------------------------------------
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidget { *; }
-keep class * extends androidx.glance.appwidget.action.ActionCallback { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver$* { *; }

# ----------------------------------------------------------------------------
# androidx.security:security-crypto (Tink)
#
# Tink registers key managers by reflection and carries a shaded protobuf.
# NOTE: this artifact is deprecated upstream, see docs/BUILD_CI_AUDIT.md.
# ----------------------------------------------------------------------------
-keep class com.google.crypto.tink.** { *; }
-keep class com.google.crypto.tink.shaded.protobuf.** { *; }
-dontwarn com.google.crypto.tink.**
-dontwarn com.google.errorprone.annotations.**

# ----------------------------------------------------------------------------
# DataStore
# ----------------------------------------------------------------------------
-keep class androidx.datastore.*.** { *; }

# ----------------------------------------------------------------------------
# Coroutines
# ----------------------------------------------------------------------------
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.debug.**

# ----------------------------------------------------------------------------
# PersianDate (com.github.samanzamani)
#
# Only jalali_to_gregorian / gregorian_to_jalali are used, both plain static-ish
# integer arithmetic, so NO keep rule is needed: R8 can shrink and rename the
# rest freely. Listed here so the next reader does not "fix" its absence.
# ----------------------------------------------------------------------------
-dontwarn com.github.samanzamani.**

# ----------------------------------------------------------------------------
# Platform plumbing
# ----------------------------------------------------------------------------
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Keep the annotation Compose's compiler leaves on composables; the runtime
# reads it for recomposition tooling in release builds too.
-keep,allowobfuscation @interface androidx.compose.runtime.Composable
