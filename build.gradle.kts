plugins {
    kotlin("jvm") version "2.2.0"
    application
}

group = "io.github.oianmol"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.anthropic:anthropic-java:3.3.0")
    implementation("com.anthropic:anthropic-java-bedrock:3.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.19.4")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.19.4")

    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("io.github.oianmol.harness.cli.MainKt")
}

tasks.test {
    useJUnitPlatform()
}

// Runs the CLI from the repo root so relative paths (knowledge-base/, inbound/) resolve.
tasks.named<JavaExec>("run") {
    workingDir = rootDir
}
