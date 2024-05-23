plugins {
    id("simplex.java-conventions")
    id("simplex.java-itest-conventions")
    id("simplex.javadoc-conventions")
    id("simplex.code-quality-conventions")
    id("simplex.spotless-conventions")
    id("simplex.software-license-conventions")
    id("simplex.kotlin-conventions")
}

repositories {
    mavenCentral()
    maven("https://repo.akka.io/maven")
}
