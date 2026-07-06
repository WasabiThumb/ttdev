# TTDev

[![Build](https://img.shields.io/github/actions/workflow/status/WasabiThumb/ttdev/build.yml?branch=master)](https://github.com/WasabiThumb/ttdev/actions)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue)](LICENSE.txt)
[![Latest Release](https://img.shields.io/gradle-plugin-portal/v/io.github.wasabithumb.ttdev)](https://plugins.gradle.org/plugin/io.github.wasabithumb.ttdev)

TTDev ([Tiny Takeover](https://minecraft.wiki/w/Tiny_Takeover) Development) is an
experimental Gradle plugin that mavenizes the Minecraft client, server and
modding APIs for use as a regular dependency. The goal is to work much faster and
more reliably than existing plugins by minimizing labor and caching eagerly.

> [!IMPORTANT]
> Since this plugin intentionally omits remapping capabilities,
> it will only work properly for modern versions with official mappings
> (MC 26.1+).

## Example
```kotlin
plugins {
    id("java")
    id("io.github.wasabithumb.ttdev") version "0.1.2"
}

repositories {
    mavenCentral()
    ttdev.repository()
}

dependencies {
    // Minecraft
    compileOnly(ttdev.minecraftClient("26.2"))
    compileOnly(ttdev.minecraftServer("26.2"))

    // Forge
    compileOnly(ttdev.forge("26.2-65.0.3"))

    // NeoForge
    compileOnly(ttdev.neoForge("26.2.0.7-beta"))

    // Fabric
    compileOnly(ttdev.fabricLoader("0.19.3"))
    compileOnly(ttdev.fabricApi("0.153.0+26.2"))
}
```

> [!TIP]
> TTDev also supports Providers, so you can declare
> ``ttdev.minecraftClient(libs.versions.minecraft)`` or
> similar.

## FAQ

### I'm getting a ``No source could resolve transient library`` error!
This is normal and should be expected. What this means is that
a target, such as the Fabric API, depends on a library that is
not contained within its source (the Fabric maven repo). This is
often the case. It's important that you keep ``mavenCentral()``
in your ``repositories`` block such that those transient dependencies
can be resolved in the Maven Central repository. This message
exists to debug cases where a transient library actually **should** 
have been successfully resolved. This message may be removed in the
future.

