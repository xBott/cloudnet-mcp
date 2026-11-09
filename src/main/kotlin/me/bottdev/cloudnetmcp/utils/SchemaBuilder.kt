package me.bottdev.cloudnetmcp.utils

import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Builds a tool input schema: `schema { string("service", "Service name") }`. */
fun schema(block: SchemaBuilder.() -> Unit): ToolSchema = SchemaBuilder().apply(block).build()

class SchemaBuilder internal constructor() {
    private val properties = linkedMapOf<String, JsonObject>()
    private val required = mutableListOf<String>()

    /** A string argument; pass [enum] to restrict it to fixed values. */
    fun string(name: String, description: String, required: Boolean = true, enum: List<String>? = null) =
        property(name, "string", description, required) {
            if (enum != null) put("enum", JsonArray(enum.map(::JsonPrimitive)))
        }

    fun integer(name: String, description: String, required: Boolean = true) =
        property(name, "integer", description, required)

    fun boolean(name: String, description: String, required: Boolean = true) =
        property(name, "boolean", description, required)

    private fun property(
        name: String,
        type: String,
        description: String,
        required: Boolean,
        extra: JsonObjectBuilder.() -> Unit = {},
    ) {
        properties[name] = buildJsonObject {
            put("type", type)
            put("description", description)
            extra()
        }
        if (required) this.required += name
    }

    internal fun build() = ToolSchema(properties = JsonObject(properties), required = required)
}
