# Samvaad Backend - PowerShell Environment Setup
# This script creates .env file with all required credentials

Write-Host " Creating .env file..." -ForegroundColor Green
Write-Host ""

# Set the backend directory
$backendDir = "D:\Samvaad\backend"
$envFile = "$backendDir\.env"

# Create .env content
$envContent = @"
PORT=8000
NODE_ENV=development
JWT_SECRET=Kx9`$mP2#nL@qR7vW4bZ8yC5xF1aD3gH6jT0sN4uM9kP2wL5qR8tU1vX4yA7bC0dE3
JWT_EXPIRY=7d
FIREBASE_PROJECT_ID=your-project-id-here
FIREBASE_PRIVATE_KEY="-----BEGIN PRIVATE KEY-----\nYOUR-KEY-HERE\n-----END PRIVATE KEY-----\n"
FIREBASE_CLIENT_EMAIL=firebase-adminsdk@your-project.iam.gserviceaccount.com
GOOGLE_CLIENT_ID=optional
GOOGLE_CLIENT_SECRET=optional
"@

# Write to .env file
Set-Content -Path $envFile -Value $envContent

Write-Host "✅ .env file created successfully!" -ForegroundColor Green
Write-Host ""
Write-Host "⚠️  IMPORTANT: Edit the .env file and add your Firebase credentials:" -ForegroundColor Yellow
Write-Host ""
Write-Host "1️⃣  Open: $envFile" -ForegroundColor Cyan
Write-Host ""
Write-Host "2️⃣  Replace these values with your actual Firebase credentials:" -ForegroundColor Cyan
Write-Host "    • FIREBASE_PROJECT_ID" -ForegroundColor White
Write-Host "    • FIREBASE_PRIVATE_KEY" -ForegroundColor White
Write-Host "    • FIREBASE_CLIENT_EMAIL" -ForegroundColor White
Write-Host ""
Write-Host "3️⃣  Get these from:" -ForegroundColor Cyan
Write-Host "    • Go to: https://console.firebase.google.com" -ForegroundColor White
Write-Host "    • Settings → Service Accounts → Generate New Private Key" -ForegroundColor White
Write-Host ""
Write-Host "4️⃣  After editing, run:" -ForegroundColor Cyan
Write-Host "    npm run dev" -ForegroundColor White
Write-Host ""
Read-Host "Press Enter to continue"
