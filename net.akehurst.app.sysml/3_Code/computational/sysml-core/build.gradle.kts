plugins {
    id("project-conventions")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":user-api"))
                api(project(":sysml-info"))
                implementation(libs.nal.agl.processor)
                implementation(libs.nak.kotlinx.collections)
                implementation(libs.nak.kotlinx.utils)
                implementation(libs.nak.kotlinx.logging.api)
            }
        }
    }
}
