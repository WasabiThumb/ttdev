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
package io.github.wasabithumb.ttdev.util.validation

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject

internal class ErrorValidatedJson<T>(
    override val name: String,
    override val error: String
) : ValidatedJson<T> {

    override val value: T
        get() = throw UnsupportedOperationException("no value")

    override fun asObject(): ValidatedJson<JsonObject> = this.polymorph()
    override fun asArray(): ValidatedJson<JsonArray> = this.polymorph()
    override fun asString(): ValidatedJson<String> = this.polymorph()
    override fun asLong(): ValidatedJson<Long> = this.polymorph()
    override fun get(index: Int): ValidatedJson<JsonElement> = this.polymorph()
    override fun get(key: String): ValidatedJson<JsonElement> = this.polymorph()
    override fun elementCount(range: IntRange): ValidatedJson<T> = this.polymorph()
    override fun <O> element(key: Int, validator: ValidatedJson<JsonElement>.() -> ValidatedJson<O>): ValidatedJson<T> = this.polymorph()
    override fun <O> element(key: String, validator: ValidatedJson<JsonElement>.() -> ValidatedJson<O>): ValidatedJson<T> = this.polymorph()
    override fun elements(validate: ValidatedJson<JsonElement>.() -> Unit): ValidatedJson<T> = this.polymorph()
    override fun <O> matchingElement(matches: ValidatedJson<JsonElement>.() -> ValidatedJson<O>): ValidatedJson<O> = this.polymorph()
    override fun valueEquals(value: Any): ValidatedJson<T> = this.polymorph()
    override fun valueMatches(pattern: String): ValidatedJson<T> = this.polymorph()

    @Suppress("UNCHECKED_CAST")
    private fun <T> polymorph(): ValidatedJson<T> {
        return this as ValidatedJson<T>
    }

}