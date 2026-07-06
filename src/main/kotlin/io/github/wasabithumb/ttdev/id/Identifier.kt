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
package io.github.wasabithumb.ttdev.id

import java.util.Collections
import java.util.LinkedList
import java.util.Objects
import java.util.stream.IntStream

/**
 * Uniquely identifies a TTDev-managed dependency,
 * with or without version info.
 */
sealed class Identifier {

    abstract val group: String

    abstract val name: String

    abstract val version: String?

    abstract val classifier: String?

    abstract fun withVersion(version: String): Versioned

    abstract fun withoutVersion(): Unversioned

    abstract fun matches(other: Identifier): Boolean

    protected abstract fun chars(): IntStream

    fun toPath(): List<String> {
        val ret: MutableList<String> = LinkedList()
        ret.addAll(this.group.split(GROUP_SEPARATOR))
        ret.add(this.name)
        this.version?.let { ret.add(it) }
        return Collections.unmodifiableList(ret)
    }

    override fun hashCode(): Int {
        var h = 7
        for (c in this.chars()) h = 31 * h + c
        return h
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Identifier) return false
        return this.group == other.group &&
                this.name == other.name &&
                Objects.equals(this.version, other.version) &&
                Objects.equals(this.classifier, other.classifier)
    }

    override fun toString(): String {
        val sb = StringBuilder()
        for (c in this.chars()) sb.append(c.toChar())
        return sb.toString()
    }

    //

    class Unversioned(
        override val group: String,
        override val name: String,
        override val classifier: String? = null
    ) : Identifier() {

        override val version: String?
            get() = null

        override fun chars(): IntStream {
            var ret = this.group.chars()
            ret = IntStream.concat(ret, IntStream.of(DELIMITER.code))
            ret = IntStream.concat(ret, this.name.chars())
            return ret
        }

        override fun withVersion(version: String): Versioned {
            return Versioned(this.group, this.name, version, this.classifier)
        }

        override fun withoutVersion(): Unversioned {
            return this
        }

        override fun matches(other: Identifier): Boolean {
            return this.group == other.group &&
                    this.name == other.name &&
                    Objects.equals(this.classifier, other.classifier)
        }

    }

    class Versioned(
        override val group: String,
        override val name: String,
        override val version: String,
        override val classifier: String? = null
    ) : Identifier() {

        override fun chars(): IntStream {
            var ret = this.group.chars()
            ret = IntStream.concat(ret, IntStream.of(DELIMITER.code))
            ret = IntStream.concat(ret, this.name.chars())
            ret = IntStream.concat(ret, IntStream.of(DELIMITER.code))
            ret = IntStream.concat(ret, this.version.chars())
            this.classifier?.let {
                ret = IntStream.concat(ret, IntStream.of(DELIMITER.code))
                ret = IntStream.concat(ret, it.chars())
            }
            return ret
        }

        override fun withVersion(version: String): Versioned {
            if (this.version == version) return this
            return Versioned(this.group, this.name, version, this.classifier)
        }

        override fun withoutVersion(): Unversioned {
            return Unversioned(this.group, this.name, this.classifier)
        }

        override fun matches(other: Identifier): Boolean {
            val ov = other.version
            return (ov == null || this.version == ov) &&
                    this.group == other.group &&
                    this.name == other.name &&
                    Objects.equals(this.classifier, other.classifier)
        }

    }

    //

    companion object {

        const val DELIMITER = ':'
        const val GROUP_SEPARATOR = '.'

        fun of(any: Any): Identifier {
            if (any is Identifier) return any
            if (any is Identified<*>) return any.identifier
            return of("$any")
        }

        fun of(notation: String): Identifier {
            val parts = notation.split(DELIMITER)
            return when (parts.size) {
                2 -> Unversioned(parts[0], parts[1])
                3 -> Versioned(parts[0], parts[1], parts[2])
                4 -> Versioned(parts[0], parts[1], parts[2], parts[3])
                else -> throw IllegalArgumentException("Invalid identifier: $notation")
            }
        }

        fun of(group: String, name: String): Unversioned {
            return Unversioned(group, name)
        }

        fun of(group: String, name: String, version: String): Versioned {
            return Versioned(group, name, version)
        }

        fun of(group: String, name: String, version: String?, classifier: String? = null): Identifier {
            return if (version == null) {
                Unversioned(group, name, classifier)
            } else {
                Versioned(group, name, version, classifier)
            }
        }

    }

}