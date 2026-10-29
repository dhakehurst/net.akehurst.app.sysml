println("===============================================")
println("Gradle: ${GradleVersion.current()}")
println("JVM: ${org.gradle.internal.jvm.Jvm.current()} '${org.gradle.internal.jvm.Jvm.current().javaHome}'")
println("===============================================")

pluginManagement {
    repositories {
        mavenLocal {
            content{
                includeGroupByRegex("net\\.akehurst.+")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
    includeBuild("./0_build-logic")
}

rootProject.name = file(".").name

fileTree(".") {
    exclude("build.gradle.kts")
    exclude("_buildSrc")
    exclude("0_build-logic")
    include("**/build.gradle.kts")
}.forEach {
    val prjName = it.parentFile.name
    val prjPath = relativePath(it.parent)
    println("including $prjName at $prjPath")
    include(prjName)
    project(":$prjName").projectDir = File(prjPath)
}