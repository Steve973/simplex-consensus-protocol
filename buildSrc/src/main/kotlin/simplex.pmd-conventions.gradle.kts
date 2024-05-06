plugins {
    pmd
}

pmd {
    toolVersion = "7.1.0"
    rulesMinimumPriority = 5
    threads = 4
    ruleSets = listOf(
        "${rootProject.projectDir}/project-resources/pmd/pmd-ruleset.xml"
    )
}
