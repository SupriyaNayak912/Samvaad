# 🎯 Samvaad Backend - Quick Reference

## Start Backend (30 seconds)

```bash
cd D:\Samvaad\backend
npm install
npm run dev
```

Backend runs at: `http://localhost:8000`

---

## Test It Works

```bash
curl http://localhost:8000/health
```

Should return: `{"status":"ok"}`

---

## 30+ API Endpoints

### Authentication
```
POST /api/auth/register      - Create account
POST /api/auth/login         - Login
POST /api/auth/verify-token  - Check token
```

### Users
```
GET  /api/users/profile      - Get profile
PUT  /api/users/profile      - Update profile
GET  /api/users/stats        - Get stats
```

### Questions
```
GET  /api/scenarios          - Get all questions
GET  /api/scenarios/random   - Random question
GET  /api/scenarios/{id}     - Specific question
```

### Practice
```
POST /api/sessions/start     - Start practice
POST /api/sessions/{id}/end  - End practice
GET  /api/sessions           - Get history
```

### Tracking
```
POST /api/activity-logs      - Log activity
GET  /api/activity-logs      - Get logs
GET  /api/stats/metrics      - Get performance
```

---

## Android Integration

Update in Android app:

```java
// RetrofitClient.java
private static final String BASE_URL = "http://10.0.2.2:8000/api/";
```

---

## Firebase Setup (One Time)

1. firebase.google.com
2. Create project
3. Service Accounts → Generate key
4. Copy to .env file

---

## Database

Stored in: `D:\Samvaad\backend\samvaad.db`

Tables:
- users
- scenarios  
- user_sessions
- activity_logs
- performance_metrics

---

## Commands

```bash
npm run dev      # Start development
npm run build    # Compile code
npm start        # Run compiled
npm run migrate  # Create tables
npm run lint     # Check code
```

---

## Test Endpoints

### Postman
- Import: `Postman_Collection.json`
- Base URL: `http://localhost:8000/api`

### cURL
```bash
curl http://localhost:8000/api/scenarios?limit=5
```

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| Port 8000 in use | Use `PORT=3001 npm run dev` |
| Database error | `rm samvaad.db && npm run migrate` |
| Firebase error | Check `.env` credentials |
| CORS error | Use correct Android URL |

---

## Learning Tasks Covered

- ✅ Task 6 - REST API
- ✅ Task 10 - Database app
- ✅ Task 11 - Authentication
- ✅ Task 12 - Activity logging

---

## Documentation

- `START_LEARNING.md` - Overview
- `LEARNING_SETUP.md` - Full setup
- `README.md` - API reference
- `ANDROID_INTEGRATION_GUIDE.md` - App integration

---

## Key Info

- **Database:** SQLite (local)
- **Auth:** Firebase + JWT
- **API:** Express.js
- **Port:** 8000
- **Perfect for:** Learning

---

**Ready to go! 🚀**
