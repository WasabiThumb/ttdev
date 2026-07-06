/*
 * TTDev Gradle Plugin
 * Copyright (c) 2026 Xavier Pedraza
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.wasabithumb.ttdev.source

import io.github.wasabithumb.ttdev.source.impl.ForgeLibrarySource
import io.github.wasabithumb.ttdev.source.impl.MavenUrlLibrarySource
import io.github.wasabithumb.ttdev.source.impl.MojangLibrarySource
import io.github.wasabithumb.ttdev.source.impl.NeoForgeLibrarySource
import java.util.WeakHashMap

object LibrarySources {

    private val mojang        = WeakHashMap<String, LibrarySource>()
    private val forge         = ForgeLibrarySource()
    private val neoforge      = NeoForgeLibrarySource()
    private val fabric        = MavenUrlLibrarySource("https://maven.fabricmc.net/", eagerJar = true)
    private val quilt         = MavenUrlLibrarySource("https://maven.quiltmc.org/repository/release/")
    private val spongepowered = MavenUrlLibrarySource("https://repo.spongepowered.org/repository/maven-public/")

    //

    fun mojang(version: String): LibrarySource {
        return this.mojang.computeIfAbsent(version) { MojangLibrarySource(version) }
    }

    fun forge(): LibrarySource {
        return this.forge
    }

    fun neoforge(): LibrarySource {
        return this.neoforge
    }

    fun fabric(): LibrarySource {
        return this.fabric
    }

    fun quilt(): LibrarySource {
        return this.quilt
    }

    fun spongepowered(): LibrarySource {
        return this.spongepowered
    }

}