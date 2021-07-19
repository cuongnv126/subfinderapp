plugins {
    java
    kotlin("jvm")
}

group = "org.cuongnv"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    api("net.java.dev.jna:jna-platform-jpms:5.8.0")
}