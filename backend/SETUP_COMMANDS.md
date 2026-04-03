# 🚀 Quick Commands to Setup Everything

## Option 1: Run PowerShell Script (Recommended for Windows)

```powershell
cd D:\Samvaad\backend
.\setup-env.ps1
```

This will:
1. Create `.env` file automatically
2. Add all fixed values (PORT, NODE_ENV, etc.)
3. Show you what to do next

Then edit the `.env` file and add your Firebase credentials.

---

## Option 2: Run Batch Script (Windows CMD)

```cmd
cd D:\Samvaad\backend
setup-env.bat
```

This will:
1. Create `.env` file
2. Add fixed values
3. Ask you to edit with Firebase credentials

---

## Option 3: Manual Commands (Any Terminal)

### Step 1: Navigate to backend
```bash
cd D:\Samvaad\backend
```

### Step 2: Create .env file with all values
```bash
# On Windows PowerShell:
echo 'PORT=8000
NODE_ENV=development
JWT_SECRET=Kx9$mP2#nL@qR7vW4bZ8yC5xF1aD3gH6jT0sN4uM9kP2wL5qR8tU1vX4yA7bC0dE3
JWT_EXPIRY=7d
FIREBASE_PROJECT_ID=your-project-id
FIREBASE_PRIVATE_KEY="-----BEGIN PRIVATE KEY-----\nYOUR-KEY\n-----END PRIVATE KEY-----\n"
FIREBASE_CLIENT_EMAIL=your-email@project.iam.gserviceaccount.com
GOOGLE_CLIENT_ID=optional
GOOGLE_CLIENT_SECRET=optional' > .env
```

### Step 3: Open and edit .env file
```bash
# On Windows:
notepad .env
```

### Step 4: Install dependencies
```bash
npm install
```

### Step 5: Start backend
```bash
npm run dev
```

---

## Full Complete Setup (Copy-Paste All at Once)

### Using PowerShell:
```powershell
# Navigate to backend
cd D:\Samvaad\backend

# Create .env file
@"
PORT=8000
NODE_ENV=development
JWT_SECRET=Kx9`$mP2#nL@qR7vW4bZ8yC5xF1aD3gH6jT0sN4uM9kP2wL5qR8tU1vX4yA7bC0dE3
JWT_EXPIRY=7d
FIREBASE_PROJECT_ID=your-firebase-project-id
FIREBASE_PRIVATE_KEY="-----BEGIN PRIVATE KEY-----\nYOUR-PRIVATE-KEY\n-----END PRIVATE KEY-----\n"
FIREBASE_CLIENT_EMAIL=firebase-adminsdk@project.iam.gserviceaccount.com
GOOGLE_CLIENT_ID=optional
GOOGLE_CLIENT_SECRET=optional
"@ | Set-Content -Path .env

# Install dependencies
npm install

# Open .env for editing (replace Firebase values)
notepad .env
```

### Using Command Prompt (CMD):
```cmd
cd D:\Samvaad\backend
setup-env.bat
npm install
notepad .env
```

---

## After Running Setup Script

1. ✅ `.env` file is created in `D:\Samvaad\backend\`
2. ⚠️ Edit the file and add your Firebase credentials
3. 🔑 Replace:
   - `your-firebase-project-id` with your actual project ID
   - `YOUR-PRIVATE-KEY` with your actual private key
   - `your-email@project.iam.gserviceaccount.com` with your client email
4. 💾 Save the file
5. 🚀 Run `npm run dev`

---

## All Available Scripts

| Script | Command | Purpose |
|--------|---------|---------|
| PowerShell | `.\setup-env.ps1` | Auto-create .env with prompts |
| Batch | `setup-env.bat` | Auto-create .env with prompts |
| Manual | `npm install` | Install dependencies |
| Run | `npm run dev` | Start backend |

---

## Step-by-Step Quickest Way

### 1. Run This Command
```powershell
cd D:\Samvaad\backend; .\setup-env.ps1
```

### 2. Edit the .env File
Open `D:\Samvaad\backend\.env` and add your Firebase values

### 3. Install Dependencies
```bash
npm install
```

### 4. Start Backend
```bash
npm run dev
```

---

## What Each Command Does

| Command | Does What |
|---------|-----------|
| `cd D:\Samvaad\backend` | Go to backend folder |
| `.\setup-env.ps1` | Create .env file automatically |
| `npm install` | Download all dependencies |
| `npm run dev` | Start development backend |

---

## 🎯 TL;DR - Just Copy This

### Windows PowerShell:
```powershell
cd D:\Samvaad\backend
.\setup-env.ps1
notepad .env
npm install
npm run dev
```

### Windows CMD:
```cmd
cd D:\Samvaad\backend
setup-env.bat
notepad .env
npm install
npm run dev
```

---

## ✅ Expected Result

After running `npm run dev`, you should see:

```
✓ Connected to SQLite database at: D:\Samvaad\backend\samvaad.db
🚀 Samvaad Backend running on http://localhost:8000
```

---

## If Script Doesn't Work

### Option 1: Allow PowerShell Scripts
```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

Then run:
```powershell
.\setup-env.ps1
```

### Option 2: Use Manual Method
```bash
cd D:\Samvaad\backend
notepad .env
```

Then copy-paste from `CREDENTIALS_REFERENCE.md`

---

**Choose one method above and follow it!** 🚀
