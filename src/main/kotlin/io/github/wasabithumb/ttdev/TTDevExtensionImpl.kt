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
package io.github.wasabithumb.ttdev

import io.github.wasabithumb.ttdev.id.Identifier
import io.github.wasabithumb.ttdev.id.StandardIdentifiers
import io.github.wasabithumb.ttdev.resolver.LibraryResolver
import io.github.wasabithumb.ttdev.source.LibrarySource
import io.github.wasabithumb.ttdev.source.LibrarySources
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.maven

internal abstract class TTDevExtensionImpl(
    val project: Project
) : TTDevExtension {

    abstract override val repositoryDir: DirectoryProperty
    init {
        // Set property conventions
        this.repositoryDir.convention(project.layout.dir(project.provider {
            val home = project.gradle.gradleUserHomeDir
            home.toPath().resolve("caches", "ttdev").toFile()
        }))
    }

    /**
     * Registers a Maven repository
     * which reads from the directory at
     * [repositoryDir].
     */
    override fun repository(): ArtifactRepository {
        val url = this.repositoryDir.asFile.get().toURI().toString()
        return this.project.repositories.maven(url)
    }

    /**
     * Fetches the Minecraft vanilla client as a library.
     * Returns a provider which, when evaluated, will
     * fetch this library and its dependents and place them
     * into a local [repository]. The resultant [Dependency]
     * can be resolved by Gradle to yield the full compile
     * classpath of this library.
     */
    override fun minecraftClient(version: String): Provider<Dependency> {
        return this.target(
            LibrarySources.mojang(version),
            StandardIdentifiers.MINECRAFT_CLIENT.withVersion(version)
        )
    }

    /**
     * Fetches the Minecraft vanilla server as a library.
     * Returns a provider which, when evaluated, will
     * fetch this library and its dependents and place them
     * into a local [repository]. The resultant [Dependency]
     * can be resolved by Gradle to yield the full compile
     * classpath of this library.
     */
    override fun minecraftServer(version: String): Provider<Dependency> {
        return this.target(
            LibrarySources.mojang(version),
            StandardIdentifiers.MINECRAFT_SERVER.withVersion(version)
        )
    }

    /**
     * Fetches the Forge API & loader as a library.
     * Returns a provider which, when evaluated, will
     * fetch this library and its dependents and place them
     * into a local [repository]. The resultant [Dependency]
     * can be resolved by Gradle to yield the full compile
     * classpath of this library.
     */
    override fun forge(version: String): Provider<Dependency> {
        return this.target(
            LibrarySources.forge(),
            StandardIdentifiers.FORGE.withVersion(version)
        )
    }

    /**
     * Fetches the NeoForge API & loader as a library.
     * Returns a provider which, when evaluated, will
     * fetch this library and its dependents and place them
     * into a local [repository]. The resultant [Dependency]
     * can be resolved by Gradle to yield the full compile
     * classpath of this library.
     */
    override fun neoForge(version: String): Provider<Dependency> {
        return this.target(
            LibrarySources.neoforge(),
            StandardIdentifiers.NEOFORGE.withVersion(version)
        )
    }

    /**
     * Fetches the Fabric loader as a library.
     * Returns a provider which, when evaluated, will
     * fetch this library and its dependents and place them
     * into a local [repository]. The resultant [Dependency]
     * can be resolved by Gradle to yield the full compile
     * classpath of this library.
     */
    override fun fabricLoader(version: String): Provider<Dependency> {
        return this.target(
            LibrarySources.fabric(),
            StandardIdentifiers.FABRIC_LOADER.withVersion(version)
        )
    }

    /**
     * Fetches the Fabric API as a library.
     * Returns a provider which, when evaluated, will
     * fetch this library and its dependents and place them
     * into a local [repository]. The resultant [Dependency]
     * can be resolved by Gradle to yield the full compile
     * classpath of this library.
     */
    override fun fabricApi(version: String): Provider<Dependency> {
        return this.target(
            LibrarySources.fabric(),
            StandardIdentifiers.FABRIC_API.withVersion(version)
        )
    }

    private fun target(
        source: LibrarySource,
        identifier: Identifier.Versioned
    ): Provider<Dependency> {
        return this.repositoryDir.map { dir ->
            val resolver = LibraryResolver(
                this.project.logger,
                this.project.dependencyFactory,
                setOf(source),
                dir.asFile.toPath()
            )
            resolver.resolve(identifier)
        }
    }

}