from fastapi import APIRouter, Depends, HTTPException
from typing import List
from app.core.db import get_db
from app.schemas.schemas import Scenario as ScenarioSchema

router = APIRouter()

@router.get("/", response_model=List[ScenarioSchema])
def read_scenarios(db = Depends(get_db)):
    scenarios_ref = db.collection("scenarios")
    docs = scenarios_ref.stream()
    
    scenarios = []
    for doc in docs:
        data = doc.to_dict()
        data["id"] = doc.id # Map Firestore doc ID to our schema id field
        scenarios.append(data)
    
    return scenarios

@router.get("/{scenario_id}", response_model=ScenarioSchema)
def read_scenario(scenario_id: str, db = Depends(get_db)):
    doc_ref = db.collection("scenarios").document(scenario_id)
    doc = doc_ref.get()
    
    if not doc.exists:
        raise HTTPException(status_code=404, detail="Scenario not found")
    
    data = doc.to_dict()
    data["id"] = doc.id
    return data
