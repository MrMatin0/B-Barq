pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    // Provisions the JDK that `kotlin { jvmToolchain(17) }` asks for instead of
    // failing on a contributor whose only installed JDK is 21 (which is what
    // recent Android Studio releases bundle). 0.10.0 is the last release line
    // verified against Gradle 8.x, which is what the wrapper pins.
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()

        // JitPack will resolve *any* group it is asked for, which makes an
        // unfiltered entry a dependency-confusion vector. It serves exactly one
        // coordinate here, so bind it to that group and nothing else.
        exclusiveContent {
            forRepository {
                maven {
                    name = "JitPack"
                    setUrl("https://jitpack.io")
                }
            }
            filter {
                includeGroup("com.github.samanzamani")
            }
        }

        // Myket store artifacts. NOTE: nothing in gradle/libs.versions.toml
        // currently resolves from here (docs/BUILD_CI_AUDIT.md #23). Kept in
        // case a store SDK is planned; delete it if not.
        maven {
            name = "Myket"
            setUrl("https://maven.myket.ir")
            content {
                includeGroupByRegex("ir\\.myket.*")
            }
        }
    }
}

rootProject.name = "BBarq"
include(":app")
