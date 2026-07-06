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

import io.github.wasabithumb.ttdev.hash.FileHash
import io.github.wasabithumb.ttdev.hash.FileHashAlgorithm
import io.github.wasabithumb.ttdev.hash.FileHashInputStream
import io.github.wasabithumb.ttdev.id.Identifier
import io.github.wasabithumb.ttdev.source.LibrarySource
import io.github.wasabithumb.ttdev.source.LibrarySourceEntry
import io.github.wasabithumb.ttdev.source.LibrarySourcePackaging
import io.github.wasabithumb.ttdev.source.helper.UrlLibrarySourceArtifact
import io.github.wasabithumb.ttdev.util.MultishotHttpInputStream
import org.apache.maven.model.Model
import org.apache.maven.model.io.xpp3.MavenXpp3Reader
import org.intellij.lang.annotations.Language
import java.io.IOException
import java.io.InputStreamReader
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.OptionalLong
import java.util.regex.Pattern

class MavenUrlLibrarySource(
    val url: String,
    @Language("RegExp") groupRegex: String = ".*",
    val eagerJar: Boolean = true
) : LibrarySource {

    private val groupPattern: Pattern = Pattern.compile(groupRegex)

    //

    override fun resolve(identifier: Identifier): LibrarySourceEntry? {
        if (!this.groupPattern.matcher(identifier.group).matches()) return null
        if (identifier !is Identifier.Versioned) {
            throw IllegalArgumentException("Source does not accept unversioned identifiers (got $identifier)")
        }

        val dir = this.libraryUrl(identifier)
        val title = "${identifier.name}-${identifier.version}"
        val location = RemoteLocation(dir, title, identifier.classifier)
        val pom = location.pom
        val pomInfo = TouchInfo.of(pom)
        if (!pomInfo.exists) {
            // May be a single JAR
            val single = location.resource("jar")
            val singleInfo = TouchInfo.of(single)
            if (!singleInfo.exists) return null
            return LibrarySourceEntry.jar(
                identifier,
                Artifact(single.toString(), singleInfo.size, singleInfo.hash)
            )
        }

        val model = readModel(pom, pomInfo.hash)
        var packaging = model.normalizedPackaging
        if (this.eagerJar &&
            packaging == LibrarySourcePackaging.POM &&
            TouchInfo.of(location.resource("jar")).exists) {
            packaging = LibrarySourcePackaging.JAR
        }
        val builder = LibrarySourceEntry.builder(identifier)
        builder.packaging(packaging)

        for (dep in model.dependencies) {
            val scope = dep.scope
            if (scope != null && scope != "compile" && scope != "provided") continue
            builder.depends(Identifier.of(dep.groupId, dep.artifactId, dep.version, dep.classifier))
        }

        location.artifactIfExists(packaging)?.let { builder.artifact(it) }
        location.artifactIfExists(packaging, "sources")?.let { builder.sources(it) }
        location.artifactIfExists(packaging, "javadoc")?.let { builder.javadoc(it) }
        return builder.build()
    }

    private fun readModel(pom: URI, hash: FileHash?): Model {
        val reader = MavenXpp3Reader()
        return MultishotHttpInputStream(pom).use { src ->
            val result: Model
            if (hash != null) {
                val hSrc = FileHashInputStream(hash.algorithm, src)
                val chars = InputStreamReader(hSrc, Charsets.UTF_8)
                result = reader.read(chars)
                val resultHash = hSrc.hash
                if (resultHash != hash) {
                    throw IOException("Hash mismatch (expected $hash, got $resultHash)")
                }
            } else {
                val chars = InputStreamReader(src, Charsets.UTF_8)
                result = reader.read(chars)
            }
            result
        }
    }

    private fun libraryUrl(identifier: Identifier): URI {
        var ret: String = this.url
        if (!ret.endsWith('/')) ret += "/"
        for (part in identifier.toPath()) {
            ret += "${part}/"
        }
        return URI.create(ret)
    }

    //

    private class Artifact(
        url: String,
        override val size: Long,
        override val hash: FileHash?
    ) : UrlLibrarySourceArtifact(url)

    private data class RemoteLocation(
        val dir: URI,
        val title: String,
        val classifier: String?
    ) {

        val pom: URI
            get() = this.resource("pom")

        fun resource(ext: String, classifier: String? = null): URI {
            val sb = StringBuilder(this.title)
            this.classifier?.let { sb.append('-').append(it) }
            classifier?.let { sb.append('-').append(it) }
            sb.append('.').append(ext)
            return this.dir.resolve(sb.toString())
        }

        fun artifactIfExists(ext: LibrarySourcePackaging, classifier: String? = null): Artifact? {
            val uri = this.resource(ext.identifier, classifier)
            val info = TouchInfo.of(uri)
            if (!info.exists) return null
            return Artifact(uri.toString(), info.size, info.hash)
        }

    }

    private sealed interface TouchInfo {
        val exists: Boolean
        val size: Long
        val hash: FileHash?

        private object None: TouchInfo {
            override val exists: Boolean
                get() = false
            override val size: Long
                get() = 0L
            override val hash: FileHash?
                get() = null
        }

        private data class Some(
            override val size: Long,
            override val hash: FileHash?
        ): TouchInfo {
            override val exists: Boolean
                get() = true
        }

        //

        companion object {

            fun of(url: URI): TouchInfo {
                val size = this.sizeOrEmpty(url)
                if (size.isEmpty) return None

                var hash: FileHash? = null
                for (algo in FileHashAlgorithm.entries) {
                    var sidecarPath = url.path
                    if (!sidecarPath.endsWith('/')) sidecarPath += "/"
                    sidecarPath += ".${algo.extension}"
                    val sidecar = url.resolve(sidecarPath)
                    val sidecarSize = this.sizeOrEmpty(sidecar)
                    if (sidecarSize.isEmpty) continue
                    val text = MultishotHttpInputStream(sidecar).use { stream ->
                        String(stream.readAllBytes(), Charsets.UTF_8).trim()
                    }
                    hash = FileHash.of(algo, text)
                    break
                }

                return Some(size.asLong, hash)
            }

            private fun sizeOrEmpty(url: URI): OptionalLong {
                HttpClient.newHttpClient().use { client ->
                    val request = HttpRequest.newBuilder(url)
                        .HEAD()
                        .build()

                    val response = client.send(request, HttpResponse.BodyHandlers.discarding())
                    val code = response.statusCode()
                    if (code == 404) return OptionalLong.empty()
                    if (code in 200 until 300) {
                        val length = response.headers().firstValueAsLong("Content-Length").orElse(-1L)
                        return OptionalLong.of(length)
                    }

                    throw IOException("Unexpected status code $code for resource at $url")
                }
            }

        }

    }

    companion object {

        internal val Model.normalizedPackaging: LibrarySourcePackaging
            get() {
                val value = this.packaging ?: return LibrarySourcePackaging.JAR
                return LibrarySourcePackaging.of(value) ?: throw IllegalStateException("Packaging \"$value\" not recognized")
            }

    }

}