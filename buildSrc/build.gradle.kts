plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    maven("https://plugins.gradle.org/m2/")
}

dependencies {
    implementation(libs.kotlin.plugin)
    implementation(libs.dependency.license.report.plugin)
    implementation(libs.spotbugs.plugin)
    implementation(libs.spotless.plugin)
}
