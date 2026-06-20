import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.testing.Test

plugins {
    application
}

// Automation scripts use an isolated, user-owned directory so a previous
// elevated build cannot leave Gradle outputs that block normal builds.
providers.gradleProperty("xenoverseBuildDir").orNull?.let {
    layout.buildDirectory.set(file(it))
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")

    implementation("org.joml:joml:1.10.8")
    implementation("org.lwjgl:lwjgl:3.3.4")
    implementation("org.lwjgl:lwjgl-glfw:3.3.4")
    implementation("org.lwjgl:lwjgl-opengl:3.3.4")
    implementation("org.lwjgl:lwjgl-stb:3.3.4")

    runtimeOnly("org.lwjgl:lwjgl:3.3.4:natives-windows")
    runtimeOnly("org.lwjgl:lwjgl:3.3.4:natives-linux")
    runtimeOnly("org.lwjgl:lwjgl-glfw:3.3.4:natives-windows")
    runtimeOnly("org.lwjgl:lwjgl-glfw:3.3.4:natives-linux")
    runtimeOnly("org.lwjgl:lwjgl-opengl:3.3.4:natives-windows")
    runtimeOnly("org.lwjgl:lwjgl-opengl:3.3.4:natives-linux")
    runtimeOnly("org.lwjgl:lwjgl-stb:3.3.4:natives-windows")
    runtimeOnly("org.lwjgl:lwjgl-stb:3.3.4:natives-linux")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass.set("xenoverse.App")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

tasks.register<Jar>("portableJar") {
    group = "build"
    description = "Builds a self-contained jar with dependencies and LWJGL natives for Windows and Linux."

    archiveBaseName.set("xenoverse")
    archiveClassifier.set("portable")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true

    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }

    dependsOn("classes")
    from(sourceSets.main.get().output)
    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.endsWith(".jar") }
            .map { zipTree(it) }
    })

    exclude(
        "META-INF/*.SF",
        "META-INF/*.DSA",
        "META-INF/*.RSA",
        "META-INF/INDEX.LIST"
    )
}
