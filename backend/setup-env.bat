@echo off
REM Samvaad Backend - Environment Setup Script
REM This script creates .env file with all required credentials

echo Creating .env file...

REM Create the .env file in the backend directory
cd /d D:\Samvaad\backend

REM Create .env with all values
(
echo PORT=8000
echo NODE_ENV=development
echo JWT_SECRET=Kx9$mP2#nL@qR7vW4bZ8yC5xF1aD3gH6jT0sN4uM9kP2wL5qR8tU1vX4yA7bC0dE3
echo JWT_EXPIRY=7d
echo FIREBASE_PROJECT_ID=your-project-id-here
echo FIREBASE_PRIVATE_KEY="-----BEGIN PRIVATE KEY-----\nYOUR-KEY-HERE\n-----END PRIVATE KEY-----\n"
echo FIREBASE_CLIENT_EMAIL=firebase-adminsdk@your-project.iam.gserviceaccount.com
echo GOOGLE_CLIENT_ID=optional
echo GOOGLE_CLIENT_SECRET=optional
) > .env

echo.
echo ✅ .env file created successfully!
echo.
echo ⚠️  IMPORTANT: Edit the .env file and add your Firebase credentials:
echo.
echo 1. Open: D:\Samvaad\backend\.env
echo 2. Replace these values with your actual Firebase credentials:
echo    - FIREBASE_PROJECT_ID
echo    - FIREBASE_PRIVATE_KEY
echo    - FIREBASE_CLIENT_EMAIL
echo.
echo 3. Get these from:
echo    https://console.firebase.google.com
echo    Settings → Service Accounts → Generate New Private Key
echo.
echo 4. After editing, run: npm run dev
echo.
pause
