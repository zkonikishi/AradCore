plugins { java }

group = "cn.owonya.aradcore"
version = "0.1.0-SNAPSHOT"

java { toolchain.languageVersion.set(JavaLanguageVersion.of(21)) }

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

val serverPlugins = file("../Beta/server/plugins")

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.5-R0.1-SNAPSHOT")
    compileOnly(files(serverPlugins.resolve("MMOItems-6.10.1-20260531.191614-59.jar")))
    compileOnly(files(serverPlugins.resolve("MythicLib-dist-1.7.1-20260704.223033-104.jar")))

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.processResources {
    filesMatching("plugin.yml") { expand("version" to project.version) }
}

tasks.test { useJUnitPlatform() }

tasks.jar { archiveFileName.set("AradCore-${project.version}.jar") }
