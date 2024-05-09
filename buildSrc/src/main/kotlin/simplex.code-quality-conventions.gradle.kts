import com.github.spotbugs.snom.Confidence
import com.github.spotbugs.snom.Effort
import com.github.spotbugs.snom.SpotBugsTask
import gradle.kotlin.dsl.accessors._984b42742b1efd04b12d9b66321c4910.pitest

plugins {
    checkstyle
    pmd
    id("com.github.spotbugs")
    id("info.solidsoft.pitest")
}

checkstyle {
    toolVersion = "10.15.0"
    configFile = file("${rootProject.projectDir}/project-resources/checkstyle/checkstyle.xml")
}

pitest {
    junit5PluginVersion.set("1.2.0")
    threads.set(4)
    targetClasses.set(listOf("org.storck.simplex.*"))
    outputFormats.set(listOf("XML", "HTML"))
    mutationThreshold.set(0)
    exportLineCoverage.set(false)
    verbose.set(false)
}

pmd {
    toolVersion = "7.1.0"
    rulesMinimumPriority = 5
    threads = 4
    ruleSets = listOf(
        "${rootProject.projectDir}/project-resources/pmd/pmd-ruleset.xml"
    )
}

spotbugs {
    toolVersion = "4.8.4"
    effort = Effort.MAX
    reportLevel = Confidence.DEFAULT
    excludeFilter = rootProject.file("project-resources/spotbugs/spotbugs-exclude.xml")
}

tasks.named<SpotBugsTask>("spotbugsMain") {
    reports.create("html") {
        required = true
        outputLocation = layout.buildDirectory.file("reports/spotbugs/spotbugs.html")
        setStylesheet("fancy-hist.xsl")
    }
}

tasks.named<SpotBugsTask>("spotbugsTest") {
    reports.create("html") {
        required = true
        outputLocation = layout.buildDirectory.file("reports/spotbugs/spotbugs-test.html")
        setStylesheet("fancy-hist.xsl")
    }
}

tasks.named("test") {
    finalizedBy("pitest")
}