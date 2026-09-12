import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// ---------------------------------------------------------------------------
// Version
//
// Single-sourced in gradle.properties so that CI can reconcile a pushed tag
// against it (see :app:printVersionName below and .github/workflows/release.yml).
// The scheme is unchanged from the shipped app: do not renumber it.
// ---------------------------------------------------------------------------
val versionMajor = providers.gradleProperty("bbarq.versionMajor").get().trim().toInt()
val versionPatch = providers.gradleProperty("bbarq.versionPatch").get().trim().toInt()
val appVersionCode = versionMajor * 10000 + versionPatch
val appVersionName = "$versionMajor.$versionPatch"

// ---------------------------------------------------------------------------
// Release signing material
//
// Either a local (git-ignored) keystore.properties file or environment
// variables in CI. Unchanged in this commit; hardened in the next one.
// ---------------------------------------------------------------------------
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

fun signingValue(propertyKey: String, envKey: String): String? =
    keystoreProperties.getProperty(propertyKey)?.takeIf { it.isNotBlank() }
        ?: providers.environmentVariable(envKey).orNull?.takeIf { it.isNotBlank() }

val releaseStorePath = signingValue("storeFile", "KEYSTORE_FILE")
val releaseStorePassword = signingValue("storePassword", "KEYSTORE_PASSWORD")
val releaseKeyAlias = signingValue("keyAlias", "KEY_ALIAS")
val releaseKeyPassword = signingValue("keyPassword", "KEY_PASSWORD")

val releaseKeystoreFile = releaseStorePath?.let { path ->
    val asIs = file(path)
    if (asIs.exists()) asIs else rootProject.file(path).takeIf { it.exists() }
}

val hasReleaseSigning = releaseKeystoreFile != null &&
    releaseStorePassword != null &&
    releaseKeyAlias != null &&
    releaseKeyPassword != null

// Room's exported schemas: review input and test fixture, tracked in git.
val roomSchemaDir = layout.projectDirectory.dir("schemas")

android {
    namespace = "com.aliJafari.bbarq"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.aliJafari.bbarq"
        minSdk = 24
        targetSdk = 36

        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = releaseKeystoreFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
    }

    buildTypes {
        debug {
            // So a debug build and a released build can sit side by side on the
            // same device instead of one uninstalling the other.
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isMinifyEnabled = false
        }

        release {
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                logger.warn(
                    "No release signing config found (keystore.properties or " +
                        "KEYSTORE_FILE/KEYSTORE_PASSWORD/KEY_ALIAS/KEY_PASSWORD). " +
                        "Falling back to the debug keystore: do not publish this build.",
                )
                signingConfigs.getByName("debug")
            }

            isMinifyEnabled = true
            // isShrinkResources is deliberately left off until a minified build
            // has been smoke-tested on a device (docs/BUILD_CI_AUDIT.md #2).
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        // A lint error fails the build. If master turns out to carry
        // pre-existing errors, generate a baseline once with
        //   ./gradlew :app:updateLintBaseline
        // and commit app/lint-baseline.xml, rather than turning this off.
        abortOnError = true
        warningsAsErrors = false
        checkDependencies = true
        checkReleaseBuilds = true
        // The app is intentionally bilingual with Persian defaults, so a missing
        // English string is a nice-to-have, not a build breaker.
        informational += "MissingTranslation"
        htmlReport = true
        xmlReport = true
        sarifReport = true
    }

    packaging {
        resources {
            // Duplicate licence/metadata files from overlapping jars. Kotlin
            // module metadata is deliberately NOT excluded: reflection over
            // top-level declarations needs it.
            excludes += setOf(
                "/META-INF/AL2.0",
                "/META-INF/LGPL2.1",
                "/META-INF/DEPENDENCIES",
                "/META-INF/LICENSE",
                "/META-INF/LICENSE.txt",
                "/META-INF/NOTICE",
                "/META-INF/NOTICE.txt",
                "/META-INF/*.version",
                "DebugProbesKt.bin",
            )
        }
    }

    bundle {
        // Language splitting is OFF on purpose: the app switches its own locale
        // at runtime (data/local/AppLanguage.kt), and Play's language splits
        // would only install the device language, silently breaking that.
        language {
            enableSplit = false
        }
        density {
            enableSplit = true
        }
        abi {
            enableSplit = true
        }
    }

    dependenciesInfo {
        // Keep it out of the APK (it is a Play-signing blob that hurts
        // reproducible-build comparisons) but keep it in the bundle, where Play
        // uses it for vulnerability reporting.
        includeInApk = false
        includeInBundle = true
    }

    sourceSets {
        // Room writes its exported schemas to app/schemas (see the ksp block).
        // Wired unconditionally: a missing schema must fail, not silently skip.
        getByName("androidTest") {
            assets.srcDir(roomSchemaDir)
        }
    }
}

kotlin {
    // Pin the JDK that compiles this project instead of inheriting whatever the
    // machine provides. settings.gradle.kts applies the Foojay resolver so this
    // is provisioned when absent.
    jvmToolchain(17)

    compilerOptions {
        // Replaces the deprecated android.kotlinOptions block. Kept at 11 to
        // match compileOptions; moving both to 17 changes desugaring behaviour
        // and therefore app behaviour, which is out of scope here.
        jvmTarget = JvmTarget.JVM_11
    }
}

ksp {
    // Committed schemas are what make Room migrations reviewable and testable.
    arg("room.schemaLocation", roomSchemaDir.asFile.path)
}

// ---------------------------------------------------------------------------
// Verification and helper tasks
// ---------------------------------------------------------------------------

// Used by .github/workflows/release.yml to reconcile the pushed tag with the
// version in gradle.properties. Keep the output to exactly one line.
tasks.register("printVersionName") {
    group = "help"
    description = "Prints the versionName that a release build would carry."
    val value = appVersionName
    doLast { println(value) }
}

tasks.register("printVersionCode") {
    group = "help"
    description = "Prints the versionCode that a release build would carry."
    val value = appVersionCode
    doLast { println(value) }
}

// Room migration tests read the exported schemas as instrumentation assets. An
// empty schemas/ directory means the tests would pass by doing nothing, so fail
// loudly instead. Wired into connectedCheck rather than check: generating the
// schema requires running KSP, and a fresh clone has not done that yet.
val roomSchemaJson = fileTree(roomSchemaDir) { include("**/*.json") }
val verifyRoomSchemas = tasks.register("verifyRoomSchemas") {
    group = "verification"
    description = "Fails when Room's exported schemas are missing from version control."
    val schemas = roomSchemaJson
    outputs.upToDateWhen { false }
    doLast {
        if (schemas.isEmpty) {
            throw GradleException(
                """
                No Room schema found under app/schemas/.

                Room exports one JSON file per database version and the migration
                tests read them as instrumentation assets. Generate and commit it:

                    ./gradlew :app:kspDebugKotlin
                    git add app/schemas

                See docs/BUILDING.md.
                """.trimIndent(),
            )
        }
    }
}

tasks.matching { it.name.startsWith("connected") }.configureEach {
    dependsOn(verifyRoomSchemas)
}

// docs/CORE_ARCHITECTURE.md: "domain/ imports no android.* and no androidx.*".
// That is the one layering rule a build can check cheaply, so it is enforced.
val domainSources = fileTree(layout.projectDirectory.dir("src/main/java/com/aliJafari/bbarq/domain")) {
    include("**/*.kt")
}
val verifyDomainPurity = tasks.register("verifyDomainPurity") {
    group = "verification"
    description = "Fails when the domain layer imports android.* or androidx.*."
    val sources = domainSources
    val report = layout.buildDirectory.file("reports/layering/domain-purity.txt")
    inputs.files(sources).withPropertyName("domainSources")
    outputs.file(report)
    doLast {
        val violations = sources.files.sortedBy { it.path }.flatMap { source ->
            source.readLines().mapIndexedNotNull { index, line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("import android.") || trimmed.startsWith("import androidx.")) {
                    "${source.name}:${index + 1}: $trimmed"
                } else {
                    null
                }
            }
        }
        val reportFile = report.get().asFile
        reportFile.parentFile.mkdirs()
        reportFile.writeText(
            if (violations.isEmpty()) "domain layer is Android-free\n" else violations.joinToString("\n", postfix = "\n"),
        )
        if (violations.isNotEmpty()) {
            throw GradleException(
                "The domain layer must stay Android-free (docs/CORE_ARCHITECTURE.md):\n" +
                    violations.joinToString("\n") { "  $it" },
            )
        }
    }
}

// The other rule from CORE_ARCHITECTURE.md ("ui/ never touches a repository,
// DAO or DTO") is reported, not enforced: master is mid-rewrite and parts of
// the UI still reach into data/ directly. Flip this to a failure once
// refactor/core-rewrite lands.
val uiSources = fileTree(layout.projectDirectory.dir("src/main/java/com/aliJafari/bbarq/ui")) {
    include("**/*.kt")
}
val reportUiLayerLeaks = tasks.register("reportUiLayerLeaks") {
    group = "verification"
    description = "Reports ui/ files that import data/ directly."
    val sources = uiSources
    val report = layout.buildDirectory.file("reports/layering/ui-data-imports.txt")
    inputs.files(sources).withPropertyName("uiSources")
    outputs.file(report)
    doLast {
        val leaks = sources.files.sortedBy { it.path }.flatMap { source ->
            source.readLines().mapIndexedNotNull { index, line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("import com.aliJafari.bbarq.data.")) {
                    "${source.name}:${index + 1}: $trimmed"
                } else {
                    null
                }
            }
        }
        val reportFile = report.get().asFile
        reportFile.parentFile.mkdirs()
        reportFile.writeText(
            if (leaks.isEmpty()) "no ui -> data imports\n" else leaks.joinToString("\n", postfix = "\n"),
        )
        if (leaks.isNotEmpty()) {
            logger.warn(
                "ui/ imports data/ directly in ${leaks.size} place(s); see " +
                    "build/reports/layering/ui-data-imports.txt",
            )
        }
    }
}

tasks.named("check") {
    dependsOn(verifyDomainPurity, reportUiLayerLeaks)
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    // MainActivity extends AppCompatActivity and Utils uses AppCompatDelegate,
    // so this belongs here rather than arriving transitively through material.
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)

    // Dependency injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Background work
    implementation(libs.androidx.work.runtime.ktx)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Persistence
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.security.crypto)

    // Networking & serialization
    implementation(libs.okhttp)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.kotlinx.serialization.json)

    // Jalali calendar conversion
    implementation(libs.persian.date)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.work.testing)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Home screen widget.
    implementation(libs.androidx.glance.appwidget)

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation(libs.androidx.material3)
    // Android Studio Preview support
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)
}
