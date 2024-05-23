pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "simplex-consensus-protocol"

include("simplex-core")
include("simplex-messages")
include("simplex-util")
include("simplex-model")
