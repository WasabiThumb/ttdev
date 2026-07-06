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

import java.io.InputStream
import java.security.MessageDigest

class FileHashInputStream(
    val algorithm: FileHashAlgorithm,
    private val source: InputStream
) : InputStream() {

    private val digest: MessageDigest = this.algorithm.newMessageDigest()
    private var digestResultGenerated: Boolean = false
    private lateinit var digestResult: FileHash

    //

    val hash: FileHash
        get() {
            if (this.digestResultGenerated) return this.digestResult
            val result = LazyFileHash(this.algorithm, this.digest.digest())
            this.digestResult = result
            this.digestResultGenerated = true
            return result
        }

    override fun read(): Int {
        this.checkNotConsumed()
        val b = this.source.read()
        if (b != -1) this.digest.update(b.toByte())
        return b
    }

    override fun read(b: ByteArray): Int {
        return this.read(b, 0, b.size)
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        this.checkNotConsumed()
        val read = this.source.read(b, off, len)
        if (read != -1) this.digest.update(b, off, read)
        return read
    }

    override fun close() {
        this.source.close()
    }

    private fun checkNotConsumed() {
        check (!this.digestResultGenerated) {
            "Cannot continue reading after hash has been generated"
        }
    }

}