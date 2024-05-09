plugins {
    java
}

val itestSourceName = "itest"

sourceSets {
    create(itestSourceName) {
        java.srcDir("src/${itestSourceName}/java")
        resources.srcDir("src/${itestSourceName}/resources")
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
    testClassesDirs = sourceSets.named(itestSourceName).get().output.classesDirs
    classpath = sourceSets.named(itestSourceName).get().runtimeClasspath
    dependsOn("${itestSourceName}Classes")
}