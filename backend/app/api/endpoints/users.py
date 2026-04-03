from fastapi import APIRouter, Depends, HTTPException
from app.core.db import get_db
from app.schemas.schemas import User
from typing import List

router = APIRouter()

@router.get("/profile", response_model=User, tags=["User"])
async def get_profile(db = Depends(get_db)):
    # Mocking for now, in real app would use current user ID from token
    user_id = "shambhavi@gmail_com" 
    if not db:
        return {"id": user_id, "name": "Shambhavi Patil", "email": "shambhavi@gmail.com", "role": "user", "firebase_uid": "mock", "member_since": "2024-01-01T00:00:00", "created_at": "2024-01-01T00:00:00", "updated_at": "2024-01-01T00:00:00"}
    
    doc = db.collection("users").document(user_id).get()
    if not doc.exists:
        raise HTTPException(status_code=404, detail="User not found")
    
    data = doc.to_dict()
    data["id"] = doc.id
    return data

@router.get("/", response_model=List[User], tags=["User"])
async def get_all_users(db = Depends(get_db)):
    if not db:
        return []
    users = []
    docs = db.collection("users").stream()
    for doc in docs:
        data = doc.to_dict()
        data["id"] = doc.id
        users.append(data)
    return users
