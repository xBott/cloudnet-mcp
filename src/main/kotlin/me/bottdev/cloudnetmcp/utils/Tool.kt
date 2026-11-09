package me.bottdev.cloudnetmcp.utils

import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonObject
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("cloudnet-mcp.tools")

/**
 * Registers a tool. The handler reads arguments through [ToolArgs] and returns a result;
 * it can throw [ToolException] for expected failures (bad input, service not found, ...).
 * Any other exception is logged and reported to the client as an internal error.
 */
fun Server.tool(
    name: String,
    description: String,
    schema: ToolSchema = schema {},
    handler: suspend (args: ToolArgs) -> CallToolResult,
) {
    addTool(name = name, description = description, inputSchema = schema) { request ->
        try {
            handler(ToolArgs(request.arguments ?: JsonObject(emptyMap())))
        } catch (e: CancellationException) {
            throw e
        } catch (e: ToolException) {
            errorResult(e.message.orEmpty())
        } catch (e: Exception) {
            logger.error("Tool '{}' failed", name, e)
            errorResult("Internal error in tool '$name': ${e.message ?: e::class.simpleName}")
        }
    }
}
