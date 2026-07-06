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

import java.util.Collections
import java.util.HashMap

enum class LibrarySourcePackaging(
    val identifier: String
) {
    POM("pom"),
    JAR("jar"),
    ZIP("zip"),
    WAR("war"),
    RAR("rar");

    override fun toString(): String {
        return this.identifier
    }

    companion object {

        private val BY_IDENTIFIER: Map<String, LibrarySourcePackaging> by lazy {
            val entries = entries
            val map = HashMap.newHashMap<String, LibrarySourcePackaging>(entries.size)
            for (entry in entries) map[entry.identifier] = entry
            Collections.unmodifiableMap(map)
        }

        fun of(value: String): LibrarySourcePackaging? {
            return BY_IDENTIFIER[value]
        }

    }
}