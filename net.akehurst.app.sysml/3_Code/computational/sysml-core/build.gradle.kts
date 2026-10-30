plugins {
    id("project-conventions")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":user-api"))
                api(project(":sysml-info"))
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.koog.agents)
                implementation(libs.nal.agl.processor)
                // SugiyamaLayout for the automatic layout of diagrams
                implementation(libs.nak.compose.layout.graph)
                implementation(libs.nak.kotlinx.collections)
                implementation(libs.nak.kotlinx.utils)
                implementation(libs.nak.kotlinx.logging.api)
            }
        }
    }
}
