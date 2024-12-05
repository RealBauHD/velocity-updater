plugins {
    id("java")
    id("com.github.johnrengelman.shadow").version("8.1.1")
}

group = "dev.bauhd"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.code.gson:gson:2.11.0")
}

tasks {
    jar {
        manifest {
            attributes["Main-Class"] = "dev.bauhd.velocityupdater.VelocityUpdater"
        }
    }
}