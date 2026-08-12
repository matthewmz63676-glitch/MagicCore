plugins {
    java
    jacoco
    id("com.gradleup.shadow") version "8.3.8"
}

group = "com.magicstudios"
version = providers.gradleProperty("magiccoreVersion").get()

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://jitpack.io")
    maven("https://maven.enginehub.org/repo/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:${providers.gradleProperty("paperApiVersion").get()}")
    compileOnly("net.luckperms:api:5.5")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1") {
        isTransitive = false
    }
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.14")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.19.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.19.2")
    implementation("org.yaml:snakeyaml:2.4")
    implementation("org.xerial:sqlite-jdbc:3.50.3.0")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.5.5")
    implementation("org.mongodb:mongodb-driver-sync:5.5.1")

    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core:3.27.4")
    testImplementation("net.kyori:adventure-text-minimessage:4.24.0")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withSourcesJar()
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.test {
    useJUnitPlatform()
    maxParallelForks = 1
    testLogging {
        events("failed", "skipped")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestReport)
}

tasks.shadowJar {
    archiveClassifier.set("")
    mergeServiceFiles()
    relocate("com.fasterxml.jackson", "com.magicstudios.magiccore.lib.jackson")
    relocate("org.yaml.snakeyaml", "com.magicstudios.magiccore.lib.snakeyaml")
}

tasks.jar {
    archiveClassifier.set("plain")
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}

tasks.register<Zip>("sourceBundle") {
    group = "distribution"
    archiveFileName.set("MagicCore-1.0.0-rc1-source.zip")
    destinationDirectory.set(layout.projectDirectory.dir(".."))
    from(projectDir) {
        include("src/**", "docs/**", "discord-bot/**", "gradle/**", "build.gradle.kts", "settings.gradle.kts",
            "gradle.properties", "gradlew", "gradlew.bat", "README.md")
        exclude("**/.gradle/**", "**/build/**")
    }
}

tasks.register<Zip>("releaseBundle") {
    group = "distribution"
    dependsOn(tasks.shadowJar, project(":discord-bot").tasks.named("shadowJar"), tasks.named("sourceBundle"))
    archiveFileName.set("MagicCore-1.0.0-rc1-release.zip")
    destinationDirectory.set(layout.projectDirectory.dir(".."))
    from(tasks.shadowJar) { into("plugin") }
    from(project(":discord-bot").tasks.named("shadowJar")) { into("discord-bot") }
    from("README.md")
    from("docs") { into("docs") }
}
