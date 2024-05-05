plugins {
    id("com.diffplug.spotless")
}

spotless {
    java {
        eclipse().configFile("${rootDir}/project-resources/java-format/eclipse-java-style.xml")
    }
}

tasks.named("check") {
    dependsOn("spotlessApply", "spotlessCheck")
}