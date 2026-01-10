# ADL Server

The **ADL Server** is a Ktor-based microservice that provides a GraphQL API for compiling and formatting ADL (Assistant Description Language) code. It is designed to be used as part of the ARC AI framework, enabling dynamic parsing and transformation of use case definitions written in ADL.

## Features

- **GraphQL API**: Exposes a mutation for compiling ADL code and returning the formatted output.
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

### GraphQL Endpoint

The main endpoint is available at:

```
POST http://localhost:8080/graphql
```

#### Example Mutation

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

#### Example Queries

**Find use cases by description:**

```graphql
query {
    searchByText(query: "forgot pass") {
        useCaseId
        score
    }
}
```

**Find use cases by conversation:**

```graphql
query {
    search(conversation: [
        { role: "user", content: "I forgot my password" }
    ]) {
        useCaseId
        score
    }
}
```

**Generate examples:**

```graphql
query {
  examples(description: "password forgotten") {
    useCaseDescription
    examples
  }
}
```

#### Other Mutations

**Store UseCase:**

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

**Delete UseCase:**

```graphql
mutation {
  delete(useCaseId: "password_forgotten") {
    useCaseId
    message
  }
}
```

### Database

The ADL Server requires a Qdrant vector database to store and search for UseCase embeddings. You can start a Qdrant instance using Docker:

```sh
docker run -p 6333:6333 -p 6334:6334 -v $(pwd)/qdrant_storage:/qdrant/storage:z qdrant/qdrant
```


## Development

- Source code is located in `src/main/kotlin`.
- Main entry point: `org.eclipse.lmos.adl.server.main`
- GraphQL mutation: `AdlCompilerMutation`
