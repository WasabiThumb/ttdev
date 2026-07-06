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

import com.google.gson.Gson
import com.google.gson.JsonElement
import java.io.InputStreamReader
import java.net.http.HttpResponse

object JsonBodyHandler : HttpResponse.BodyHandler<JsonElement> {

    private val gson = Gson()

    //

    override fun apply(responseInfo: HttpResponse.ResponseInfo): HttpResponse.BodySubscriber<JsonElement> {
        return HttpResponse.BodySubscribers.mapping(
            HttpResponse.BodySubscribers.ofInputStream()
        ) { stream ->
            InputStreamReader(stream, Charsets.UTF_8).use { reader ->
                this.gson.fromJson(reader, JsonElement::class.java)
            }
        }
    }

}