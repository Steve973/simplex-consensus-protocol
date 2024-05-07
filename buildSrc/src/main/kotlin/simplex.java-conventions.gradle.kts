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

tasks.test {
    useJUnitPlatform()
}
