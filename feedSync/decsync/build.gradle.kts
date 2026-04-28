plugins {
    alias(libs.plugins.feedflow.library)
}

kotlin {
    androidLibrary {
        namespace = "com.prof18.feedflow.feedsync.decsync"
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":core"))
                implementation(project(":database"))
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.touchlab.kermit)
                implementation(libs.multiplatform.settings)
                implementation(project.dependencies.platform(libs.koin.bom))
                implementation(libs.koin.core)
            }
        }

        androidMain {
            dependencies {
                // libdecsync has no JVM target — Android only
                implementation(libs.libdecsync)
                implementation(libs.kotlinx.serialization.json)
            }
        }

        jvmMain {
            dependencies {
                // libdecsync has no JVM artifact; bundle the extracted Android runtime JAR
                // which is pure JVM bytecode for the File-based constructor path
                implementation(files("libs/libdecsync-jvm.jar"))
                implementation(libs.kotlinx.serialization.json)
            }
        }
    }
}
