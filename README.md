[![GitHub Actions Build Status](https://github.com/eclipse-lmos/arc/actions/workflows/gradle.yml/badge.svg?branch=main)](https://github.com/eclipse-lmos/arc/actions/workflows/gradle.yml)
[![GitHub Actions Publish Status](https://github.com/eclipse-lmos/arc/actions/workflows/gradle-publish.yml/badge.svg?branch=main)](https://github.com/eclipse-lmos/arc/actions/workflows/gradle-publish.yml)
[![Apache 2.0 License](https://img.shields.io/badge/license-Apache%202.0-green.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Contributor Covenant](https://img.shields.io/badge/Contributor%20Covenant-2.1-4baaaa.svg)](CODE_OF_CONDUCT.md)

# The Arc Project

The goal of the Arc project is to utilize the power of Kotlin DSL to define
a language optimized for building LLM powered AI Agents solutions.

```kotlin

fun main() = runBlocking {
    // Set OpenAI API Key as System Property or Environment Variable.
    // System.setProperty("OPENAI_API_KEY", "****")

    agents {
        // Use the Agent DSL to define your agents.
        agent {
            name = "MyAgent"
            model { "gpt-4o" }
            prompt {
                """
                You are a helpful assistant. Help the user with their questions.
                """
            }
        }
        // Add more agents here
        
    }.serve()
}
```

Check out the examples at https://github.com/eclipse-lmos/arc/tree/main/examples.

Please also take a look at the documentation -> https://eclipse.dev/lmos/arc2

Check out the [Arc Agent Demo Project](https://github.com/eclipse-lmos/arc-spring-init) for 
an example Spring Boot project that uses the Arc Agent Framework.

## Code of Conduct

This project has adopted the [Contributor Covenant](https://www.contributor-covenant.org/) in version 2.1 as our code of conduct. Please see the details in our [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md). All contributors must abide by the code of conduct.

By participating in this project, you agree to abide by its [Code of Conduct](./CODE_OF_CONDUCT.md) at all times.

## Licensing
Copyright (c) 2025 Deutsche Telekom AG and others.

Sourcecode licensed under the [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0) (the "License"); you may not use this project except in compliance with the License.

This project follows the [REUSE standard for software licensing](https://reuse.software/).    
Each file contains copyright and license information, and license texts can be found in the [./LICENSES](./LICENSES) folder. For more information visit https://reuse.software/.   

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the LICENSE for the specific language governing permissions and limitations under the License.
