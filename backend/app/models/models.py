from sqlmodel import SQLModel, Field, Relationship
from typing import List, Optional
from datetime import datetime

class User(SQLModel, table=True):
    id: str = Field(primary_key=True)
    email: str = Field(index=True, unique=True)
    name: str
    role: str = "Engineering Student"
    member_since: datetime = Field(default_factory=datetime.utcnow)

class Scenario(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    title: str
    prompt_text: str
    category: str
    difficulty: str

class PracticeSession(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    user_id: str = Field(foreign_key="user.id")
    scenario_id: int = Field(foreign_key="scenario.id")
    start_time: datetime = Field(default_factory=datetime.utcnow)
    end_time: Optional[datetime] = None
    final_score: Optional[float] = None

class ActivityLog(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    uid: str = Field(foreign_key="user.id")
    timestamp: datetime = Field(default_factory=datetime.utcnow)
    screen_name: str
    event_type: str
    duration_ms: int
