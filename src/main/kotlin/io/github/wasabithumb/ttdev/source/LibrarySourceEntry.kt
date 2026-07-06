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

import io.github.wasabithumb.ttdev.id.Identified
import io.github.wasabithumb.ttdev.id.Identifier

@ConsistentCopyVisibility
data class LibrarySourceEntry internal constructor(
    override val identifier: Identifier.Versioned,
    val depends: Set<Identifier>,
    val packaging: LibrarySourcePackaging,
    val artifact: LibrarySourceArtifact?,
    val sources: LibrarySourceArtifact?,
    val javadoc: LibrarySourceArtifact?
) : Identified<Identifier.Versioned> {

    class Builder(
        private val identifier: Identifier.Versioned
    ) {

        private val depends: MutableSet<Identifier> = mutableSetOf()
        private var packaging: LibrarySourcePackaging? = null
        private var artifact: LibrarySourceArtifact? = null
        private var sources: LibrarySourceArtifact? = null
        private var javadoc: LibrarySourceArtifact? = null

        //

        fun depends(identifier: Identifier): Builder {
            this.depends.add(identifier)
            return this
        }

        fun artifact(artifact: LibrarySourceArtifact?): Builder {
            this.artifact = artifact
            if (artifact != null && this.packaging == null) this.packaging = LibrarySourcePackaging.JAR
            return this
        }

        fun packaging(packaging: LibrarySourcePackaging): Builder {
            this.packaging = packaging
            return this
        }

        fun sources(sources: LibrarySourceArtifact?): Builder {
            this.sources = sources
            return this
        }

        fun javadoc(javadoc: LibrarySourceArtifact?): Builder {
            this.javadoc = javadoc
            return this
        }

        fun build(): LibrarySourceEntry {
            return LibrarySourceEntry(
                this.identifier,
                this.depends.toSet(),
                this.packaging ?: LibrarySourcePackaging.POM,
                this.artifact,
                this.sources,
                this.javadoc
            )
        }

    }

    companion object {

        fun jar(identifier: Identifier.Versioned, artifact: LibrarySourceArtifact): LibrarySourceEntry {
            return LibrarySourceEntry(
                identifier,
                emptySet(),
                LibrarySourcePackaging.JAR,
                artifact,
                null,
                null
            )
        }

        fun builder(identifier: Identifier.Versioned): Builder = Builder(identifier)

    }

}