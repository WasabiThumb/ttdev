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

class ForgeLibrarySource(
    url: String = "https://maven.minecraftforge.net/releases/"
) : LibrarySource {

    private val maven = MavenUrlLibrarySource(url)

    //

    override fun resolve(identifier: Identifier): LibrarySourceEntry? {
        if (identifier !is Identifier.Versioned) throw IllegalArgumentException("Source does not accept unversioned identifiers (got $identifier)")
        if (FORGE_GROUP != identifier.group) return null

        // BOM
        if (FORGE_NAME == identifier.name && identifier.classifier == null) {
            val version = identifier.version
            val builder = LibrarySourceEntry.builder(identifier)
            for (name in CORE) builder.depends(Identifier.of(FORGE_GROUP, name, version))
            return builder.build()
        }

        return this.maven.resolve(identifier)
    }

    //

    companion object {

        private val FORGE_GROUP: String
            get() = StandardIdentifiers.FORGE.group

        private val FORGE_NAME: String
            get() = StandardIdentifiers.FORGE.name

        // Not sure how else to determine this
        private val CORE: Set<String> = setOf(
            "fmlcore",
            "fmlearlydisplay",
            "fmlloader",
            "forge-transformers",
            "javafmllanguage",
            "lowcodelanguage",
            "mclanguage"
        )

    }

}