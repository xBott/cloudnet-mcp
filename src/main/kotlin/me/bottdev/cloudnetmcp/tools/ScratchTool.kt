package me.bottdev.cloudnetmcp.tools

import io.modelcontextprotocol.kotlin.sdk.server.Server
import me.bottdev.cloudnetmcp.utils.ToolException
import me.bottdev.cloudnetmcp.utils.schema
import me.bottdev.cloudnetmcp.utils.textResult
import me.bottdev.cloudnetmcp.utils.tool

/**
 * Template for a CloudNet tool. To add a real one: copy this file, rename the function and tool,
 * describe the inputs in `schema`, do the CloudNet call in the handler, and register it in
 * `createServer()`.
 *
 * Tips:
 *  - The description is what the AI reads to decide when to call the tool: say what it does and returns.
 *  - Throw [ToolException] for expected failures (e.g. "Service 'Lobby-1' not found").
 */
fun Server.registerScratchTool() {
    tool(
        name = "scratch",
        description = "Placeholder CloudNet tool. Echoes the requested service name; does not contact CloudNet yet.",
        schema = schema {
            string("service", "Name of a CloudNet service, e.g. Lobby-1")
        },
    ) { args ->
        val service = args.string("service")

        textResult("TODO: look up service '$service' in CloudNet")
    }
}
