plugins {
    java
}

tasks.withType<Javadoc> {
    source = sourceSets.main.get().allJava
}