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
package io.github.wasabithumb.ttdev.source.impl

import io.github.wasabithumb.ttdev.id.Identifier
import io.github.wasabithumb.ttdev.id.StandardIdentifiers
import io.github.wasabithumb.ttdev.source.LibrarySource
import io.github.wasabithumb.ttdev.source.LibrarySourceEntry
import io.github.wasabithumb.ttdev.source.LibrarySourcePackaging

class NeoForgeLibrarySource(
    url: String = "https://maven.neoforged.net/releases/"
) : LibrarySource {

    private val maven = MavenUrlLibrarySource(url, "^net\\.(minecraftforge|neoforged).*")

    //

    override fun resolve(identifier: Identifier): LibrarySourceEntry? {
        if (STUBBED.contains(identifier.withoutVersion())) return newStub(identifier)
        val ret = this.maven.resolve(identifier) ?: return null
        if (!StandardIdentifiers.NEOFORGE.matches(identifier)) return ret
        // Add the universal JAR as an artifact
        val universal = this.resolve(identifier.withClassifier("universal"))
            ?: throw IllegalStateException("Universal JAR could not be resolved")
        return ret.toBuilder()
            .packaging(universal.packaging)
            .artifact(universal.artifact)
            .build()
    }

    //

    companion object {

        private val STUBBED: Set<Identifier.Unversioned> = setOf(
            Identifier.of("net.neoforged", "minecraft-dependencies")
        )

        private fun newStub(identifier: Identifier): LibrarySourceEntry {
            if (identifier !is Identifier.Versioned) {
                throw IllegalArgumentException("Cannot generate stub for unversioned identifier")
            }
            return LibrarySourceEntry.builder(identifier)
                .packaging(LibrarySourcePackaging.POM)
                .build()
        }

    }

}