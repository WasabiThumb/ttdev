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
package io.github.wasabithumb.ttdev.resolver

import io.github.wasabithumb.ttdev.hash.FileHash
import io.github.wasabithumb.ttdev.hash.FileHashAlgorithm
import io.github.wasabithumb.ttdev.hash.FileHashInputStream
import io.github.wasabithumb.ttdev.id.Identifier
import io.github.wasabithumb.ttdev.source.LibrarySource
import io.github.wasabithumb.ttdev.source.LibrarySourceArtifact
import io.github.wasabithumb.ttdev.source.LibrarySourceEntry
import io.github.wasabithumb.ttdev.source.LibrarySourcePackaging
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyFactory
import org.gradle.api.logging.Logger
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStreamWriter
import java.nio.file.Files
import java.nio.file.Path
import java.util.SequencedSet

class LibraryResolver(
    val logger: Logger,
    val dependencyFactory: DependencyFactory,
    val sources: Collection<LibrarySource>,
    val dir: Path
) {

    fun resolve(identifier: Identifier.Versioned): Dependency {
        this.logger.info("Resolving library: $identifier")

        // Ensure that the content is fetched for this artifact and its dependencies
        this.fetchContent(identifier, IdentifierStack(), false)

        // Create the dependency object
        return this.dependencyFactory.create(
            identifier.group,
            identifier.name,
            identifier.version
        )
    }

    private fun fetchContent(identifier: Identifier, parents: IdentifierStack, optional: Boolean) {
        if (identifier in parents) {
            throw IllegalStateException("Library $identifier refers to itself (cyclic dependency chain)")
        }

        // Check if the library was already fetched, only if a fully qualified identifier was passed
        var storageSpec: LibraryStorageSpec? = null
        if (identifier is Identifier.Versioned) {
            storageSpec = this.storageSpec(identifier)
            if (storageSpec.isPopulated()) {
                this.logger.debug("- Skipping (POM exists)")
                return
            }
        }

        // Resolve
        val entry = this.sources.stream()
            .map { it.resolve(identifier) }
            .filter { it != null }
            .findFirst()
            .orElse(null)
            ?: if (optional) {
                this.logger.warn("- No source could resolve transient library $identifier, skipping")
                return
            } else {
                throw IllegalStateException("No source could resolve library $identifier")
            }

        // Check if the library was already fetched, if a fully qualified identifier is newly available
        if (storageSpec == null) {
            storageSpec = this.storageSpec(entry.identifier)
            if (storageSpec.isPopulated()) {
                this.logger.debug("- Skipping (POM exists after resolution)")
                return
            }
        }

        try {
            // Actually fetch
            storageSpec.createHomeIfNotExists()
            this.fetchContentCacheMiss(entry, storageSpec)

            // Recurse
            val depends = entry.depends
            if (depends.isNotEmpty()) {
                parents.push(identifier)
                for (depend in depends) {
                    this.logger.debug("Resolving depend: $depend")
                    fetchContent(depend, parents, true)
                }
                parents.pop()
            }
        } catch (e: Exception) {
            // Nuke the POM so that future runs know that the library was not fetched successfully
            try {
                Files.deleteIfExists(storageSpec.pom)
            } catch (e1: Exception) {
                e.addSuppressed(e1)
            }
            throw e
        }
    }

    private fun fetchContentCacheMiss(entry: LibrarySourceEntry, storage: LibraryStorageSpec) {
        // Determine the hash algorithm to use for stored content
        val hashAlgorithm = entry.artifact?.hash?.algorithm ?: FileHashAlgorithm.SHA1

        // Generate the POM
        this.logger.debug("- Generating POM")
        val pomData = generatePom(entry)
        ByteArrayInputStream(pomData).use { src ->
            pipeAndHash(src, storage.pom, hashAlgorithm, null)
        }

        val packaging = entry.packaging
        if (packaging == LibrarySourcePackaging.POM) return

        // Fetch artifact
        val artifact = entry.artifact
        if (artifact != null) {
            this.logger.debug("- Fetching artifact")
            if (!this.fetchArtifact(storage, packaging, artifact, hashAlgorithm, null)) {
                this.logger.debug("- Skipped artifact (hash collision)")
            }
        }

        // Fetch sources
        val sources = entry.sources
        if (sources != null) {
            this.logger.debug("- Fetching sources")
            if (!this.fetchArtifact(storage, packaging, sources, hashAlgorithm, "sources")) {
                this.logger.debug("- Skipped sources (hash collision)")
            }
        }

        // Fetch javadoc
        val javadoc = entry.javadoc
        if (javadoc != null) {
            this.logger.debug("- Fetching javadoc")
            if (!this.fetchArtifact(storage, packaging, javadoc, hashAlgorithm, "javadoc")) {
                this.logger.debug("- Skipped javadoc (hash collision)")
            }
        }
    }

    private fun fetchArtifact(
        storage: LibraryStorageSpec,
        packaging: LibrarySourcePackaging,
        artifact: LibrarySourceArtifact,
        hashAlgorithm: FileHashAlgorithm,
        classifier: String?
    ): Boolean {
        val hash = artifact.hash
        val dest = storage.file(packaging.identifier, classifier)
        if (hash != null && Files.exists(dest) && hash == hash.algorithm.hashFile(dest)) return false
        artifact.stream().use { src -> pipeAndHash(src, dest, hashAlgorithm, hash) }
        return true
    }

    private fun storageSpec(identifier: Identifier.Versioned): LibraryStorageSpec {
        var head: Path = this.dir
        for (part in identifier.toPath()) head = head.resolve(part)
        val home = head
        val title = "${identifier.name}-${identifier.version}"
        return LibraryStorageSpec(home, title)
    }

    private fun generatePom(entry: LibrarySourceEntry): ByteArray {
        return ByteArrayOutputStream().use { stream ->
            OutputStreamWriter(stream, Charsets.UTF_8).use { out ->
                out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                out.write("<project xmlns=\"http://maven.apache.org/POM/4.0.0\"" +
                        " xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd\"" +
                        " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n")
                out.write("\t<modelVersion>4.0.0</modelVersion>\n")

                val group = entry.identifier.group
                out.write("\t<groupId>")
                out.write(group)
                out.write("</groupId>\n")

                val name = entry.identifier.name
                out.write("\t<artifactId>")
                out.write(name)
                out.write("</artifactId>\n")

                val version = entry.identifier.version
                out.write("\t<version>")
                out.write(version)
                out.write("</version>\n")

                val packaging = entry.packaging
                out.write("\t<packaging>")
                out.write(packaging.identifier)
                out.write("</packaging>\n")

                out.write("\t<dependencies>\n")
                for (dep in entry.depends) {
                    val depGroup = dep.group
                    val depName = dep.name
                    val depVersion = dep.version
                    out.write("\t\t<dependency>\n")

                    out.write("\t\t\t<groupId>")
                    out.write(depGroup)
                    out.write("</groupId>\n")

                    out.write("\t\t\t<artifactId>")
                    out.write(depName)
                    out.write("</artifactId>\n")

                    if (depVersion != null) {
                        out.write("\t\t\t<version>")
                        out.write(depVersion)
                        out.write("</version>\n")
                    }

                    out.write("\t\t\t<scope>compile</scope>\n")
                    out.write("\t\t</dependency>\n")
                }
                out.write("\t</dependencies>\n")

                out.write("</project>\n")
            }
            stream.toByteArray()
        }
    }

    //

    private class IdentifierStack {

        private val set: SequencedSet<Identifier.Unversioned> = LinkedHashSet()

        //

        operator fun contains(identifier: Identifier): Boolean {
            return this.set.contains(identifier.withoutVersion())
        }

        fun push(identifier: Identifier) {
            if (!this.set.add(identifier.withoutVersion())) {
                throw IllegalStateException("Cannot push duplicate identifier to stack")
            }
        }

        fun pop() {
            val iter = this.set.reversed().iterator()
            try {
                iter.next()
            } catch (e: NoSuchElementException) {
                throw IllegalStateException("No identifier to pop", e)
            }
            iter.remove()
        }

    }

    private data class LibraryStorageSpec(
        val home: Path,
        val title: String
    ) {

        val pom: Path
            get() = this.file("pom")

        fun file(ext: String, classifier: String? = null): Path {
            return if (classifier == null) {
                this.home.resolve("${this.title}.$ext")
            } else {
                this.home.resolve("${this.title}-$classifier.$ext")
            }
        }

        fun isPopulated(): Boolean {
            return Files.exists(this.pom)
        }

        fun createHomeIfNotExists() {
            if (!Files.exists(this.home)) Files.createDirectories(this.home)
        }

    }

    companion object {

        internal fun pipeAndHash(src: InputStream, dest: Path, algo: FileHashAlgorithm, verify: FileHash?) {
            val hashDest = dest.parent.resolve(dest.fileName.toString() + "." + algo.extension)
            try {
                val hash = FileHashInputStream(algo, src).use { a ->
                    Files.newOutputStream(dest).use { b ->
                        a.transferTo(b)
                        a.hash
                    }
                }
                if (verify != null && verify != hash) {
                    throw IOException("Hash mismatch (expected $verify, got $hash)")
                }
                Files.newBufferedWriter(hashDest, Charsets.UTF_8).use { w ->
                    w.write(hash.hex)
                }
            } catch (e: IOException) {
                try {
                    deleteFilesIfExists(dest, hashDest)
                } catch (e2: IOException) {
                    e.addSuppressed(e2)
                }
                throw e
            }
        }

        internal fun deleteFilesIfExists(vararg files: Path) {
            var ex: IOException? = null
            for (file in files) {
                try {
                    Files.deleteIfExists(file)
                } catch (e: IOException) {
                    if (ex != null) e.addSuppressed(ex)
                    ex = e
                }
            }
            if (ex != null) {
                throw ex
            }
        }

    }

}