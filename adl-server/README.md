<!--
SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others

SPDX-License-Identifier: CC-BY-4.0
-->
# ADL Server

The **ADL Server** is a Ktor-based microservice that provides a GraphQL API for compiling and formatting ADL (Assistant Description Language) code. It is designed to be used as part of the ARC AI framework, enabling dynamic parsing and transformation of use case definitions written in ADL.

## Features

- **GraphQL API**: Exposes mutations for compiling and validating ADL code.
- **Validation**: Validates ADL files and returns syntax errors, used tools, and references.
- **Kotlin & Ktor**: Built with modern Kotlin and Ktor 2.x for high performance and easy extensibility.
- **ARC Integration**: Leverages ARC's use case parsing and formatting utilities.
- **Extensible**: Can be extended with additional GraphQL queries, mutations, or custom logic.

## Usage

### Configuration

The server can be configured using the following environment variables:

| Variable | Description | Default Value |
| --- | --- | --- |
| `ADL_SERVER_PORT` | The port on which the server should listen for incoming connections. | `8080` |
| `ADL_DEV_MODE` | Indicates whether the server is running in development mode. | `false` |
| `QDRANT_HOST` | Qdrant vector database host. | `localhost` |
| `QDRANT_PORT` | Qdrant vector database port. | `6334` |
| `QDRANT_COLLECTION_NAME` | Qdrant collection name for UseCase embeddings. | `usecase_embeddings` |

### Start the Server

```sh
./gradlew :adl-server:run
```

By default, the server listens on port `8080`. You can override the port by setting the environment variable `ADL_SERVER_PORT`.

### Docker

You can also run the server using Docker.

#### Build the Docker Image

Run the following command from the root of the repository:

```sh
docker build -f adl-server/Dockerfile -t adl-server .
```

#### Run the Docker Container

```sh
docker run -p 8080:8080 -e OPENAI_API_KEY=your-api-key adl-server
```

### GraphQL Endpoint

The main endpoint is available at:

```
POST http://localhost:8080/graphql
```

You can explore the API using GraphiQL at:
`http://localhost:8080/graphiql` (when running in dev mode or if enabled)

#### 1. Core ADL Operations

**Compile ADL (`compile`)**
Compiles ADL code into a structured format.

```graphql
mutation{
  compile(
    conditionals: ["isMonday"]
    adl: """
    ### UseCase: password_forgotten
    #### Description
    The user has forgotten their password.

    #### Solution
    Kindly ask the customer to reset their computer.

    <isMonday> Talk to Bob.

    ```kotlin
    "The current time is: ${time()}"
    ```

    ----

    """){
     compiledOutput
  }
}
```

**Validate ADL (`validate`)**
Checks ADL code for syntax errors, missing references, and extracts used tools.

```graphql
mutation{
  validate(
    adl: """
    ### UseCase: password_forgotten
    #### Description
    The user has forgotten their password.

    #### Solution 
    Call @reset_password() and go to use case #user_verification.
    
    ----
    """
  ) {
    syntaxErrors {
      line
      message
    }
    usedTools
    references
  }
}
```

#### 2. Use Case Management (Knowledge Base)

These operations require Qdrant to be running.

**Store Use Case (`store`)**
Embeds and stores a use case in the vector database.

```graphql
mutation {
  store(adl: """
    ### UseCase: password_forgotten
    # ...
  """) {
    storedExamplesCount
    message
  }
}
```

**Delete Use Case (`delete`)**
Removes a use case from the database.

```graphql
mutation {
  delete(useCaseId: "password_forgotten") {
    useCaseId
    message
  }
}
```

**Search Use Cases (`search`, `searchByText`)**

```graphql
# Find by text description
query {
    searchByText(query: "forgot pass") {
        useCaseId
        score
    }
}

# Find by conversation context (semantic search)
query {
    search(conversation: [
        { role: "user", content: "I forgot my password" }
    ]) {
        useCaseId
        score
    }
}
```

#### 3. Assistant & Simulation

Interact with the ADL assistant capabilities directly.

**Chat with Assistant (`assistant`)**
Send a message to the assistant, providing the ADL context dynamically.

```graphql
mutation {
  assistant(
    input: {
       useCases: """
         ### UseCase: hello
         #### Solution
         Say hello back.
         ----
       """
       request: {
          messages: [{role: "user", content: "Hello"}]
          userContext: { userId: "user-1" }
          conversationContext: { conversationId: "conv-1" }
       }
    }
  ) {
    messages { role content }
    toolCalls { name arguments }
    responseTime
  }
}
```

**Generate System Prompt (`systemPrompt`)**
Get the raw system prompt that would be sent to the LLM.

```graphql
mutation {
  systemPrompt(
    adl: "...",
    conditionals: ["isVip"],
    sessionId: "session-1"
  ) {
    systemPrompt
    useCaseCount
  }
}
```

#### 4. Development & Testing Tools

**Generate Test Cases (`createTests`)**
Create synthetic test conversations from a use case description.

```graphql
mutation {
  createTests(useCase: "The user needs to verify their email address...") {
    title
    description
    expectedConversation { role content }
  }
}
```

**Evaluate Conversation (`eval`)**
Check if a conversation follows the use case rules.

```graphql
mutation {
  eval(input: {
    useCase: """
      ### UseCase: verify_email
      #### Solution
      Ask for email.
      ----
    """
    conversation: "User: verify me. Assistant: OK."
  }) {
    verdict
    score
    reasons
    violations
  }
}
```

**Generate Examples (`examples`)**
Generate example user utterances for a use case description.

```graphql
query {
  examples(description: "password forgotten") {
    useCaseDescription
    examples
  }
}
```

### Database

The ADL Server requires a Qdrant vector database to store and search for UseCase embeddings. You can start a Qdrant instance using Docker:

```sh
docker run -p 6333:6333 -p 6334:6334 -v $(pwd)/qdrant_storage:/qdrant/storage:z qdrant/qdrant
```


## Testing

### Quick Test (One Command)

**From project root (Windows):**
```powershell
.\test-adl-validation.ps1
```

**From adl-server directory (Windows):**
```powershell
cd adl-server
.\quick-test.ps1
```

**From adl-server directory (Linux/Mac):**
```bash
cd adl-server
chmod +x test-all.sh
./test-all.sh
```

**Note:** If you encounter JVM target compatibility issues, use the quick test:
```powershell
cd adl-server
.\quick-test.ps1
```

This will:
1. Run unit tests
2. Build the application
3. Start the server
4. Run integration tests
5. Stop the server

### Unit Tests Only
```bash
./gradlew :adl-server:test
```

### Manual Testing

See [TESTING.md](TESTING.md) for detailed testing instructions including:
- GraphiQL interface
- cURL examples
- Test scripts

## Development

- Source code is located in `src/main/kotlin`.
- Test code is located in `src/test/kotlin`.
- Main entry point: `org.eclipse.lmos.adl.server.main`
- GraphQL mutations: `AdlCompilerMutation`, `AdlValidationMutation`
