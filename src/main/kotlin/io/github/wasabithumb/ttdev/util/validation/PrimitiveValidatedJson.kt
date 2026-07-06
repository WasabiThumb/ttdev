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
import java.util.Objects
import java.util.regex.Pattern

class PrimitiveValidatedJson<T>(
    override val name: String,
    override val value: T
) : ValidatedJson<T> {

    override val error: String?
        get() = null

    override fun asObject(): ValidatedJson<JsonObject> {
        return this.newError("not an object")
    }

    override fun asArray(): ValidatedJson<JsonArray> {
        return this.newError("not an object")
    }

    override fun asString(): ValidatedJson<String> {
        if (this.value is String) return this.polymorph()
        return this.newError("not a string")
    }

    override fun asLong(): ValidatedJson<Long> {
        if (this.value is Long) return this.polymorph()
        return this.newError("not a long")
    }

    override fun get(index: Int): ValidatedJson<JsonElement> {
        return this.newError("primitive has no elements")
    }

    override fun get(key: String): ValidatedJson<JsonElement> {
        return this.newError("primitive has no elements")
    }

    override fun elementCount(range: IntRange): ValidatedJson<T> {
        return this.newError("primitive has no elements")
    }

    override fun <O> element(key: Int, validator: ValidatedJson<JsonElement>.() -> ValidatedJson<O>): ValidatedJson<T> {
        return this.newError("primitive has no elements")
    }

    override fun <O> element(key: String, validator: ValidatedJson<JsonElement>.() -> ValidatedJson<O>): ValidatedJson<T> {
        return this.newError("primitive has no elements")
    }

    override fun elements(validate: ValidatedJson<JsonElement>.() -> Unit): ValidatedJson<T> {
        return this.newError("primitive has no elements")
    }

    override fun <O> matchingElement(matches: ValidatedJson<JsonElement>.() -> ValidatedJson<O>): ValidatedJson<O> {
        return this.newError("primitive has no elements")
    }

    override fun valueEquals(value: Any): ValidatedJson<T> {
        if (Objects.equals(this.value, value)) return this
        return this.newError("value is not equal to $value")
    }

    override fun valueMatches(pattern: String): ValidatedJson<T> {
        if (Pattern.compile(pattern).matcher("${this.value}").matches()) return this
        return this.newError("value does not match pattern $pattern")
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> polymorph(): ValidatedJson<T> {
        return this as ValidatedJson<T>
    }

    private fun <O> newError(message: String): ErrorValidatedJson<O> {
        return ErrorValidatedJson(this.name, message)
    }

}