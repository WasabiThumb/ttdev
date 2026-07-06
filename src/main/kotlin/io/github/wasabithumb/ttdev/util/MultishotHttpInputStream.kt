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
package io.github.wasabithumb.ttdev.util

import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.regex.Pattern
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class MultishotHttpInputStream(
    private val url: URI,
    private val maxRetries: Int = 5,
    private val retryDelay: Duration = 1.seconds,
    private val bufferSize: Int = 8192, // 8 KB
    private val skipReconnectThreshold: Long = 52428800L, // 50 MB
    private val timeout: Duration = 10.seconds,
) : InputStream() {

    private val tryErrors: MutableList<IOException> = ArrayList(maxRetries)
    private val client: HttpClient = HttpClient.newBuilder().connectTimeout(timeout.toJavaDuration()).build()
    private var shot: Shot? = null
    private var retries: Int = 0
    private var mark: Long = 0L

    constructor(
        url: String
    ): this(URI.create(url))

    //

    override fun read(): Int {
        return this.useShot { it.read() }
    }

    override fun read(b: ByteArray): Int {
        return this.read(b, 0, b.size)
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        return this.useShot { it.read(b, off, len) }
    }

    override fun skip(n: Long): Long {
        if (n <= 0L) return 0L
        if (n >= this.skipReconnectThreshold) {
            val shot = this.shot
            var pos = 0L
            var toSkip: Long = n
            if (shot != null) {
                this.shot = null
                shot.close()
                val total = shot.total
                if (total >= 0) {
                    val rem = total - shot.position
                    if (n > rem) toSkip = rem
                }
                pos = shot.position
            }
            this.shot = this.newShot(pos + toSkip)
            return toSkip
        } else {
            return this.useShot { it.skip(n) }
        }
    }

    override fun markSupported(): Boolean {
        return true
    }

    override fun mark(readlimit: Int) {
        this.mark = this.shot?.position ?: 0L
    }

    override fun reset() {
        val shot = this.shot
        if (shot != null) {
            this.shot = null
            shot.close()
        }
        this.shot = this.newShot(this.mark)
    }

    override fun available(): Int {
        return this.useShot { it.available() }
    }

    override fun close() {
        try {
            this.shot?.close()
        } finally {
            this.client.close()
        }
    }

    private fun <T> useShot(task: (shot: Shot) -> T): T {
        var bufferFactory: BufferFactory? = null
        var shot: Shot? = this.shot
        var position: Long = shot?.position ?: 0L
        while (true) {
            val retries = this.retries
            if (retries != 0 && retries > this.maxRetries) {
                val ex = IOException("Transfer from ${this.url} failed (retried x$retries)")
                for (error in this.tryErrors) ex.addSuppressed(error)
                throw ex
            }
            try {
                if (shot == null) {
                    if (bufferFactory == null) bufferFactory = BufferFactory()
                    shot = this.newShot(position, bufferFactory)
                    this.shot = shot
                }
                return task(shot)
            } catch (e: IOException) {
                if (shot != null) {
                    this.shot = null
                    shot.close()
                    position = shot.position
                    shot = null
                }
                this.tryErrors.add(e)
                this.retries = retries + 1
                Thread.sleep(this.retryDelay.toJavaDuration())
            }
        }
    }

    private fun newShot(position: Long, bufferFactory: BufferFactory = BufferFactory()): Shot {
        val requestBuilder = HttpRequest.newBuilder(this.url)
        requestBuilder.timeout(this.timeout.toJavaDuration())
        if (position != 0L) requestBuilder.setHeader("Range", "bytes=$position-")
        val request = requestBuilder.build()
        val response = this.client.send(request, HttpResponse.BodyHandlers.ofInputStream())
        val code = response.statusCode()
        if (code !in 200 until 300) throw IOException("Non-2XX status code $code")
        val isPartial = position != 0L && code == 206
        val total = calcResponseLength(response, isPartial, position)
        val bufferLength = if (total in 1 .. this.bufferSize) total.toInt() else this.bufferSize
        val buffer = bufferFactory.get(bufferLength)
        return if (position != 0L && code == 206) {
            Shot(buffer, response.body(), total, position)
        } else {
            Shot(buffer, response.body(), total, 0L)
        }
    }

    //

    private class Shot(
        val buffer: ByteArray,
        val stream: InputStream,
        val total: Long,
        @Volatile var position: Long
    ) : InputStream() {

        private var rd: Int = 0
        private var wr: Int = 0
        private var eof: Boolean = false

        override fun read(): Int {
            val readable = this.poll()
            if (readable == 0) return -1
            val b = this.buffer[this.rd]
            this.rd = (this.rd + 1) % this.buffer.size
            this.position++
            return b.toUByte().toInt()
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (len <= 0) return 0
            val readable = this.poll()
            if (readable == 0) return -1
            val count = minOf(readable, len)
            val head = this.rd
            val target = head + count
            if (target > this.buffer.size) {
                val upper = this.buffer.size - head
                System.arraycopy(this.buffer, head, b, off, upper)
                System.arraycopy(this.buffer, 0, b, off + upper, count - upper)
                this.rd = target % this.buffer.size
            } else {
                System.arraycopy(this.buffer, head, b, off, count)
                this.rd = if (target == this.buffer.size) 0 else target
            }
            this.position += count
            return count
        }

        override fun skip(n: Long): Long {
            val skipped = this.stream.skip(minOf(this.remaining, n))
            this.position += skipped
            return skipped
        }

        override fun available(): Int {
            val rd = this.readable
            if (rd == 0) return this.stream.available()
            return rd
        }

        override fun close() {
            this.stream.close()
        }

        /** Only returns 0 at end of data */
        private fun poll(): Int {
            // Content-Length/Content-Range limit
            val limit = this.remaining
            if (limit <= 0L) return 0

            while (true) {
                // Try to fill the buffer if we haven't hit EOF
                if (!this.eof) {
                    var quota: Int = this.writable
                    while (quota != 0) {
                        val head = this.wr
                        val regionLimit = this.buffer.size - head
                        val written = this.stream.read(this.buffer, this.wr, minOf(quota, regionLimit))
                        if (written == -1) {
                            this.eof = true
                            break
                        }
                        this.wr = (this.wr + written) % this.buffer.size
                        quota -= written
                        if (this.stream.available() == 0) {
                            break
                        }
                    }
                }

                // Try to drain the buffer
                val readable = this.readable
                if (readable == 0) {
                    if (this.eof) return 0
                    continue
                }

                // Enforce limit on readable count
                return if (readable.toLong() > limit) limit.toInt() else readable
            }
        }

        private val readable: Int
            get() = if (this.wr >= this.rd) {
                this.wr - this.rd
            } else {
                this.buffer.size - this.rd + this.wr
            }

        private val writable: Int
            get() = if (this.wr < this.rd) {
                this.rd - this.wr - 1
            } else {
                this.buffer.size - this.wr + this.rd - 1
            }

        private val remaining: Long
            get() = if (this.total >= 0L) { this.total - this.position } else { 0x7FFFFFFFL }

    }

    private class BufferFactory {

        private var value: ByteArray? = null

        //

        fun get(size: Int): ByteArray {
            val value = this.value
            if (value == null || value.size < size) {
                val created = ByteArray(size)
                this.value = created
                return created
            } else {
                return value
            }
        }

    }

    companion object {

        private val CONTENT_RANGE_PATTERN: Pattern = Pattern.compile(
            "^bytes\\x20(\\d+)-(?:\\d+)?/(\\d+)$",
            Pattern.CASE_INSENSITIVE
        )

        internal fun calcResponseLength(response: HttpResponse<*>, isPartial: Boolean, intendedPosition: Long): Long {
            if (!isPartial) return response.headers().firstValueAsLong("Content-Length").orElse(-1L)
            val range = response.headers().firstValue("Content-Range")
            if (!range.isPresent) return -1L
            val matcher = CONTENT_RANGE_PATTERN.matcher(range.get())
            if (!matcher.matches()) return -1L
            val start = matcher.group(1).toLong()
            if (intendedPosition != start) {
                throw IOException("Received Content-Range start $start is not intended position of $intendedPosition")
            }
            return matcher.group(2).toLong()
        }

    }

}