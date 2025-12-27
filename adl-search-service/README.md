# ADL Search Service

This service indexes ADLs into Qdrant and returns semantically related ADLs for a conversation.

Run locally (assumes Qdrant running on localhost:6333):

```bash
python -m pip install -r requirements.txt
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

Environment variables:
- `QDRANT_HOST` (default: `localhost`)
- `QDRANT_PORT` (default: `6333`)
- `QDRANT_COLLECTION` (default: `adl_collection`)

Index sample ADLs:

```bash
curl -X POST "http://localhost:8000/index" -H "Content-Type: application/json" \
  -d @sample_adls.json
```

Query example:

```bash
curl -X POST "http://localhost:8000/query" -H "Content-Type: application/json" \
  -d '{"conversation": "I need something that parses PDFs and extracts invoice line items", "top_k": 3}'
```
