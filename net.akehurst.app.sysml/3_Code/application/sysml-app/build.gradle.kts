plugins {
    id("project-conventions")
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlin.compose)
}

// project-conventions makes every JS/Wasm target a library, and this module adds an executable.
// Both write to the same folder, so turn off the library distribution: the app is not a library.
tasks.matching { it.name.endsWith("LibraryDistribution") }.configureEach { enabled = false }

kotlin {
    wasmJs {
        binaries.executable()
    }
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":sysml-info"))
                implementation(project(":user-api"))
                implementation(project(":sysml-core"))
                implementation(project(":sysml-gui"))
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        jvmMain {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "net.akehurst.app.sysml.application.MainKt"
    }
}
