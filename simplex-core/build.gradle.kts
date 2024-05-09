plugins {
    id("simplex.java-conventions")
    id("simplex.java-itest-conventions")
    id("simplex.javadoc-conventions")
    id("simplex.code-quality-conventions")
    id("simplex.spotless-conventions")
    id("simplex.kotlin-conventions")
}

dependencies {
    implementation(project(":simplex-api"))

    implementation(libs.bouncycastle.provider.fips)
    implementation(libs.google.guava)
    implementation(libs.jackson.databind)
    implementation(libs.slf4j.api)
}

repositories {
    mavenCentral()
}
