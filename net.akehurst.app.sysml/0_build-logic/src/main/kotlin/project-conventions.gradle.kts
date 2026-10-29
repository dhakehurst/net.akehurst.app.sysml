@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.ZoneId

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.dokka)
    alias(libs.plugins.buildconfig)
    signing
    alias(libs.plugins.vanniktech.maven.publish) apply false
}
val kotlin_languageVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2
val kotlin_apiVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2
val jvmTargetVersion = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17

repositories {
    mavenLocal {
        content{
            includeGroupByRegex("net\\.akehurst.+")
        }
    }
    mavenCentral()
    google()
    gradlePluginPortal()
}

group = rootProject.name
version = libs.versions.project.get()
project.layout.buildDirectory = File(rootProject.projectDir, ".gradle-build/${project.name}")

buildConfig {
    val now = Instant.now()
    fun fBbuildStamp(): String = DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneId.of("UTC")).format(now)
    fun fBuildDate(): String = DateTimeFormatter.ofPattern("yyyy-MMM-dd").withZone(ZoneId.of("UTC")).format(now)
    fun fBuildTime(): String = DateTimeFormatter.ofPattern("HH:mm:ss z").withZone(ZoneId.of("UTC")).format(now)

    buildConfigField("String", "version", "\"${project.version}\"")
    buildConfigField("String", "buildStamp", "\"${fBbuildStamp()}\"")
    buildConfigField("String", "buildDate", "\"${fBuildDate()}\"")
    buildConfigField("String", "buildTime", "\"${fBuildTime()}\"")
}

kotlin {
    applyDefaultHierarchyTemplate()
    compilerOptions {
        optIn.add("kotlin.time.ExperimentalTime")
    }
    jvm {
        val main by compilations.getting {
            compileTaskProvider.configure {
                compilerOptions {
                    languageVersion.set(kotlin_languageVersion)
                    apiVersion.set(kotlin_apiVersion)
                    jvmTarget.set(jvmTargetVersion)
                }
            }
        }
        val test by compilations.getting {
            compileTaskProvider.configure {
                compilerOptions {
                    languageVersion.set(kotlin_languageVersion)
                    apiVersion.set(kotlin_apiVersion)
                    jvmTarget.set(jvmTargetVersion)
                }
            }
        }
    }
    js {
        binaries.library()
        nodejs()
        browser()
        generateTypeScriptDefinitions()
        compilerOptions {
            target.set("es2015")
            freeCompilerArgs = listOf("-Xes-long-as-bigint")
        }
    }
    wasmJs {
        binaries.library()
        browser()
    }
    //macosArm64()

    sourceSets {
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(kotlin("test"))
            implementation(kotlin("test-annotations-common"))
        }
        jvmTest.dependencies {
            implementation("org.junit.jupiter:junit-jupiter")
            runtimeOnly("org.junit.platform:junit-platform-launcher")
        }
    }
}



tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
    filter {
        isFailOnNoMatchingTests = false
    }
    testLogging {
        showExceptions = true
        showStandardStreams = true
        events = setOf(
            org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED,
            org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
        )
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

signing {
    setRequired( {  gradle.taskGraph.hasTask("uploadArchives") })
    useGpgCmd()
    val publishing = project.properties["publishing"] as PublishingExtension
    sign(publishing.publications)
}
val signTasks = tasks.matching { it.name.matches(Regex("sign(.)+")) }.toTypedArray()
tasks.forEach {
    when {
        it.name.matches(Regex("publish(.)+")) -> {
            it.mustRunAfter(*signTasks)
        }
    }
}

mavenPublishing {
    signAllPublications()
    publishToMavenCentral(automaticRelease = false)

    coordinates(group as String, project.name, version as String)
    pom {
        name.set(project.name)
        description.set("A SysML Application")

        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                name.set("Dr. David H. Akehurst")
                email.set("dr.david.h@akehurst.net")
            }
        }
        scm {
            url.set("https://github.com/dhakehurst/net.akehurst.app.sysml")
        }
    }
}

publishing {
    repositories {
        maven {
            name = "Other"
            url = uri(providers.gradleProperty("publishTo").orElse("other"))
            credentials {
                username = providers.environmentVariable("NEXUS_USER")
                    .orElse(providers.gradleProperty("NEXUS_USER"))
                    .orNull
                password = providers.environmentVariable("NEXUS_PASS")
                    .orElse(providers.gradleProperty("NEXUS_PASS"))
                    .orNull
            }
        }
    }
}