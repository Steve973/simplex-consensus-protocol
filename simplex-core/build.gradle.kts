plugins {
    id("simplex.java-conventions")
    id("simplex.java-itest-conventions")
    id("simplex.spotless-conventions")
    id("simplex.kotlin-conventions")
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

dependencies {
    implementation(project(":simplex-api"))

    implementation(libs.bouncycastle.provider.fips)
    implementation(libs.google.guava)
    implementation(libs.jackson.databind)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testImplementation(libs.bundles.kotest)
}

tasks.test {
    useJUnitPlatform()
}

repositories {
    mavenCentral()
}
