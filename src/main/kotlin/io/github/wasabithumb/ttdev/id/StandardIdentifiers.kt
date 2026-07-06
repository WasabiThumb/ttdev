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

object StandardIdentifiers {

    /** ``net.minecraft:client`` */
    val MINECRAFT_CLIENT = Identifier.of("com.mojang.minecraft",           "client"            )

    /** ``net.minecraft:server`` */
    val MINECRAFT_SERVER = Identifier.of("com.mojang.minecraft",           "server"            )

    /** ``net.fabricmc:fabric-loader`` */
    val FABRIC_LOADER    = Identifier.of("net.fabricmc",                   "fabric-loader"     )

    /** ``net.fabricmc.fabric-api:fabric-api`` */
    val FABRIC_API       = Identifier.of("net.fabricmc.fabric-api",        "fabric-api"        )

    /** ``org.quiltmc:quilt-loader */
    val QUILT_LOADER     = Identifier.of("org.quiltmc",                    "quilt-loader"      )

    /** ``org.quiltmc.quilted-fabric-api:quilted-fabric-api`` */
    val QUILT_API        = Identifier.of("org.quiltmc.quilted-fabric-api", "quilted-fabric-api")

    /** ``net.neoforged:neoforge`` */
    val NEOFORGE         = Identifier.of("net.neoforged",                  "neoforge"          )

    /** ``net.minecraftforge:forge`` */
    val FORGE            = Identifier.of("net.minecraftforge",             "forge"             )

    /** ``org.spongepowered:spongeapi`` */
    val SPONGE_API       = Identifier.of("org.spongepowered",              "spongeapi"         )

    /** ``org.spongepowered:spongeforge`` */
    val SPONGEFORGE      = Identifier.of("org.spongepowered",              "spongeforge"       )

}