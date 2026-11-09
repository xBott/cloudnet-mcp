package me.bottdev.cloudnetmcp

import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import io.modelcontextprotocol.kotlin.sdk.server.mcpStreamableHttp
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

private const val USAGE = """Usage:
  cloudnet-mcp                                   Serve MCP over stdio (default)
  cloudnet-mcp --http [--host H] [--port P]      Serve MCP over Streamable HTTP at http://H:P/mcp
               [--allowed-host NAME]...          (defaults: host 127.0.0.1, port 3000)

  --allowed-host  Extra hostname/IP clients may use to reach the server (repeatable).
                  Requests whose Host header is not localhost or an allowed host get 403
                  (DNS-rebinding protection). Needed when clients connect by LAN IP or domain."""

/**
 * Entry point. Picks the transport from the command line:
 *  - stdio: the client launches this process and talks over stdin/stdout (Claude Code, Claude Desktop, IDEs).
 *  - http:  a long-running server that clients connect to by URL (remote access, several clients at once).
 */
fun main(args: Array<String>) {
    // kotlin-logging (used by the SDK) prints a banner to stdout on first use, which would
    // corrupt the stdio protocol stream. Must be set before any SDK class is touched.
    System.setProperty("kotlin-logging.logStartupMessage", "false")

    when (val mode = parseArgs(args)) {
        is Mode.Stdio -> runBlocking { runStdioServer() }
        is Mode.Http -> runHttpServer(mode)
    }
}

private sealed interface Mode {
    data object Stdio : Mode
    data class Http(val host: String, val port: Int, val allowedHosts: List<String>) : Mode
}

private fun parseArgs(args: Array<String>): Mode {
    var http = false
    var host = "127.0.0.1"
    var port = 3000
    val allowedHosts = mutableListOf<String>()
    val it = args.iterator()
    while (it.hasNext()) {
        when (val arg = it.next()) {
            "--http" -> http = true
            "--host" -> host = if (it.hasNext()) it.next() else usageError("--host needs a value")
            "--port" -> port = (if (it.hasNext()) it.next() else null)?.toIntOrNull()
                ?: usageError("--port needs a number")
            "--allowed-host" -> allowedHosts += if (it.hasNext()) it.next() else usageError("--allowed-host needs a value")
            "-h", "--help" -> { System.err.println(USAGE); exitProcess(0) }
            else -> usageError("Unknown argument: $arg")
        }
    }
    return if (http) Mode.Http(host, port, allowedHosts) else Mode.Stdio
}

private fun usageError(message: String): Nothing {
    System.err.println("$message\n\n$USAGE")
    exitProcess(2)
}

/** Never write to stdout in stdio mode: it carries the JSON-RPC protocol. Log via SLF4J (stderr). */
private suspend fun runStdioServer() {
    val logger = LoggerFactory.getLogger("cloudnet-mcp")
    val server = createServer()
    val transport = StdioServerTransport(
        input = System.`in`.asSource().buffered(),
        output = System.out.asSink().buffered(),
    )

    val session = server.createSession(transport)
    logger.info("{} {} (MCP SDK {}) started on stdio", BuildConfig.NAME, BuildConfig.VERSION, BuildConfig.MCP_SDK_VERSION)

    val closed = Job()
    session.onClose { closed.complete() }
    closed.join()
    logger.info("cloudnet-mcp stopped")
}

/** Streamable HTTP transport. Each client session gets its own [createServer] instance. */
private fun runHttpServer(mode: Mode.Http) {
    val logger = LoggerFactory.getLogger("cloudnet-mcp")
    logger.info(
        "{} {} (MCP SDK {}) listening on http://{}:{}/mcp",
        BuildConfig.NAME, BuildConfig.VERSION, BuildConfig.MCP_SDK_VERSION, mode.host, mode.port,
    )
    if (mode.host != "127.0.0.1" && mode.host != "localhost") {
        logger.warn("Listening on {} with no authentication: anyone who can reach this port can call every tool", mode.host)
    }
    embeddedServer(CIO, host = mode.host, port = mode.port) {
        mcpStreamableHttp(
            path = "/mcp",
            allowedHosts = listOf("localhost", "127.0.0.1", "[::1]") + mode.allowedHosts,
        ) {
            createServer()
        }
    }.start(wait = true)
}
