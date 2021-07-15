import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.20"
}

group = "org.cuongnv"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":swingui"))

    implementation("com.google.code.gson:gson:2.8.6")
    implementation("com.squareup.okhttp3:okhttp:3.14.7")
    implementation("io.reactivex.rxjava3:rxkotlin:3.0.1")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnit()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}