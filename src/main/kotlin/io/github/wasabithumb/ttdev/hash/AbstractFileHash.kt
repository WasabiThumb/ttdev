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
package io.github.wasabithumb.ttdev.hash

internal abstract class AbstractFileHash(
    override val algorithm: FileHashAlgorithm
) : FileHash {

    override fun hashCode(): Int {
        var h = 7
        for (i in 0 until this.byteLength) {
            h = 31 * h + this[i].toInt().and(0xFF)
        }
        return h
    }

    override fun equals(other: Any?): Boolean {
        val qual = other as? FileHash ?: return false
        if (this.algorithm != qual.algorithm) return false
        val len = this.byteLength
        if (len != qual.byteLength) return false
        for (i in 0 until len) {
            if (this[i] != qual[i]) return false
        }
        return true
    }

    override fun toString(): String {
        return this.hex
    }

}