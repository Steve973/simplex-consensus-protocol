import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    java
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17)) // Update to the desired Java version
    }
    sourceCompatibility = JavaVersion.VERSION_17
    withJavadocJar()
    withSourcesJar()
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

val libs = the<LibrariesForLibs>()

dependencies {
    implementation(libs.slf4j.api)
    testImplementation(libs.slf4j.api)
    testImplementation(libs.slf4j.simple)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
    compileOnly(libs.spotbugs.annotations)
    annotationProcessor(libs.spotbugs.annotations)
    testCompileOnly(libs.spotbugs.annotations)
    testAnnotationProcessor(libs.spotbugs.annotations)
    testImplementation(libs.bundles.kotest)
}

tasks.test {
    useJUnitPlatform()
}
