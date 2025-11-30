# Kotlin Code Block Runner

This module provides a CodeBlockRunner implementation that executes Kotlin code blocks using the Kotlin scripting API, along with a CodeBlockProcessor for processing markdown strings containing code blocks.

## Overview

The `arc-kotlin-runner` module contains:
- **KotlinCodeBlockRunner**: Executes Kotlin code blocks using the Kotlin scripting API

## Components

### KotlinCodeBlockRunner

A `CodeBlockRunner` implementation that executes Kotlin code blocks in a sandboxed environment.

**Features:**
- Executes Kotlin code blocks (identified by `kotlin` or `kts` language)
- Configurable execution timeout (default: 10 seconds)
- Captures script output and return values
- Returns formatted execution results
- Handles compilation and runtime errors gracefully

**Security:**
- Execution timeout prevents infinite loops
- Scripts run in isolated evaluation context
- No direct file system or network access by default

**Example:**
```kotlin
val runner = KotlinCodeBlockRunner(timeoutMs = 5000)
val codeBlock = CodeBlock(code = "1 + 1", language = "kotlin")
val result = runner.run(codeBlock).getOrNull() // Returns "2"
```

## Usage

### Basic Usage

```kotlin
import org.eclipse.lmos.arc.kotlin.runner.KotlinCodeBlockRunner
import org.eclipse.lmos.arc.assistants.support.usecases.code.CodeBlock

// Create runner
val runner = KotlinCodeBlockRunner()

// Execute code
val codeBlock = CodeBlock(code = "2 * 3 + 4", language = "kotlin")
val result = runner.run(codeBlock)

// Handle result
when {
    result.isSuccess() -> println("Result: ${result.getOrNull()}")
    result.isFailure() -> println("Error: ${result.exceptionOrNull()?.message}")
}
```

**Output:**
```
Result: 10
```

### Async Usage

```kotlin
import kotlinx.coroutines.runBlocking

runBlocking {
    val codeBlock = CodeBlock(code = "listOf(1,2,3).sum()", language = "kotlin")
    val result = runner.run(codeBlock)
    println("Sum: ${result.getOrNull()}")
}
```

### Handling Different Result Types

## ServiceLoader Integration

The `KotlinCodeBlockRunner` is automatically discovered via Java's ServiceLoader mechanism.

**Service Registration:**
The runner is registered in:
```
META-INF/services/org.eclipse.lmos.arc.assistants.support.usecases.code.CodeBlockRunner
```

**Content:**
```
org.eclipse.lmos.arc.kotlin.runner.KotlinCodeBlockRunner
```

## Dependencies

```kotlin
dependencies {
    implementation(project(":arc-assistants"))
    implementation("org.jetbrains.kotlin:kotlin-scripting-common")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm-host")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.slf4j:slf4j-api")
}
```

## Code Block Format

Code blocks are identified using standard markdown syntax:

````markdown
```kotlin
// Your Kotlin code here
val x = 10
x * 2
```
````

The language identifier can be:
- `kotlin` - Standard Kotlin code
- `kts` - Kotlin script

## Result Type

The runner returns `Result<String?, CodeException>` which wraps the execution result:

**Success cases:**
- Returns the string representation of the result value
- Returns `null` for Unit or no meaningful output

**Failure cases:**
- `ExecutionException` - compilation or runtime errors
- `TimeoutException` - script execution exceeded timeout

**Usage:**
```kotlin
val result = runner.run(codeBlock)

when {
    result.isSuccess() -> {
        val value = result.getOrNull()
        println("Result: $value")
    }
    result.isFailure() -> {
        when (val error = result.exceptionOrNull()) {
            is ExecutionException -> println("Execution failed: ${error.message}")
            is TimeoutException -> println("Timeout: ${error.message}")
        }
    }
}
```

## Exception Types

- **ExecutionException**: Thrown when script compilation or execution fails
- **TimeoutException**: Thrown when script execution exceeds the configured timeout

Both inherit from `CodeException`.

## Return Value Handling

The runner returns a `Result<String?, CodeException>` type with different values:

**Non-null values:**
```kotlin
CodeBlock("1 + 1", "kotlin")
// Result.Success("2")
```

**Unit or null:**
```kotlin
CodeBlock("val x = 5", "kotlin")
// Result.Success(null)
```

**Compilation errors:**
```kotlin
CodeBlock("val x =", "kotlin")  // incomplete
// Result.Failure(ExecutionException("Compilation/Execution failed: ..."))
```

**Timeout:**
```kotlin
CodeBlock("Thread.sleep(20000)", "kotlin")  // exceeds timeout
// Result.Failure(TimeoutException("Script execution timed out after 10000ms"))
```

## Configuration

### Timeout Configuration

```kotlin
// Custom timeout (5 seconds)
val runner = KotlinCodeBlockRunner(timeoutMs = 5000)
```

### Default Configuration

- **Timeout:** 10 seconds (10,000ms)
- **Supported languages:** kotlin, kts
- **ServiceLoader:** Enabled by default

## Examples

### Mathematical Operations

```kotlin
val codeBlock = CodeBlock(code = "(1..10).sum()", language = "kotlin")
val result = runner.run(codeBlock).getOrNull()
// Output: "55"
```

### String Operations

```kotlin
val codeBlock = CodeBlock(
    code = """
        "hello world".split(" ")
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
    """.trimIndent(),
    language = "kotlin"
)
val result = runner.run(codeBlock).getOrNull()
// Output: "Hello World"
```

### Collection Operations

```kotlin
val codeBlock = CodeBlock(
    code = """
        listOf(1, 2, 3, 4, 5)
            .filter { it % 2 == 0 }
            .map { it * 2 }
    """.trimIndent(),
    language = "kotlin"
)
val result = runner.run(codeBlock).getOrNull()
// Output: "[4, 8]"
```

### Data Classes

```kotlin
val codeBlock = CodeBlock(
    code = """
        data class Person(val name: String, val age: Int)
        val person = Person("Alice", 30)
        person.name
    """.trimIndent(),
    language = "kotlin"
)
val result = runner.run(codeBlock).getOrNull()
// Output: "Alice"
```

### Checking if Runner Can Handle Language

```kotlin
val kotlinBlock = CodeBlock(code = "1 + 1", language = "kotlin")
val pythonBlock = CodeBlock(code = "1 + 1", language = "python")

runner.canHandle(kotlinBlock)  // true
runner.canHandle(pythonBlock)  // false
```

## Error Handling

### Compilation Errors

Code that doesn't compile will return a Result.Failure with ExecutionException:

```kotlin
val codeBlock = CodeBlock("val x =", "kotlin")
val result = runner.run(codeBlock)

// result.isFailure() == true
// result.exceptionOrNull() is ExecutionException
```

### Runtime Errors

Runtime exceptions are caught and returned as ExecutionException:

```kotlin
val codeBlock = CodeBlock("val x: String? = null\nx!!.length", "kotlin")
val result = runner.run(codeBlock)

// result.isFailure() == true
// Contains error information in the exception message
```

### Timeout Errors

Long-running scripts that exceed the timeout will return TimeoutException:

```kotlin
val runner = KotlinCodeBlockRunner(timeoutMs = 1000)
val codeBlock = CodeBlock("Thread.sleep(5000)", "kotlin")
val result = runner.run(codeBlock)

// result.isFailure() == true
// result.exceptionOrNull() is TimeoutException
```

### Unsupported Languages

Code blocks with unsupported languages return null:

```kotlin
val codeBlock = CodeBlock("print('hello')", "python")
val result = runner.run(codeBlock)

// result.getOrNull() == null (because canHandle returns false)
```

## Testing

The module includes comprehensive tests:
- **KotlinCodeBlockRunnerTest**: 30+ tests for the runner covering various scenarios including compilation errors, timeouts, and different return types

Run tests:
```bash
./gradlew :arc-kotlin-runner:test
```

## Logging

The module uses SLF4J for logging:

- **DEBUG**: Execution information, runner selection
- **TRACE**: Detailed code block extraction
- **ERROR**: Execution failures

Configure logging level in your application:
```properties
org.eclipse.lmos.arc.kotlin.runner=DEBUG
```

## Performance Considerations

- **ServiceLoader**: Lazily loads runners on first use
- **Timeout**: Prevents long-running scripts from blocking
- **Isolation**: Each script runs in its own evaluation context

## Limitations

1. **No State Persistence**: Each code block execution is independent
2. **No External Dependencies**: Scripts cannot import external libraries
3. **Timeout Required**: Long-running computations will be interrupted
4. **Read-Only**: Scripts should not have side effects
