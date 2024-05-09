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
    implementation(libs.pitest.plugin)
    implementation(libs.spotbugs.plugin)
    implementation(libs.spotless.plugin)
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}
