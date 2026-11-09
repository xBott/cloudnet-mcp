package me.bottdev.cloudnetmcp

import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import java.io.PipedInputStream
import java.io.PipedOutputStream
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/** End-to-end tests: a real MCP client talks to the server over in-process stdio pipes. */
class CloudNetMcpServerTest {

    private val client = Client(Implementation(name = "test-client", version = "1.0"))
    private val server = createServer()

    private suspend fun connect() {
        val clientToServer = PipedOutputStream()
        val serverIn = PipedInputStream(clientToServer)
        val serverToClient = PipedOutputStream()
        val clientIn = PipedInputStream(serverToClient)

        server.createSession(
            StdioServerTransport(serverIn.asSource().buffered(), serverToClient.asSink().buffered()),
        )
        client.connect(
            StdioClientTransport(clientIn.asSource().buffered(), clientToServer.asSink().buffered()),
        )
    }

    @AfterTest
    fun tearDown() = runBlocking {
        client.close()
        server.close()
    }

    @Test
    fun `server reports name and version from the build`() = runBlocking {
        withTimeout(10.seconds) {
            connect()
            assertEquals(BuildConfig.NAME, client.serverVersion?.name)
            assertEquals(BuildConfig.VERSION, client.serverVersion?.version)
        }
    }

    @Test
    fun `scratch tool is listed and answers`() = runBlocking {
        withTimeout(10.seconds) {
            connect()

            val tools = client.listTools().tools.map { it.name }
            assertTrue("scratch" in tools, "Expected scratch in $tools")

            val result = client.callTool(name = "scratch", arguments = mapOf("service" to "Lobby-1"))
            assertEquals(false, result.isError ?: false)
            assertTrue("Lobby-1" in (result.content.single() as TextContent).text)
        }
    }
}
