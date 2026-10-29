plugins {
    id("project-conventions")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(libs.nao.omg.sysml.api)
                api(libs.nao.omg.sysml.ram)
                api(libs.nak.kotlinx.collections)
                api(libs.nak.kotlinx.utils)
            }
        }
    }
}
