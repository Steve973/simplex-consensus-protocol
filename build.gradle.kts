plugins {
    id("org.sonarqube")
}
group = "org.storck"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

sonar {
    properties {
        property("sonar.projectKey", "Steve973_simplex-consensus-protocol")
        property("sonar.organization", "steve973")
        property("sonar.host.url", "https://sonarcloud.io")
    }
}
