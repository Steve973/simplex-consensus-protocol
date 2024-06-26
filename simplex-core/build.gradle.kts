plugins {
    id("simplex.java-module-common-conventions")
}

dependencies {
    implementation(project(":simplex-api"))

    implementation(libs.bouncycastle.provider.fips)
    implementation(libs.jackson.databind)
}
