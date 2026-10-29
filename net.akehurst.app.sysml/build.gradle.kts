plugins {
    // this is necessary to avoid the plugins to be loaded in each subproject
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.buildconfig) apply false
    alias(libs.plugins.vanniktech.maven.publish) apply false
}

group = rootProject.name
project.layout.buildDirectory = File(rootProject.projectDir, ".gradle-build/${project.name}")