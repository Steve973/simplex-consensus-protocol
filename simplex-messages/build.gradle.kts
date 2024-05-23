plugins {
    id("simplex.scala-conventions")
}

repositories {
    mavenCentral()
    maven("https://repo.akka.io/maven")
}

dependencies {
    implementation(platform(libs.akka.dependencies.bom))
    implementation(libs.akka.actor.typed)
    implementation(libs.akka.stream)
    implementation(libs.akka.slf4j)
    implementation(libs.logback.classic)
    implementation(libs.bouncycastle.provider.fips)
    implementation(libs.jackson.databind)
}
