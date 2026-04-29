# Samvaad Backend - Quick Start Guide

## 📋 Project Overview

**Samvaad** is a speech practice and interview preparation platform. This backend serves the mobile application (Android) with:

- 👤 User authentication & profile management
- 🎯 Practice scenarios/questions delivery
- 🎬 Session tracking and recording management
- 📊 Performance metrics and analytics
- 📝 Activity logging and audit trails

## 🚀 Quick Start (5 minutes)

### Option 1: Using Docker (Recommended)

```bash
cd backend
cp .env.example .env
# Edit .env with your Firebase credentials

docker-compose up
```

The backend will be available at `http://localhost:8000`

### Option 2: Manual Setup

```bash
# Install Node.js 16+ and PostgreSQL 12+

cd backend
npm install

# Setup environment
cp .env.example .env
# Edit .env with your configuration

# Create database and run migrations
npm run migrate

# Start development server
npm run dev
```

## 📁 Project Structure

```
backend/
├── src/
│   ├── config/           # Configuration (Database, Firebase, etc.)
│   ├── controllers/      # Business logic for each feature
│   ├── routes/          # API endpoint definitions
│   ├── middleware/      # Auth, error handling, etc.
│   ├── types/           # TypeScript type definitions
│   ├── utils/           # Helper functions (S3, Google Auth)
│   ├── database/        # Migrations and seeding
│   └── index.ts         # Main application entry
├── package.json         # Dependencies
├── tsconfig.json        # TypeScript config
├── Dockerfile           # Docker container definition
├── docker-compose.yml   # Docker services orchestration
└── kubernetes/          # K8s deployment files
```

## 🔌 API Quick Reference

### Authentication
```bash
POST /api/auth/register          # Register new user
POST /api/auth/login             # Email/password login
POST /api/auth/google-login      # Google OAuth login
POST /api/auth/verify-token      # Verify JWT token
```

### Users
```bash
GET /api/users/profile           # Get current user
PUT /api/users/profile           # Update profile
GET /api/users/stats             # Get user statistics
```

### Scenarios
```bash
GET /api/scenarios               # List scenarios (with filtering)
GET /api/scenarios/random        # Get random question
GET /api/scenarios/:id           # Get specific scenario
POST /api/scenarios              # Create scenario (admin)
```

### Sessions
```bash
POST /api/sessions/start         # Start practice session
POST /api/sessions/:id/end       # End session with score
GET /api/sessions                # Get user's sessions
GET /api/sessions/:id            # Get session details
```

### Activity & Stats
```bash
POST /api/activity-logs          # Log user activity
GET /api/activity-logs           # Get activity logs
GET /api/stats/metrics           # Get performance metrics
GET /api/stats/summary           # Get performance summary
GET /api/stats/trend             # Get performance trend
```

## 🔐 Environment Setup

Create `.env` file in backend directory:

```env
# Server
PORT=8000
NODE_ENV=development

# PostgreSQL Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=samvaad
DB_USER=postgres
DB_PASSWORD=password

# JWT
JWT_SECRET=your-super-secret-key-change-in-production
JWT_EXPIRY=7d

# Firebase (Get from Firebase Console)
FIREBASE_PROJECT_ID=your-project-id
FIREBASE_PRIVATE_KEY="-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----\n"
FIREBASE_CLIENT_EMAIL=firebase-adminsdk@your-project.iam.gserviceaccount.com

# AWS S3 (for video storage)
AWS_REGION=us-east-1
AWS_ACCESS_KEY_ID=your-access-key
AWS_SECRET_ACCESS_KEY=your-secret-key
S3_BUCKET=samvaad-videos

# Client URLs
FRONTEND_URL=http://localhost:3000
```

## 📦 Database Schema

The backend automatically creates tables on first run:

- **users** - User accounts and profiles
- **scenarios** - Interview questions/practice topics
- **user_sessions** - Practice session records
- **activity_logs** - User activity tracking
- **performance_metrics** - Performance scores and feedback

## 🧪 Running Migrations & Seeds

```bash
# Create tables
npm run migrate

# Seed sample scenarios
npx ts-node src/database/seed.ts
```

## 🛠️ Development

```bash
# Start dev server (auto-restart on file changes)
npm run dev

# Build TypeScript
npm run build

# Lint code
npm run lint

# Run tests
npm run test
```

## 📱 Integration with Android App

Update the Android app's `RetrofitClient.java`:

```java
private static final String BASE_URL = "http://your-backend-url:8000/api/v1/";
```

### Example: Fetch Scenarios

```bash
curl -X GET http://localhost:8000/api/scenarios?limit=10
```

Response:
```json
{
  "scenarios": [
    {
      "id": 1,
      "title": "Tell me about yourself",
      "category": "Behavioral",
      "difficulty": "Easy"
    }
  ],
  "total": 1,
  "page": 1,
  "limit": 10
}
```

### Example: Authentication Flow

1. **Register**
```bash
POST /api/auth/register
{
  "email": "user@example.com",
  "password": "password123",
  "name": "John Doe"
}
```

2. **Get Token** - Response includes `token`

3. **Use Token in Requests**
```bash
GET /api/users/profile
Authorization: Bearer <token>
```

## 🚀 Deployment

### Docker Deployment

```bash
# Build image
docker build -t samvaad-backend:latest .

# Run container
docker run -p 8000:8000 --env-file .env samvaad-backend:latest
```

### Kubernetes Deployment

```bash
kubectl apply -f kubernetes/deployment.yaml
```

### Cloud Platforms

- **Heroku**: `git push heroku main`
- **AWS**: Use Elastic Beanstalk
- **Google Cloud**: Use Cloud Run
- **Azure**: Use App Service

## 📊 Key Features

✅ **Authentication**
- Firebase-based user auth
- JWT tokens for API
- Google OAuth integration

✅ **Scenario Management**
- Store interview questions
- Categorization & difficulty levels
- Random question selection

✅ **Session Tracking**
- Record practice sessions
- Store video recordings
- Calculate scores

✅ **Analytics**
- Performance metrics (clarity, confidence, fluency, pace)
- Activity logging
- User statistics

✅ **Scalability**
- PostgreSQL for persistence
- Firebase for real-time logs
- S3 for video storage
- Kubernetes-ready

## 🐛 Troubleshooting

### Database Connection Error
- Ensure PostgreSQL is running: `psql -U postgres`
- Check `.env` database credentials

### Firebase Authentication Error
- Verify Firebase private key format (newlines as `\n`)
- Check Firebase credentials in `.env`

### CORS Error
- Update `FRONTEND_URL` in `.env`
- Ensure frontend URL matches in app

## 📚 Additional Resources

- [Express.js Docs](https://expressjs.com/)
- [PostgreSQL Docs](https://www.postgresql.org/docs/)
- [Firebase Admin SDK](https://firebase.google.com/docs/admin/setup)
- [TypeScript Handbook](https://www.typescriptlang.org/docs/)

## 🤝 Contributing

1. Create a new branch: `git checkout -b feature/your-feature`
2. Make changes and commit: `git commit -am 'Add feature'`
3. Push to branch: `git push origin feature/your-feature`
4. Submit a pull request

## 📄 License

MIT

## 📞 Support

For issues and questions, create a GitHub issue or contact the development team.

---

**Happy Coding! 🎉**
