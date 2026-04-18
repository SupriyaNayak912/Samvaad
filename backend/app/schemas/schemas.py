from pydantic import BaseModel, EmailStr, HttpUrl
from typing import List, Optional
from datetime import datetime

# --- User Schemas ---
class UserBase(BaseModel):
    email: EmailStr
    name: str
    role: str = "user"
    avatar_url: Optional[str] = None
    bio: Optional[str] = None

class UserCreate(UserBase):
    password: str  # To be hashed as password_hash

class User(UserBase):
    id: str  # Firestore Document ID
    firebase_uid: str
    member_since: datetime
    total_sessions: int = 0
    practice_streak: int = 0
    best_score: float = 0.0
    created_at: datetime
    updated_at: datetime

    class Config:
        from_attributes = True

# --- Scenario Schemas ---
class ScenarioBase(BaseModel):
    title: str
    description: str
    category: str
    difficulty: str
    duration_seconds: Optional[int] = 0
    tips: Optional[str] = None
    audio_url: Optional[str] = None

class Scenario(ScenarioBase):
    id: str
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None

    class Config:
        from_attributes = True

# --- Session Schemas ---
class SessionBase(BaseModel):
    scenario_id: str

class Session(SessionBase):
    id: str
    user_id: str
    start_time: datetime
    end_time: Optional[datetime] = None
    duration_ms: int = 0
    score: float = 0.0
    feedback: Optional[str] = None
    recording_url: Optional[str] = None
    status: str = "in_progress"

    class Config:
        from_attributes = True

# --- Activity Log Schemas ---
class ActivityLogCreate(BaseModel):
    screen_name: str
    event_type: str
    duration_ms: int
    metadata: Optional[str] = None

class ActivityLog(ActivityLogCreate):
    id: str
    user_id: str
    firebase_uid: str
    timestamp: datetime

    class Config:
        from_attributes = True

# --- Performance Metrics Schemas ---
class PerformanceMetricsBase(BaseModel):
    clarity_score: float
    confidence_score: float
    fluency_score: float
    pace_score: float
    overall_score: float
    comments: Optional[str] = None

class PerformanceMetrics(PerformanceMetricsBase):
    id: str
    user_id: str
    session_id: str

    class Config:
        from_attributes = True

# --- Utility Schemas ---
class Token(BaseModel):
    access_token: str
    token_type: str

class PerformanceStats(BaseModel):
    avg_score: float
    sessions_completed: int
    practice_streak: int
