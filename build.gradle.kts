import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        mavenCentral()
    }
}

plugins {
    kotlin("jvm") version "1.5.20" apply true
}

group = "org.cuongnv"
version = "1.0"

repositories {
    flatDir {
        dirs("${rootProject.projectDir}/libs")
    }
    mavenCentral()
}

dependencies {
    implementation(project(":swingui"))

    implementation("com.google.code.gson:gson:2.8.6")
    implementation("io.reactivex.rxjava3:rxkotlin:3.0.1")

    implementation("com.google.http-client:google-http-client:1.39.2-sp.1")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnit()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}

val copyLibraries by tasks.registering {
    doLast {
        copy {
            from(configurations.compileClasspath)
            into("${project.projectDir.absolutePath}/build/libs/libs/")
        }
    }
}

val compileToExecutable = tasks.register<Jar>("compileToExecutable") {
    dependsOn(copyLibraries)

    manifest {
        attributes("Main-Class" to "org.cuongnv.subfinder.ApplicationKt")
        attributes("Version" to project.version)
        attributes("Class-Path" to
                configurations.compileClasspath.get().joinToString(separator = " ") { "libs/${it.name}" })
    }

    archiveFileName.set("subfinder.jar")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    isPreserveFileTimestamps = false

    with(tasks.jar.get() as CopySpec)
}