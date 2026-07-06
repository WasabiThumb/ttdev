import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
    alias(libs.plugins.gradle.publish)
    alias(libs.plugins.indra) apply false
    alias(libs.plugins.indra.publishing)
    alias(libs.plugins.indra.licenser)
}

group = "io.github.wasabithumb"
version = "0.1.2"
description = "Provides Minecraft modding APIs for modern versions with minimal drama"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.gson)
    implementation(libs.maven.core)
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        jvmTarget = JvmTarget.JVM_25
        freeCompilerArgs = listOf(
            "-jvm-default=enable",
            "-Xjdk-release=25"
        )
    }
}

indra {
    github("WasabiThumb", "ttdev")
    apache2License()
    configurePublications {
        pom {
            developers {
                developer {
                    id = "WasabiThumb"
                    name = "Xavier Pedraza"
                    timezone = "America/New_York"
                }
            }
        }
    }
}

indraSpotlessLicenser {
    licenseHeaderFile(rootProject.file("license_header.txt"))
}

indraPluginPublishing {
    website("https://github.com/WasabiThumb/ttdev")
    plugin(
        "ttdev",
        "io.github.wasabithumb.ttdev.TTDevPlugin",
        "TTDev",
        "${project.description}",
        listOf(
            "minecraft",
            "mod",
            "client",
            "server",
            "fabric",
            "forge",
            "neoforge"
        )
    )
}
