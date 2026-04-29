# 🎓 Samvaad Backend - Learning Edition

## What You Have

A **simple, local backend** perfect for learning Android development tasks:

- ✅ **SQLite Database** - Local file storage (no PostgreSQL)
- ✅ **Firebase Authentication** - Secure login/signup
- ✅ **Express.js API** - 30+ REST endpoints
- ✅ **Local Development** - Run on your computer
- ✅ **Learning Focused** - Simple, easy to understand

---

## 📱 Perfect For These Learning Tasks

- **Task 6** - Fetch data from REST API
- **Task 10** - Database-backed app
- **Task 11** - User authentication with Firebase
- **Task 12** - Background tasks & activity logging

---

## 🚀 Start Here

### 1️⃣ Read This First
**→ `LEARNING_SETUP.md`** (5 min read)

### 2️⃣ Setup Backend
```bash
cd D:\Samvaad\backend
npm install
npm run dev
```

### 3️⃣ Test It Works
```bash
curl http://localhost:8000/health
```

### 4️⃣ Connect Android App
Follow `ANDROID_INTEGRATION_GUIDE.md`

---

## 📚 Documentation

| File | Purpose | Read Time |
|------|---------|-----------|
| **LEARNING_SETUP.md** | Setup & overview | 5 min |
| **QUICKSTART.md** | Quick start guide | 5 min |
| **README.md** | API reference | 10 min |
| **ANDROID_INTEGRATION_GUIDE.md** | Connect app | 15 min |

---

## 🔌 30+ API Endpoints

Perfect for learning REST API calls:

**Authentication** (5 endpoints)
- Register user
- Login with email
- Google OAuth
- Verify token

**User Data** (3 endpoints)
- Get profile
- Update profile
- Get statistics

**Interview Questions** (3 endpoints)
- List questions
- Get random question
- Get specific question

**Practice Sessions** (4 endpoints)
- Start session
- End session
- Get history
- Get details

**Activity Tracking** (3 endpoints)
- Log activity
- Get logs
- Get summary

**Performance Metrics** (4 endpoints)
- Save scores
- Get history
- Get summary
- Get trends

---

## 💾 Local Database (SQLite)

Stored in: `D:\Samvaad\backend\samvaad.db`

Tables:
- `users` - User accounts
- `scenarios` - Interview questions
- `user_sessions` - Practice records
- `activity_logs` - Activity tracking
- `performance_metrics` - Performance scores

---

## 🔧 Setup (3 Steps)

### Step 1: Install
```bash
cd D:\Samvaad\backend
npm install
```

### Step 2: Configure
```bash
cp .env.example .env
# Edit .env with Firebase credentials
```

### Step 3: Run
```bash
npm run dev
# Backend at http://localhost:8000
```

---

## ✨ What You'll Learn

- ✅ REST API concepts
- ✅ How to fetch data from API
- ✅ User authentication flows
- ✅ Database operations
- ✅ Firebase integration
- ✅ Activity logging
- ✅ Performance tracking
- ✅ Error handling

---

## 🎯 Quick Commands

```bash
npm run dev         # Start backend
npm run build       # Build code
npm run migrate     # Create database
npm run lint        # Check code
```

---

## 📱 Android Integration

Update `RetrofitClient.java`:
```java
private static final String BASE_URL = "http://10.0.2.2:8000/api/";
```

(See ANDROID_INTEGRATION_GUIDE.md for complete steps)

---

## 🧪 Test Endpoints

### Postman
- Import `Postman_Collection.json`
- Set `base_url` to `http://localhost:8000/api`
- Test each endpoint

### cURL
```bash
curl http://localhost:8000/api/scenarios?limit=5
```

---

## 📊 Perfect for Learning

- **Simple** - No Docker/Kubernetes
- **Local** - Run on your computer
- **Complete** - All features included
- **Educational** - Well-commented code
- **Real** - Actual API endpoints

---

## 🎓 Learning Path

### Week 1: Basics
- Setup backend
- Understand API structure
- Test endpoints

### Week 2: Authentication
- Register/login
- JWT tokens
- Connect to Android

### Week 3: Data Fetching
- Get scenarios
- Parse responses
- Display in app

### Week 4+: Full App
- Sessions & tracking
- Performance metrics
- Activity logging

---

## 📞 Getting Started

**Next:** Open `LEARNING_SETUP.md` →

It has:
- Complete setup instructions
- Example API calls
- Troubleshooting tips
- Learning tasks covered

---

## ✅ Ready to Go

Everything is setup and ready:
- ✅ Source code complete
- ✅ Database configured
- ✅ API endpoints working
- ✅ Documentation ready
- ✅ Examples included

**Start with:** `LEARNING_SETUP.md`

Happy Learning! 🎉
