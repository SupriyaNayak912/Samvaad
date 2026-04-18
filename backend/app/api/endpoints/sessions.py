from fastapi import APIRouter, Depends, HTTPException
from app.core.db import get_db
from app.schemas.schemas import Session, SessionBase, PerformanceMetrics, PerformanceMetricsBase
from typing import List
import uuid
from datetime import datetime

router = APIRouter()

@router.post("/start", response_model=Session, tags=["Sessions"])
async def start_session(session_data: SessionBase, db = Depends(get_db)):
    session_id = str(uuid.uuid4())
    new_session = {
        "id": session_id,
        "user_id": "shambhavi@gmail_com", # Mocked
        "scenario_id": session_data.scenario_id,
        "start_time": datetime.utcnow(),
        "status": "in_progress",
        "score": 0.0,
        "duration_ms": 0
    }
    if db:
        db.collection("user_sessions").document(session_id).set(new_session)
    return new_session

@router.post("/{session_id}/metrics", response_model=PerformanceMetrics, tags=["Stats"])
async def save_metrics(session_id: str, metrics: PerformanceMetricsBase, db = Depends(get_db)):
    metric_id = str(uuid.uuid4())
    metric_dict = metrics.dict()
    metric_dict.update({
        "id": metric_id,
        "session_id": session_id,
        "user_id": "shambhavi@gmail_com"
    })
    if db:
        db.collection("performance_metrics").document(metric_id).set(metric_dict)
        db.collection("user_sessions").document(session_id).update({
            "score": metrics.overall_score,
            "status": "completed",
            "end_time": datetime.utcnow()
        })
    return metric_dict
