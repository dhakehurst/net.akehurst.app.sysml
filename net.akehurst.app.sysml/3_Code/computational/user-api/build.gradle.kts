plugins {
    id("project-conventions")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":sysml-info"))
            }
        }
    }
}
