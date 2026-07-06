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
import java.util.regex.Pattern
import java.util.stream.IntStream
import kotlin.collections.iterator

class ElementValidatedJson<T: JsonElement>(
    override val name: String,
    override val value: T
) : ValidatedJson<T> {

    override val error: String?
        get() = null

    override fun asObject(): ValidatedJson<JsonObject> {
        return if (this.value.isJsonObject) {
            this.polymorphElement { it.asJsonObject }
        } else {
            this.newError("not an object")
        }
    }

    override fun asArray(): ValidatedJson<JsonArray> {
        return if (this.value.isJsonArray) {
            this.polymorphElement { it.asJsonArray }
        } else {
            this.newError("not an array")
        }
    }

    override fun asString(): ValidatedJson<String> {
        return if (this.value.isJsonPrimitive && this.value.asJsonPrimitive.isString) {
            this.polymorphPrimitive { it.asString }
        } else {
            this.newError("not an string")
        }
    }

    override fun asLong(): ValidatedJson<Long> {
        return if (this.value.isJsonPrimitive && this.value.asJsonPrimitive.isNumber) {
            val long: Long
            try {
                long = this.value.asLong
                return PrimitiveValidatedJson(this.name, long)
            } catch (_: NumberFormatException) {
                this.newError("not a long")
            }
        } else {
            this.newError("not an string")
        }
    }

    override fun get(index: Int): ValidatedJson<JsonElement> {
        val element: JsonElement
        val name: String

        if (this.value.isJsonObject) {
            element = this.value.asJsonObject.get("$index") ?: return this.newError("missing key \"$index\"")
            name = "${this.name}.$index"
        } else if (this.value.isJsonArray) {
            val arr = this.value.asJsonArray
            if (index < 0 || index >= arr.size()) {
                return this.newError("index $index out of bounds for array length ${arr.size()}")
            }
            element = arr[index]
            name = "${this.name}[$index]"
        } else {
            return this.newError("not an indexable type")
        }

        return ElementValidatedJson(name, element)
    }

    override fun get(key: String): ValidatedJson<JsonElement> {
        val element: JsonElement
        val name: String

        if (this.value.isJsonObject) {
            element = this.value.asJsonObject.get(key) ?: return this.newError("missing key \"$key\"")
            name = "${this.name}.$key"
        } else if (this.value.isJsonArray) {
            val intKey: Int = key.toIntOrNull() ?: return this.newError("cannot index array with non-integer key \"$key\"")
            val arr = this.value.asJsonArray
            if (intKey < 0 || intKey >= arr.size()) {
                return this.newError("index $intKey out of bounds for array length ${arr.size()}")
            }
            element = arr[intKey]
            name = "${this.name}[$intKey]"
        } else {
            return this.newError("not an indexable type")
        }

        return ElementValidatedJson(name, element)
    }

    override fun elementCount(range: IntRange): ValidatedJson<T> {
        val count = if (this.value.isJsonObject) {
            this.value.asJsonObject.size()
        } else if (this.value.isJsonArray) {
            this.value.asJsonArray.size()
        } else {
            return this.newError("not an indexable type")
        }
        if (count in range) return this
        return this.newError("element count ($count) not in range $range")
    }

    override fun <O> element(
        key: Int,
        validator: ValidatedJson<JsonElement>.() -> ValidatedJson<O>
    ): ValidatedJson<T> {
        val v = validator.invoke(this.get(key))
        val error = v.error ?: return this
        return this.newError("failed to validate ${v.name}: $error")
    }

    override fun <O> element(
        key: String,
        validator: ValidatedJson<JsonElement>.() -> ValidatedJson<O>
    ): ValidatedJson<T> {
        val v = validator.invoke(this.get(key))
        val error = v.error ?: return this
        return this.newError("failed to validate ${v.name}: $error")
    }

    override fun elements(validate: ValidatedJson<JsonElement>.() -> Unit): ValidatedJson<T> {
        val elements = this.elements ?: return this.newError("not an indexable type")
        val errors: MutableSet<String> = LinkedHashSet()

        for ((name, element) in elements) {
            val validator = ElementValidatedJson(name, element)
            validate.invoke(validator)
            val error = validator.error ?: continue
            errors.add(error)
        }

        if (errors.isNotEmpty()) {
            val desc = errors.joinToString(", ")
            return this.newError("one or more children failed to validate: $desc")
        }

        return this
    }

    override fun <O> matchingElement(matches: ValidatedJson<JsonElement>.() -> ValidatedJson<O>): ValidatedJson<O> {
        val elements = this.elements ?: return this.newError("not an indexable type")
        val errors: MutableSet<String> = LinkedHashSet()

        for ((name, element) in elements) {
            val validator = ElementValidatedJson(name, element)
            val outValidator = matches.invoke(validator)
            val error = outValidator.error ?: return outValidator
            errors.add(error)
        }

        if (errors.isEmpty()) {
            return this.newError("no children to match")
        } else {
            val desc = errors.joinToString(", ")
            return this.newError("all children failed to match: $desc")
        }
    }

    override fun valueEquals(value: Any): ValidatedJson<T> {
        if (this.value == value) return this
        return this.newError("value is not equal to $value")
    }

    override fun valueMatches(@Language("RegExp") pattern: String): ValidatedJson<T> {
        val valueString: String = try {
            this.value.asString
        } catch (_: UnsupportedOperationException) {
            return this.newError("value cannot be stringified")
        }
        val matcher = Pattern.compile(pattern).matcher(valueString)
        if (matcher.matches()) return this
        return this.newError("value ($valueString) does not match pattern $pattern")
    }

    private val elements: Iterator<Pair<String, JsonElement>>?
        get() = if (this.value.isJsonObject) {
            this.value.asJsonObject.entrySet()
                .stream()
                .map { "${this.name}.${it.key}" to it.value }
                .iterator()
        } else if (this.value.isJsonArray) {
            val arr = this.value.asJsonArray
            IntStream.range(0, arr.size())
                .mapToObj { "${this.name}[$it]" to arr[it] }
                .iterator()
        } else {
            null
        }

    private fun <O: JsonElement> polymorphElement(fn: (t: T) -> O): ValidatedJson<O> {
        return ElementValidatedJson(this.name, fn.invoke(this.value))
    }

    private fun <O> polymorphPrimitive(fn: (t: T) -> O): ValidatedJson<O> {
        return PrimitiveValidatedJson(this.name, fn.invoke(this.value))
    }

    private fun <O> newError(message: String): ErrorValidatedJson<O> {
        return ErrorValidatedJson(this.name, message)
    }

}