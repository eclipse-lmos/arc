package org.eclipse.lmos.adl.server.models


/**
 * Represents the status and details of a connected MCP server.
 *
 * @property url The URL of the MCP server.
 * @property reachable Indicates whether the server is currently reachable and responding.
 * @property toolCount The number of tools available on this server.
 */
data class McpServerDetails(val url: String, val reachable: Boolean, val toolCount: Int)
