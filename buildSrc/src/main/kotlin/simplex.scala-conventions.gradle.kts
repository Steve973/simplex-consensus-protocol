import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.kotlin.dsl.the

plugins {
    scala
}

val libs = the<LibrariesForLibs>()

dependencies {
    implementation(libs.scala.library)
}