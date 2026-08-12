plugins {
    application
    id("com.gradleup.shadow") version "8.3.8"
}

repositories { mavenCentral() }

dependencies {
    implementation("net.dv8tion:JDA:6.4.2") { exclude(module = "opus-java") }
    implementation("com.fasterxml.jackson.core:jackson-databind:2.19.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.19.2")
    runtimeOnly("ch.qos.logback:logback-classic:1.5.18")
}

java { toolchain.languageVersion.set(JavaLanguageVersion.of(21)) }
application { mainClass.set("com.magicstudios.magiccore.discordbot.MagicCoreDiscordBot") }
tasks.shadowJar { archiveClassifier.set(""); mergeServiceFiles() }
tasks.assemble { dependsOn(tasks.shadowJar) }
