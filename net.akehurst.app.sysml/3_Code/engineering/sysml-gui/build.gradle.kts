plugins {
    id("project-conventions")
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":user-api"))
                api(libs.compose.runtime)
                api(libs.compose.foundation)
                api(libs.compose.ui)
                api(libs.compose.material3)
                api(libs.material.icons.core)
                api(libs.nak.compose.components)
                api(libs.nak.compose.code.editor)
                api(libs.nak.compose.layout.multipane)
                api(libs.nak.compose.layout.graph)
            }
        }
    }
}
