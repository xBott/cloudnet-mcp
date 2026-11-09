package me.bottdev.cloudnetmcp

import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import me.bottdev.cloudnetmcp.tools.registerScratchTool

/** Builds the MCP server and registers all tools. Transport-agnostic, so tests can reuse it. */
fun createServer(): Server {
    val server = Server(
        serverInfo = Implementation(name = BuildConfig.NAME, version = BuildConfig.VERSION),
        options = ServerOptions(
            capabilities = ServerCapabilities(
                tools = ServerCapabilities.Tools(listChanged = true),
            ),
        ),
    )

    server.registerScratchTool()

    return server
}
