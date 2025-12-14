# ADL Search Server

The ADL Search Server is a microservice that provides search capabilities for ADL content.
It exposes a GraphQL API for querying and retrieving ADL documents based on conversation context.

## Features

- **UseCase Embeddings**: Creates and stores embeddings from UseCase examples for semantic search
- **Conversation Embeddings**: Generates embeddings from conversation messages
- **Vector Search**: Uses Qdrant vector database for similarity search
- **GraphQL API**: Exposes search functionality via GraphQL

## Prerequisites

### Qdrant Vector Database

The server requires a running Qdrant instance for storing and searching embeddings.

#### Start Qdrant with Docker

```bash
# Pull and run Qdrant
docker run -d \
  --name qdrant \
  -p 6333:6333 \
  -p 6334:6334 \
  -v qdrant_storage:/qdrant/storage \
  qdrant/qdrant
```

#### Start Qdrant with Docker Compose

Create a `docker-compose.yml` file:

```yaml
version: '3.8'
services:
  qdrant:
    image: qdrant/qdrant
    container_name: qdrant
    ports:
      - "6333:6333"  # REST API
      - "6334:6334"  # gRPC API
    volumes:
      - qdrant_storage:/qdrant/storage
    environment:
      - QDRANT__SERVICE__GRPC_PORT=6334

volumes:
  qdrant_storage:
```

Then run:

```bash
docker-compose up -d
```

#### Verify Qdrant is Running

```bash
# Check Qdrant health via REST API
curl http://localhost:6333/health
```

## Configuration

The server can be configured using environment variables:

| Variable | Description | Default |
|----------|-------------|---------|
| `ADL_SERVER_PORT` | Server port | `8080` |
| `QDRANT_HOST` | Qdrant host | `localhost` |
| `QDRANT_PORT` | Qdrant gRPC port | `6334` |
| `QDRANT_COLLECTION_NAME` | Collection name for embeddings | `usecase_embeddings` |
| `QDRANT_VECTOR_SIZE` | Vector dimension size | `384` |
| `ARC_DEV_MODE` | Enable development mode | `false` |

## Running the Server

```bash
# Start the server
./gradlew :adl-search-server:run

# Or with custom configuration
QDRANT_HOST=localhost QDRANT_PORT=6334 ./gradlew :adl-search-server:run
```

## Embedding Components

### QdrantUseCaseEmbeddingsStore

Stores UseCase embeddings in Qdrant for semantic search:

```kotlin
val embeddingModel = AllMiniLmL6V2EmbeddingModel()
val store = QdrantUseCaseEmbeddingsStore(embeddingModel)

// Initialize collection
store.initialize()

// Store UseCases
store.storeUseCases(useCases)

// Search for similar UseCases
val results = store.search("user query", limit = 5)
```

### MessageEmbedder

Creates embeddings from conversation messages:

```kotlin
val embedder = Langchain4jMessageEmbedder(embeddingModel)
val embeddings = embedder.embed(messages)
```

### ConversationEmbedder

Creates a single embedding for an entire conversation:

```kotlin
val embedder = ConversationEmbedder(embeddingModel, RolePrefixedStrategy())
val embedding = embedder.embed(messages)
```

## GraphQL API

Access the GraphQL playground at: `http://localhost:8080/graphiql`

### Schema Overview

#### Queries

| Query | Description |
|-------|-------------|
| `version` | Returns the supported ADL version |
| `search` | Searches UseCases based on conversation context |
| `searchByText` | Searches UseCases using a text query |

#### Mutations

| Mutation | Description |
|----------|-------------|
| `store` | Stores a UseCase in the embeddings store |
| `delete` | Deletes a UseCase by ID |
| `clearAll` | Clears all UseCases from the store |

### Example Queries

#### Get API Version

```graphql
query {
  version
}
```

**Response:**
```json
{
  "data": {
    "version": "1.0.0"
  }
}
```

#### Search by Conversation

Search for matching UseCases based on a conversation history:

```graphql
query SearchByConversation($conversation: [MessageInput!]!, $limit: Int, $scoreThreshold: Float) {
  search(conversation: $conversation, limit: $limit, scoreThreshold: $scoreThreshold) {
    useCaseId
    score
    matchedExamples
  }
}
```

**Variables:**
```json
{
  "conversation": [
    { "role": "user", "content": "I need help with my internet connection" },
    { "role": "assistant", "content": "I can help you with that. What seems to be the problem?" },
    { "role": "user", "content": "It's very slow today" }
  ],
  "limit": 5,
  "scoreThreshold": 0.5
}
```

**Response:**
```json
{
  "data": {
    "search": [
      {
        "useCaseId": "internet-troubleshooting",
        "score": 0.87,
        "matchedExamples": ["Help me fix my slow internet", "My connection is slow"]
      },
      {
        "useCaseId": "network-diagnostics",
        "score": 0.72,
        "matchedExamples": ["Run network diagnostics"]
      }
    ]
  }
}
```

#### Search by Text Query

Search for UseCases using a simple text query:

```graphql
query SearchByText($query: String!, $limit: Int, $scoreThreshold: Float) {
  searchByText(query: $query, limit: $limit, scoreThreshold: $scoreThreshold) {
    useCaseId
    score
    matchedExamples
  }
}
```

**Variables:**
```json
{
  "query": "billing information",
  "limit": 10,
  "scoreThreshold": 0.3
}
```

**Response:**
```json
{
  "data": {
    "searchByText": [
      {
        "useCaseId": "billing-inquiry",
        "score": 0.91,
        "matchedExamples": ["Show me my billing information", "What's my current bill?"]
      }
    ]
  }
}
```

### Example Mutations

#### Store a UseCase

Store a UseCase with its examples for semantic search:

```graphql
mutation StoreUseCase($adl: String!) {
  store(adl: $adl) {
    success
    storedExamplesCount
    message
  }
}
```

**Variables:**
```json
{
  "adl": "useCase: billing-inquiry\nexamples:\n  - Show me my billing information\n  - What's my current bill?\n  - I want to see my invoices"
}
```

**Response:**
```json
{
  "data": {
    "store": {
      "success": true,
      "storedExamplesCount": 3,
      "message": "UseCase successfully stored with 3 embeddings"
    }
  }
}
```

#### Delete a UseCase

Delete a specific UseCase by its ID:

```graphql
mutation DeleteUseCase($useCaseId: String!) {
  delete(useCaseId: $useCaseId) {
    success
    useCaseId
    message
  }
}
```

**Variables:**
```json
{
  "useCaseId": "billing-inquiry"
}
```

**Response:**
```json
{
  "data": {
    "delete": {
      "success": true,
      "useCaseId": "billing-inquiry",
      "message": "UseCase successfully deleted"
    }
  }
}
```

#### Clear All UseCases

Remove all UseCases from the embeddings store:

```graphql
mutation {
  clearAll {
    success
    message
  }
}
```

**Response:**
```json
{
  "data": {
    "clearAll": {
      "success": true,
      "message": "All UseCases successfully cleared"
    }
  }
}
```

### Using cURL

You can also interact with the GraphQL API using cURL:

```bash
# Get version
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "{ version }"}'

# Search by text
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "query($query: String!) { searchByText(query: $query, limit: 5) { useCaseId score matchedExamples } }",
    "variables": { "query": "billing help" }
  }'

# Store a UseCase
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "mutation($adl: String!) { store(adl: $adl) { success storedExamplesCount message } }",
    "variables": { "adl": "useCase: example\nexamples:\n  - Example query" }
  }'
```

