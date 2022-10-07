import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.20-RC"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.7.20-RC"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    application

}

group = "com.virginiaprivacy"
version = "1.0.3"

repositories {
    mavenCentral()
}

application {
    mainClass.set("com.virginiaprivacy.bookeobot.ScraperKt")
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.slf4j:slf4j-simple:2.0.1")
    implementation("com.vonage:client:7.1.0")
    implementation("com.carrotsearch.thirdparty:simple-xml-safe:2.7.1")
    implementation("io.javalin:javalin:4.6.4")
    implementation("io.ktor:ktor-client-core:2.1.1")
    implementation("io.ktor:ktor-client-cio:2.1.1")
    implementation("io.ktor:ktor-client-content-negotiation:2.1.1")
    implementation("io.ktor:ktor-serialization-kotlinx-xml:2.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.0")


}

tasks.shadowJar {
    this.archiveFileName.set("bookeobot.jar")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}
