plugins {
    java
}

sourceSets {
    create("itest") {
        java.srcDir("src/itest/java")
        resources.srcDir("src/itest/resources")
        compileClasspath += sourceSets.main.get().output + configurations.testRuntimeClasspath.get()
        runtimeClasspath += output + compileClasspath
    }
}

tasks.named<Copy>("processItestResources").configure {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

val itest by tasks.registering(Test::class) {
    group = "verification"
    description = "Runs the integration tests."
    testClassesDirs = sourceSets.named("itest").get().output.classesDirs
    classpath = sourceSets.named("itest").get().runtimeClasspath
    dependsOn("itestClasses")
}