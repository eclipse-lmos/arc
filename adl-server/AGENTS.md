<!--
SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others

SPDX-License-Identifier: CC-BY-4.0
-->
# ADL Server

The **ADL Server** is a Ktor-based microservice that provides a GraphQL API for compiling and formatting ADL (Assistant Description Language) code. It is designed to be used as part of the ARC AI framework, enabling dynamic parsing and transformation of use case definitions written in ADL.
The architecture of this module follows the SOLID principles to ensure maintainability, scalability, and testability.


## Coding Guidelines 
- Every file MUST contain the SPDX license identifier at the top.
- Clean Code principles should be followed.
- Each class MUST contain a KDoc comment explaining its purpose.
- Each class MUST have unit tests covering at least 80% of its functionality.

### SOLID Principles

*   **Single Responsibility Principle (SRP)**: Each class should have a single responsibility. For example, `TestExecutor` is responsible solely for executing tests, while `ConversationEvaluator` focuses on evaluating conversation outcomes.
*   **Open/Closed Principle (OCP)**: Classes should be open for extension but closed for modification. strategies and listeners can be used to extend behavior without altering core logic.
*   **Liskov Substitution Principle (LSP)**: Subtypes must be substitutable for their base types.
*   **Interface Segregation Principle (ISP)**: Clients should not be forced to depend on interfaces they do not use. Keep interfaces small and focused.
*   **Dependency Inversion Principle (DIP)**: Depend on abstractions, not concretions. Use dependency injection to provide dependencies.
