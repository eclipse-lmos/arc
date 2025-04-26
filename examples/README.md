# Arc Examples

This directory contains example applications demonstrating various features and capabilities of the Arc framework.

## Examples

### Main Examples

1. **AgentServer.kt**
   - Demonstrates how to run an Agent server with a web interface
   - Creates a simple weather assistant using GPT-4o
   - Web interface accessible at http://localhost:8080/chat/index.html#/chat

2. **AgentScriptServer.kt**
   - Demonstrates how to run Agents defined in Kotlin script files.
   - Uses hot reloading for agents from the "examples/agents" directory.
   - Web interface accessible at http://localhost:8080/chat/index.html#/chat

3. **McpAgent.kt**
   - Demonstrates how to connect an Agent to tools hosted on an MCP (Model Control Protocol) server.
   - Uses the "getBooks" tool from the MCP server
   - Requires McpApplication to be running

4. **OllamaAgent.kt**
   - Demonstrates how to connect an Agent to the Ollama server (a local LLM server)

5. **OpenAIAgent.kt**
   - Demonstrates how to connect an Agent to the OpenAI API.
   
6. **ContextAgent.kt**
   - Demonstrates how to provide Agents with access to external beans / components.


