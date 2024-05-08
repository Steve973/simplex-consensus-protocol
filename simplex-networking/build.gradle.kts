plugins {
    id("simplex.java-conventions")
    id("simplex.java-itest-conventions")
    id("simplex.javadoc-conventions")
    id("simplex.code-quality-conventions")
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
    implementation(libs.netty.all)
    implementation(libs.slf4j.api)
    implementation(libs.spotbugs.annotations)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testImplementation(libs.bundles.kotest)
}

tasks.test {
    useJUnitPlatform()
}

repositories {
    mavenCentral()
    maven("https://dl.cloudsmith.io/public/libp2p/jvm-libp2p/maven/")
    maven("https://jitpack.io")
    maven("https://artifacts.consensys.net/public/maven/maven/")
}