plugins {
    id("java")
    id("com.gradleup.shadow").version("9.3.2")
}

group = "dev.bauhd"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("com.github.javaparser:javaparser-core:3.28.0")
}

tasks {
    jar {
        manifest {
            attributes["Main-Class"] = "dev.bauhd.velocityupdater.VelocityUpdater"
        }
    }
}