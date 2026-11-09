package me.bottdev.cloudnetmcp.utils

/** Throw from a tool handler to send [message] back to the client as an error result. */
class ToolException(message: String) : Exception(message)
