from fastapi import FastAPI
from app.api.endpoints import scenarios, auth, sessions, users
from app.core.db import initialize_firebase

app = FastAPI(title="SAMVAAD API", version="1.0.0")

# Initialize Cloud Database (Firestore)
@app.on_event("startup")
def on_startup():
    initialize_firebase()

# Registering Professional Modular Routes
app.include_router(auth.router, prefix="/api/auth", tags=["Auth"])
app.include_router(scenarios.router, prefix="/api/scenarios", tags=["Scenarios"])
app.include_router(sessions.router, prefix="/api/sessions", tags=["Sessions"])
app.include_router(users.router, prefix="/api/users", tags=["User"])

@app.get("/")
async def root():
    return {"message": "Welcome to SAMVAAD Professional API"}
