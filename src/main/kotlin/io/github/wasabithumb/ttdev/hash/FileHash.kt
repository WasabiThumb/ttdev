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

interface FileHash {

    /** Algorithm used to generate this hash */
    val algorithm: FileHashAlgorithm

    /** Newly created array containing the byte values of this hash */
    val bytes: ByteArray

    /** Length of this hash in bytes */
    val byteLength: Int
        get() = this.algorithm.hashLength

    /** Hexadecimal string representing the bytes of this hash */
    val hex: String

    /** Retrieves the Nth byte of this hash */
    operator fun get(index: Int): Byte

    /** Alias for [hex] */
    override fun toString(): String

    //

    companion object {

        fun of(algorithm: FileHashAlgorithm, hex: String): FileHash {
            val bytes = hex.hexToByteArray()
            if (bytes.size != algorithm.hashLength) {
                throw IllegalArgumentException("Invalid length for ${algorithm.name} " +
                        "hash (expected ${algorithm.hashLength}, got ${bytes.size})")
            }
            return CompleteFileHash(algorithm, bytes, hex)
        }

    }

}