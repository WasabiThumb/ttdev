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

import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Provider

interface TTDevExtension {

    /**
     * Path of the TTDev repository.
     * Defaults to ``$GRADLE_USER_HOME/caches/ttdev``.
     */
    val repositoryDir: DirectoryProperty

    /**
     * Registers a Maven repository
     * which reads from the directory at
     * [repositoryDir].
     */
    fun repository(): ArtifactRepository

    /**
     * Fetches the Minecraft vanilla client as a library.
     * Returns a provider which, when evaluated, will
     * fetch this library and its dependents and place them
     * into a local [repository]. The resultant [Dependency]
     * can be resolved by Gradle to yield the full compile
     * classpath of this library.
     */
    fun minecraftClient(version: String): Provider<Dependency>

    /**
     * Fetches the Minecraft vanilla client as a library.
     * Returns a provider which, when evaluated, will
     * fetch this library and its dependents and place them
     * into a local [repository]. The resultant [Dependency]
     * can be resolved by Gradle to yield the full compile
     * classpath of this library.
     */
    fun minecraftClient(version: Provider<String>): Provider<Dependency> {
        return version.flatMap { this.minecraftClient(it) }
    }

    /**
     * Fetches the Minecraft vanilla server as a library.
     * Returns a provider which, when evaluated, will
     * fetch this library and its dependents and place them
     * into a local [repository]. The resultant [Dependency]
     * can be resolved by Gradle to yield the full compile
     * classpath of this library.
     */
    fun minecraftServer(version: String): Provider<Dependency>

    /**
     * Fetches the Minecraft vanilla server as a library.
     * Returns a provider which, when evaluated, will
     * fetch this library and its dependents and place them
     * into a local [repository]. The resultant [Dependency]
     * can be resolved by Gradle to yield the full compile
     * classpath of this library.
     */
    fun minecraftServer(version: Provider<String>): Provider<Dependency> {
        return version.flatMap { this.minecraftServer(it) }
    }

    /**
     * Fetches the Forge API & loader as a library.
     * Returns a provider which, when evaluated, will
     * fetch this library and its dependents and place them
     * into a local [repository]. The resultant [Dependency]
     * can be resolved by Gradle to yield the full compile
     * classpath of this library.
     */
    fun forge(version: String): Provider<Dependency>

    /**
     * Fetches the Forge API & loader as a library.
     * Returns a provider which, when evaluated, will
     * fetch this library and its dependents and place them
     * into a local [repository]. The resultant [Dependency]
     * can be resolved by Gradle to yield the full compile
     * classpath of this library.
     */
    fun forge(version: Provider<String>): Provider<Dependency> {
        return version.flatMap { this.forge(it) }
    }

    /**
     * Fetches the NeoForge API & loader as a library.
     * Returns a provider which, when evaluated, will
     * fetch this library and its dependents and place them
     * into a local [repository]. The resultant [Dependency]
     * can be resolved by Gradle to yield the full compile
     * classpath of this library.
     */
    fun neoForge(version: String): Provider<Dependency>

    /**
     * Fetches the NeoForge API & loader as a library.
     * Returns a provider which, when evaluated, will
     * fetch this library and its dependents and place them
     * into a local [repository]. The resultant [Dependency]
     * can be resolved by Gradle to yield the full compile
     * classpath of this library.
     */
    fun neoForge(version: Provider<String>): Provider<Dependency> {
        return version.flatMap { this.neoForge(it) }
    }

    /**
     * Fetches the Fabric loader as a library.
     * Returns a provider which, when evaluated, will
     * fetch this library and its dependents and place them
     * into a local [repository]. The resultant [Dependency]
     * can be resolved by Gradle to yield the full compile
     * classpath of this library.
     */
    fun fabricLoader(version: String): Provider<Dependency>

    /**
     * Fetches the Fabric loader as a library.
     * Returns a provider which, when evaluated, will
     * fetch this library and its dependents and place them
     * into a local [repository]. The resultant [Dependency]
     * can be resolved by Gradle to yield the full compile
     * classpath of this library.
     */
    fun fabricLoader(version: Provider<String>): Provider<Dependency> {
        return version.flatMap { this.fabricLoader(it) }
    }

    /**
     * Fetches the Fabric API as a library.
     * Returns a provider which, when evaluated, will
     * fetch this library and its dependents and place them
     * into a local [repository]. The resultant [Dependency]
     * can be resolved by Gradle to yield the full compile
     * classpath of this library.
     */
    fun fabricApi(version: String): Provider<Dependency>

    /**
     * Fetches the Fabric API as a library.
     * Returns a provider which, when evaluated, will
     * fetch this library and its dependents and place them
     * into a local [repository]. The resultant [Dependency]
     * can be resolved by Gradle to yield the full compile
     * classpath of this library.
     */
    fun fabricApi(version: Provider<String>): Provider<Dependency> {
        return version.flatMap { this.fabricApi(it) }
    }

}