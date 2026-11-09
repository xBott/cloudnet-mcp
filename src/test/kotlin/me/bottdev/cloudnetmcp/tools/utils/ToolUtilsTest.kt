package me.bottdev.cloudnetmcp.tools.utils

import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import me.bottdev.cloudnetmcp.utils.ToolArgs
import me.bottdev.cloudnetmcp.utils.ToolException
import me.bottdev.cloudnetmcp.utils.schema
import me.bottdev.cloudnetmcp.utils.textResult
import me.bottdev.cloudnetmcp.utils.tool
import java.io.PipedInputStream
import java.io.PipedOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds

class ToolUtilsTest {

    private val args = ToolArgs(buildJsonObject {
        put("name", "Lobby-1")
        put("count", 3)
        put("force", true)
        put("nothing", JsonNull)
        put("list", buildJsonArray { })
    })

    @Test
    fun `reads typed arguments`() {
        assertEquals("Lobby-1", args.string("name"))
        assertEquals(3, args.int("count"))
        assertEquals(true, args.boolean("force"))
    }

    @Test
    fun `missing or null optional arguments are null`() {
        assertNull(args.stringOrNull("absent"))
        assertNull(args.intOrNull("nothing"))
    }

    @Test
    fun `missing required argument throws ToolException`() {
        val e = assertFailsWith<ToolException> { args.string("absent") }
        assertEquals("Missing required argument 'absent'", e.message)
    }

    @Test
    fun `wrong type throws ToolException`() {
        assertEquals("Argument 'name' must be an integer", assertFailsWith<ToolException> { args.int("name") }.message)
        assertEquals("Argument 'count' must be a string", assertFailsWith<ToolException> { args.string("count") }.message)
        assertEquals("Argument 'list' must be a single value", assertFailsWith<ToolException> { args.string("list") }.message)
        // A quoted number is a string, not an integer.
        val quoted = ToolArgs(buildJsonObject { put("n", "5") })
        assertFailsWith<ToolException> { quoted.int("n") }
    }

    // --- schema ---

    @Test
    fun `schema builder produces properties and required list`() {
        val schema = schema {
            string("service", "Service name")
            string("state", "Lifecycle state", required = false, enum = listOf("RUNNING", "STOPPED"))
            integer("limit", "Max results", required = false)
        }
        val props = schema.properties!!
        assertEquals("string", props["service"]!!.jsonObject["type"]!!.jsonPrimitive.content)
        assertEquals(
            listOf("RUNNING", "STOPPED"),
            props["state"]!!.jsonObject["enum"]!!.jsonArray.map { (it as JsonPrimitive).content },
        )
        assertEquals("integer", props["limit"]!!.jsonObject["type"]!!.jsonPrimitive.content)
        assertEquals(listOf("service"), schema.required)
    }

    @Test
    fun `tool turns exceptions into error results`() = runBlocking {
        val server = Server(
            Implementation("test-server", "0"),
            ServerOptions(capabilities = ServerCapabilities(tools = ServerCapabilities.Tools(listChanged = false))),
        )
        server.tool("needs_arg", "test", schema { string("x", "x") }) { a -> textResult(a.string("x")) }
        server.tool("crashes", "test") { error("boom") }

        val client = Client(Implementation("test-client", "0"))
        try {
            withTimeout(10.seconds) {
                connect(server, client)

                val ok = client.callTool("needs_arg", mapOf("x" to "hi"))
                assertEquals("hi", ok.text())

                val missing = client.callTool("needs_arg", emptyMap())
                assertEquals(true, missing.isError)
                assertEquals("Missing required argument 'x'", missing.text())

                val crash = client.callTool("crashes", emptyMap())
                assertEquals(true, crash.isError)
                assertEquals("Internal error in tool 'crashes': boom", crash.text())
            }
        } finally {
            client.close()
            server.close()
        }
    }

    private fun io.modelcontextprotocol.kotlin.sdk.types.CallToolResult.text() =
        (content.single() as TextContent).text

    private suspend fun connect(server: Server, client: Client) {
        val clientToServer = PipedOutputStream()
        val serverIn = withContext(Dispatchers.IO) {
            PipedInputStream(clientToServer)
        }
        val serverToClient = PipedOutputStream()
        val clientIn = withContext(Dispatchers.IO) {
            PipedInputStream(serverToClient)
        }
        server.createSession(StdioServerTransport(serverIn.asSource().buffered(), serverToClient.asSink().buffered()))
        client.connect(StdioClientTransport(clientIn.asSource().buffered(), clientToServer.asSink().buffered()))
    }

}
