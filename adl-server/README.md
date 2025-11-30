# ADL Server

The **ADL Server** is a Ktor-based microservice that provides a GraphQL API for compiling and formatting ADL (Assistant Description Language) code. It is designed to be used as part of the ARC AI framework, enabling dynamic parsing and transformation of use case definitions written in ADL.

## Features

- **GraphQL API**: Exposes a mutation for compiling ADL code and returning the formatted output.
- **Kotlin & Ktor**: Built with modern Kotlin and Ktor 2.x for high performance and easy extensibility.
- **ARC Integration**: Leverages ARC's use case parsing and formatting utilities.
- **Extensible**: Can be extended with additional GraphQL queries, mutations, or custom logic.

## Usage

### Start the Server

You can start the server using Gradle:

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

```mutation{
 
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


## Environment Variables

- `ADL_SERVER_PORT`: Port for the server (default: 8080)
- `ADL_DEV_MODE`: Enable development mode (default: false)

## Development

- Source code is located in `src/main/kotlin`.
- Main entry point: `org.eclipse.lmos.adl.server.main`
- GraphQL mutation: `AdlCompilerMutation`
