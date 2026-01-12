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

#### Example: Compile Mutation

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

#### Example: Validate Mutation

```mutation{
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

The validation mutation returns:
- **syntaxErrors**: List of syntax errors found in the ADL code (with line numbers and messages)
- **usedTools**: List of tools/functions used in the ADL code (extracted from `@function()` calls)
- **references**: List of references to other use cases (extracted from `#usecase_id` patterns)


## Environment Variables

- `ADL_SERVER_PORT`: Port for the server (default: 8080)
- `ADL_DEV_MODE`: Enable development mode (default: false)

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
