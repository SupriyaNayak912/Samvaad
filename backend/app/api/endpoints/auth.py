from fastapi import APIRouter, HTTPException
from app.schemas.schemas import UserCreate, Token

router = APIRouter()

@router.post("/register", tags=["Auth"])
async def register(user: UserCreate):
    return {"message": "User registered successfully", "user": user.email}

@router.post("/login", tags=["Auth"])
async def login(credentials: dict):
    return {"access_token": "mock_token_123", "token_type": "bearer"}

@router.post("/google-login", tags=["Auth"])
async def google_login(data: dict):
    return {"access_token": "google_mock_token", "token_type": "bearer"}
