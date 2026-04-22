plugins {
    kotlin("jvm") version "2.3.10"
    kotlin("plugin.serialization") version "2.1.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    application
}

group = "com.log.analyst"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // Koog AI framework
    implementation("ai.koog:koog-agents:0.8.0")

    // Serialization (JSON output + LLM response parsing)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")

    // Coroutines (Koog agent layer)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("io.kotest:kotest-assertions-core:5.9.1")
}

application {
    mainClass.set("MainKt")
}

tasks.test {
    useJUnitPlatform()
}

tasks.shadowJar {
    archiveBaseName.set("log-analyst-koog")
    archiveClassifier.set("")
    archiveVersion.set(version.toString())
}

kotlin {
    jvmToolchain(17)
}
