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
import org.intellij.lang.annotations.Language

interface ValidatedJson<out T> {

    val name: String

    val value: T

    val error: String?

    fun asObject(): ValidatedJson<JsonObject>

    fun asArray(): ValidatedJson<JsonArray>

    fun asString(): ValidatedJson<String>

    fun asLong(): ValidatedJson<Long>

    fun get(index: Int): ValidatedJson<JsonElement>

    fun get(key: String): ValidatedJson<JsonElement>

    fun elementCount(count: Int): ValidatedJson<T> {
        return this.elementCount(count .. count)
    }

    fun elementCount(range: IntRange): ValidatedJson<T>

    fun <O> element(key: Int, validator: ValidatedJson<JsonElement>.() -> ValidatedJson<O>): ValidatedJson<T>

    fun <O> element(key: String, validator: ValidatedJson<JsonElement>.() -> ValidatedJson<O>): ValidatedJson<T>

    fun elements(validate: ValidatedJson<JsonElement>.() -> Unit): ValidatedJson<T>

    fun <O> matchingElement(matches: ValidatedJson<JsonElement>.() -> ValidatedJson<O>): ValidatedJson<O>

    fun valueEquals(value: Any): ValidatedJson<T>

    fun valueMatches(@Language("RegExp") pattern: String): ValidatedJson<T>

    fun unrwap(): T {
        val error = this.error ?: return this.value
        throw IllegalStateException("${this.name} is invalid ($error)")
    }

    //

    companion object {

        fun <T> of(name: String, value: T): ValidatedJson<T> {
            if (value is JsonElement) return ElementValidatedJson(name, value)
            return PrimitiveValidatedJson(name, value)
        }

    }

}