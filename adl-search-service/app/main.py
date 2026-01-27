from typing import List, Optional
import os
import json

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer
from qdrant_client import QdrantClient
from qdrant_client.http.models import Distance, VectorParams

QDRANT_HOST = os.getenv("QDRANT_HOST", "localhost")
QDRANT_PORT = int(os.getenv("QDRANT_PORT", "6333"))
COLLECTION_NAME = os.getenv("QDRANT_COLLECTION", "adl_collection")

app = FastAPI(title="ADL Search Service")


class ADL(BaseModel):
    id: str
    title: Optional[str]
    content: str
    metadata: Optional[dict] = None


class ConversationRequest(BaseModel):
    conversation: str
    top_k: Optional[int] = 5


def get_model():
    return SentenceTransformer("all-MiniLM-L6-v2")


def get_qdrant_client():
    return QdrantClient(host=QDRANT_HOST, port=QDRANT_PORT)


@app.on_event("startup")
def startup_event():
    global model, qdrant
    model = get_model()
    qdrant = get_qdrant_client()
    # Ensure collection exists
    try:
        if COLLECTION_NAME not in [c.name for c in qdrant.get_collections().result]:
            vector_size = model.get_sentence_embedding_dimension()
            qdrant.recreate_collection(
                collection_name=COLLECTION_NAME,
                vectors_config=VectorParams(size=vector_size, distance=Distance.COSINE),
            )
    except Exception:
        # older qdrant-client may return different types, fallback to simple create
        vector_size = model.get_sentence_embedding_dimension()
        try:
            qdrant.create_collection(
                collection_name=COLLECTION_NAME,
                vectors_config=VectorParams(size=vector_size, distance=Distance.COSINE),
            )
        except Exception:
            pass


@app.post("/index")
def index_adls(adls: List[ADL]):
    if not adls:
        raise HTTPException(status_code=400, detail="No ADLs provided")
    texts = [a.content for a in adls]
    ids = [a.id for a in adls]
    embeddings = model.encode(texts, show_progress_bar=False).tolist()
    points = [
        {"id": ids[i], "vector": embeddings[i], "payload": {"title": adls[i].title, "metadata": adls[i].metadata}}
        for i in range(len(adls))
    ]
    qdrant.upsert(collection_name=COLLECTION_NAME, points=points)
    return {"indexed": len(points)}


@app.post("/query")
def query_adls(req: ConversationRequest):
    if not req.conversation:
        raise HTTPException(status_code=400, detail="Conversation text required")
    q_emb = model.encode([req.conversation])[0].tolist()
    search_result = qdrant.search(collection_name=COLLECTION_NAME, query_vector=q_emb, limit=req.top_k)
    results = []
    for r in search_result:
        results.append({
            "id": r.id,
            "score": r.score,
            "payload": r.payload,
        })
    return {"results": results}


@app.get("/health")
def health():
    return {"status": "ok"}


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=8000)
