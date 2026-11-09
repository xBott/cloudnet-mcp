package me.bottdev.cloudnetmcp.utils

import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent

fun textResult(text: String) = CallToolResult(content = listOf(TextContent(text)))

fun errorResult(message: String) = CallToolResult(content = listOf(TextContent(message)), isError = true)
