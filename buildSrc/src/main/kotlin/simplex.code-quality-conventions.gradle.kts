import com.github.spotbugs.snom.Confidence
import com.github.spotbugs.snom.Effort
import com.github.spotbugs.snom.SpotBugsTask

plugins {
    checkstyle
    pmd
    id("com.github.spotbugs")
}

checkstyle {
    toolVersion = "10.15.0"
    configFile = file("${rootProject.projectDir}/project-resources/checkstyle/checkstyle.xml")
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
