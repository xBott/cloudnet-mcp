package me.bottdev.cloudnetmcp.utils

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull

/**
 * Typed access to a tool call's arguments. The required variants throw [ToolException]
 * when an argument is missing or has the wrong type; the `OrNull` variants return null if missing.
 */
class ToolArgs(private val json: JsonObject) {

    fun string(name: String): String = stringOrNull(name) ?: missing(name)
    fun stringOrNull(name: String): String? =
        primitive(name)?.let { if (it.isString) it.content else wrongType(name, "a string") }

    fun int(name: String): Int = intOrNull(name) ?: missing(name)
    fun intOrNull(name: String): Int? =
        primitive(name)?.let { p -> p.intOrNull.takeIf { !p.isString } ?: wrongType(name, "an integer") }

    fun boolean(name: String): Boolean = booleanOrNull(name) ?: missing(name)
    fun booleanOrNull(name: String): Boolean? =
        primitive(name)?.let { p -> p.booleanOrNull.takeIf { !p.isString } ?: wrongType(name, "a boolean") }

    private fun primitive(name: String): JsonPrimitive? = when (val value = json[name]) {
        null, JsonNull -> null
        is JsonPrimitive -> value
        else -> wrongType(name, "a single value")
    }

    private fun missing(name: String): Nothing = throw ToolException("Missing required argument '$name'")
    private fun wrongType(name: String, expected: String): Nothing =
        throw ToolException("Argument '$name' must be $expected")
}
