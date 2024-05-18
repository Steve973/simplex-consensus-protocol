import info.solidsoft.gradle.pitest.PitestPluginExtension

plugins {
    jacoco
    id("info.solidsoft.pitest")
}

tasks.named("test") {
    finalizedBy("jacocoTestReport")
}

tasks.withType<JacocoReport> {
    reports {
        xml.required = true
        html.required = true
        html.outputLocation = layout.buildDirectory.dir("reports/jacoco")
    }
    dependsOn("test")
}

configure<PitestPluginExtension> {
    junit5PluginVersion.set("1.2.0")
    threads.set(4)
    targetClasses.set(listOf("org.storck.simplex.*"))
    outputFormats.set(listOf("XML", "HTML"))
    mutationThreshold.set(0)
    exportLineCoverage.set(false)
    verbose.set(false)
}

tasks.named("test") {
    finalizedBy("pitest")
}
