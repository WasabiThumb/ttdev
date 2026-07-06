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

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import io.github.wasabithumb.ttdev.hash.FileHash
import io.github.wasabithumb.ttdev.hash.FileHashAlgorithm
import io.github.wasabithumb.ttdev.id.Identifier
import io.github.wasabithumb.ttdev.id.StandardIdentifiers
import io.github.wasabithumb.ttdev.source.LibrarySource
import io.github.wasabithumb.ttdev.source.LibrarySourceArtifact
import io.github.wasabithumb.ttdev.source.LibrarySourceEntry
import io.github.wasabithumb.ttdev.source.helper.UrlLibrarySourceArtifact
import io.github.wasabithumb.ttdev.util.JsonBodyHandler
import io.github.wasabithumb.ttdev.util.MultishotHttpInputStream
import io.github.wasabithumb.ttdev.util.validation.ValidatedJson
import org.jetbrains.annotations.Blocking
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class MojangLibrarySource(
    val version: String
) : LibrarySource {

    private val clientIdentifier: Identifier.Versioned = StandardIdentifiers.MINECRAFT_CLIENT.withVersion(this.version)
    private val serverIdentifier: Identifier.Versioned = StandardIdentifiers.MINECRAFT_SERVER.withVersion(this.version)

    private val packageInfo: PackageInfo by lazy {
        val pkgUrl = getPackageUrl(this.version)
        val json = fetchJson(pkgUrl)
        PackageInfo.validateAndParse(json)
    }

    private val serverBundleInfo: ServerBundleInfo by lazy {
        ServerBundleInfo.read(this.packageInfo.server.url, this.version)
    }

    //

    @Blocking
    override fun resolve(identifier: Identifier): LibrarySourceEntry? {
        if (this.clientIdentifier.matches(identifier)) {
            // client
            return LibrarySourceEntry.builder(this.clientIdentifier)
                .artifact(this.packageInfo.client)
                .clientLibraries()
                .build()
        } else if (this.serverIdentifier.matches(identifier)) {
            // server
            val info = this.serverBundleInfo
            val builder = LibrarySourceEntry.builder(this.serverIdentifier).artifact(info.api)
            for ((k, v) in info.libraries) builder.depends(k.withVersion(v.version))
            return builder.build()
        } else {
            // libraries
            // client uses a lot of the same libraries as server, but the client
            // path is much more efficient. hence the order here is important!
            val clientLibrary = matchClientLibrary(identifier)
            if (clientLibrary != null) return clientLibrary
            return matchServerLibrary(identifier)
        }
    }

    private fun matchClientLibrary(identifier: Identifier): LibrarySourceEntry? {
        val lib = this.packageInfo.clientLibraries[identifier.withoutVersion()] ?: return null
        val versioned: Identifier.Versioned = if (identifier is Identifier.Versioned) {
            if (lib.version != identifier.version) return null
            identifier
        } else {
            identifier.withVersion(lib.version)
        }
        return LibrarySourceEntry.jar(versioned, lib.artifact)
    }

    private fun matchServerLibrary(identifier: Identifier): LibrarySourceEntry? {
        val lib = this.serverBundleInfo.libraries[identifier.withoutVersion()] ?: return null
        val versioned: Identifier.Versioned = if (identifier is Identifier.Versioned) {
            if (lib.version != identifier.version) return null
            identifier
        } else {
            identifier.withVersion(lib.version)
        }
        return LibrarySourceEntry.jar(versioned, lib)
    }

    private fun LibrarySourceEntry.Builder.clientLibraries(): LibrarySourceEntry.Builder {
        for (library in this@MojangLibrarySource.packageInfo.clientLibraries) {
            this.depends(library.key.withVersion(library.value.version))
        }
        return this
    }

    override fun hashCode(): Int {
        return this.version.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is MojangLibrarySource && this.version == other.version
    }

    override fun toString(): String {
        return "MojangLibrarySource{version=${this.version}}"
    }

    //

    private class ServerBundleJarInJarArtifact(
        val url: String,
        val entryName: String,
        val version: String,
        override val hash: FileHash
    ) : LibrarySourceArtifact {

        override val size: Long
            get() = -1L

        override fun stream(): InputStream {
            val zip = ZipInputStream(MultishotHttpInputStream(this.url))
            var close = true
            try {
                var entry: ZipEntry
                while (true) {
                    entry = zip.nextEntry ?: break
                    if (entry.name == this.entryName) {
                        close = false
                        return zip
                    }
                }
                throw IOException("Entry ${this.entryName} not found in server bundle")
            } finally {
                if (close) zip.close()
            }
        }

    }

    private data class ServerBundleInfo(
        val api: ServerBundleJarInJarArtifact,
        val libraries: Map<Identifier.Unversioned, ServerBundleJarInJarArtifact>
    ) {

        companion object {

            private const val FOUND_VERSIONS = 0b01
            private const val FOUND_LIBRARIES = 0b10
            private const val FOUND_ALL = 0b11

            //

            fun read(url: String, version: String): ServerBundleInfo {
                ZipInputStream(MultishotHttpInputStream(url)).use { zip ->
                    var entry: ZipEntry
                    var apiEntry: String? = null
                    var apiHash: FileHash? = null
                    val libraries: MutableMap<Identifier.Unversioned, ServerBundleJarInJarArtifact> = mutableMapOf()
                    var found = 0

                    while (true) {
                        entry = zip.nextEntry ?: break
                        if ("META-INF/versions.list" == entry.name) {
                            val v = parseVersions(zip, version)
                            apiEntry = v.first
                            apiHash = v.second
                            found = found.or(FOUND_VERSIONS)
                            if (found == FOUND_ALL) break
                        } else if ("META-INF/libraries.list" == entry.name) {
                            parseLibraries(zip, url, libraries)
                            found = found.or(FOUND_LIBRARIES)
                            if (found == FOUND_ALL) break
                        }
                    }

                    when (found) {
                        0 -> throw IOException("Could not find versions.list, libraries.list in server bundle")
                        (FOUND_VERSIONS) -> throw IOException("Could not find libraries.list in server bundle")
                        (FOUND_LIBRARIES) -> throw IOException("Could not find versions.list in server bundle")
                    }

                    return ServerBundleInfo(
                        ServerBundleJarInJarArtifact(
                            url,
                            apiEntry!!,
                            version,
                            apiHash!!
                        ),
                        libraries.toMap()
                    )
                }
            }

            private fun parseLibraries(
                stream: InputStream,
                url: String,
                dest: MutableMap<Identifier.Unversioned, ServerBundleJarInJarArtifact>
            ) {
                val r = BufferedReader(InputStreamReader(stream, Charsets.UTF_8))
                var line: String
                while (true) {
                    line = r.readLine() ?: break
                    val parts = line.split("\\s+".toRegex())
                    if (parts.size != 3) throw IOException("Failed to parse libraries.list entry: $line")
                    val identifier = Identifier.of(applyPrefix(parts[1]))
                    if (identifier !is Identifier.Versioned) {
                        throw IOException("Identifier $identifier in libaries.list has no version")
                    }
                    dest[identifier.withoutVersion()] = ServerBundleJarInJarArtifact(
                        url,
                        "META-INF/libraries/${parts[2]}",
                        identifier.version,
                        FileHash.of(FileHashAlgorithm.SHA256, parts[0])
                    )
                }
            }

            private fun parseVersions(stream: InputStream, targetVersion: String): Pair<String, FileHash> {
                val r = BufferedReader(InputStreamReader(stream, Charsets.UTF_8))
                var line: String
                while (true) {
                    line = r.readLine() ?: break
                    val parts = line.split("\\s+".toRegex())
                    if (parts.size != 3) continue
                    if (targetVersion != parts[1]) continue
                    return "META-INF/versions/${parts[2]}" to FileHash.of(FileHashAlgorithm.SHA256, parts[0])
                }
                throw IOException("Version $targetVersion not found in versions.list")
            }

        }

    }

    private class ArtifactInfo(
        url: String,
        override val size: Long,
        override val hash: FileHash
    ) : UrlLibrarySourceArtifact(url) {

        companion object {

            fun parse(obj: JsonObject): ArtifactInfo {
                return ArtifactInfo(
                    url = obj.get("url").asString,
                    size = obj.get("size").asLong,
                    hash = FileHash.of(FileHashAlgorithm.SHA1, obj.get("sha1").asString)
                )
            }

            fun validate(element: ValidatedJson<JsonElement>): ValidatedJson<JsonObject> {
                return element.asObject()
                    .element("url") { asString() }
                    .element("size") { asLong() }
                    .element("sha1") { asString() }
            }

        }

    }

    private data class LibraryInfo(
        val version: String,
        val artifact: ArtifactInfo
    )

    private data class PackageInfo(
        val client: ArtifactInfo,
        val server: ArtifactInfo,
        val clientLibraries: Map<Identifier.Unversioned, LibraryInfo>
    ) {

        companion object {

            fun validateAndParse(element: JsonElement, name: String = "package"): PackageInfo {
                val obj = validate(ValidatedJson.of(name, element)).unrwap()
                return parse(obj)
            }

            fun parse(obj: JsonObject): PackageInfo {
                val downloads = obj.get("downloads").asJsonObject
                val librariesArray = obj.get("libraries").asJsonArray

                val client = ArtifactInfo.parse(downloads.get("client").asJsonObject)
                val server = ArtifactInfo.parse(downloads.get("server").asJsonObject)
                val libraries: MutableMap<Identifier.Unversioned, LibraryInfo> = java.util.HashMap.newHashMap(librariesArray.size())

                for (el in librariesArray) {
                    val qual = el.asJsonObject
                    if (qual.has("rules")) continue // dependency is conditional
                    val name = qual.get("name").asString
                    val identifier = Identifier.of(applyPrefix(name)) as Identifier.Versioned
                    val artifact = ArtifactInfo.parse(qual.get("downloads").asJsonObject.get("artifact").asJsonObject)
                    libraries[identifier.withoutVersion()] = LibraryInfo(
                        version = identifier.version,
                        artifact = artifact
                    )
                }

                return PackageInfo(client, server, libraries.toMap())
            }

            fun validate(element: ValidatedJson<JsonElement>): ValidatedJson<JsonObject> {
                return element.asObject()
                    .element("downloads") {
                        asObject()
                            .element("client", ArtifactInfo::validate)
                            .element("server", ArtifactInfo::validate)
                    }
                    .element("libraries") {
                        asArray().elements {
                            asObject()
                                .element("name") {
                                    asString()
                                        .valueMatches("^(.*):(.*):(.*)$")
                                }
                                .element("downloads") {
                                    asObject()
                                        .element("artifact", ArtifactInfo::validate)
                                }
                        }
                    }
            }

        }

    }

    companion object {

        const val VERSION_MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"

        internal fun applyPrefix(id: String): String {
            return if (id.startsWith("com.mojang") || id.startsWith("net.minecraft")) {
                // Should be resolved by this source
                id
            } else {
                // Should be resolved by central
                "net.minecraft.libraries.${id}"
            }
        }

        private fun fetchJson(url: String): JsonElement {
            HttpClient.newHttpClient().use { client ->
                val request = HttpRequest.newBuilder(URI.create(url))
                    .header("Accept", "application/json")
                    .build()

                val response = client.send(request, JsonBodyHandler)
                val status = response.statusCode()
                if (status !in 200 until 300) throw IOException("Non-2XX status code $status")

                return response.body()
            }
        }

        private fun getPackageUrl(ver: String): String {
            val versions = fetchJson(VERSION_MANIFEST_URL)
                .takeIf { it.isJsonObject }
                ?.asJsonObject
                ?.get("versions")
                ?.takeIf { it.isJsonArray }
                ?.asJsonArray
                ?: throw IllegalStateException("Malformed version manifest")

            for (version in versions) {
                if (!version.isJsonObject) continue
                val qual = version.asJsonObject
                val id = qual.get("id") ?: continue
                if (!id.isJsonPrimitive) continue
                val url = qual.get("url") ?: continue
                if (!url.isJsonPrimitive) continue
                if (ver != id.asString) continue
                return url.asString
            }

            throw IllegalStateException("Version $ver not found in manifest")
        }

    }

}