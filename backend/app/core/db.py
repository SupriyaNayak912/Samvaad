import firebase_admin
from firebase_admin import credentials, firestore
import os
from dotenv import load_dotenv

load_dotenv()

# Initialize Firebase Admin SDK
# You will need to download your service account key from Firebase Console
# Project Settings > Service accounts > Generate new private key
# Place the file in the backend folder and set the path here
SERVICE_ACCOUNT_KEY_PATH = os.getenv("FIREBASE_SERVICE_ACCOUNT_KEY", "serviceAccountKey.json")

def initialize_firebase():
    if not firebase_admin._apps:
        cred = credentials.Certificate(SERVICE_ACCOUNT_KEY_PATH)
        firebase_admin.initialize_app(cred)

def get_db():
    initialize_firebase()
    return firestore.client()
