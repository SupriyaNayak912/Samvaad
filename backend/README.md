# Samvaad Backend API Documentation

## Overview
Samvaad backend is a Node.js/Express server providing REST APIs for a speech practice and interview preparation mobile application.

## Tech Stack
- **Runtime**: Node.js
- **Language**: TypeScript
- **Framework**: Express.js
- **Database**: PostgreSQL
- **Authentication**: Firebase Auth + JWT
- **Cloud Storage**: AWS S3
- **Real-time**: Firebase Firestore

## API Endpoints

### Authentication `/api/auth`

#### Register User
- **POST** `/api/auth/register`
- **Body**: `{ email, password, name }`
- **Response**: `{ user, token }`

#### Login
- **POST** `/api/auth/login`
- **Body**: `{ email, password }`
- **Response**: `{ user, token }`

#### Google Login
- **POST** `/api/auth/google-login`
- **Body**: `{ idToken }`
- **Response**: `{ user, token }`

#### Logout
- **POST** `/api/auth/logout`
- **Headers**: `Authorization: Bearer {token}`
- **Response**: `{ message }`

### User Management `/api/users`

#### Get User Profile
- **GET** `/api/users/profile`
- **Headers**: `Authorization: Bearer {token}`
- **Response**: `{ id, firebase_uid, email, name, role, stats }`

#### Update User Profile
- **PUT** `/api/users/profile`
- **Headers**: `Authorization: Bearer {token}`
- **Body**: `{ name, bio, avatar_url }`
- **Response**: `{ message, user }`

#### Get User Stats
- **GET** `/api/users/stats`
- **Headers**: `Authorization: Bearer {token}`
- **Response**: `{ user, sessions, performance }`

#### Get All Users
- **GET** `/api/users?page=1&limit=20`
- **Response**: `{ users, total, page, limit }`

### Scenarios `/api/scenarios`

#### Get All Scenarios
- **GET** `/api/scenarios?page=1&limit=20&category=interview&difficulty=easy`
- **Query Params**: `page`, `limit`, `category`, `difficulty`
- **Response**: `{ scenarios, total, page, limit }`

#### Get Scenario by ID
- **GET** `/api/scenarios/{id}`
- **Response**: `{ id, title, description, category, difficulty, tips, audio_url }`

#### Get Random Scenario
- **GET** `/api/scenarios/random`
- **Response**: `{ id, title, description, category, difficulty }`

#### Create Scenario
- **POST** `/api/scenarios`
- **Headers**: `Authorization: Bearer {token}`
- **Body**: `{ title, description, category, difficulty, duration_seconds, tips, audio_url }`
- **Response**: `{ id, title, ... }`

#### Update Scenario
- **PUT** `/api/scenarios/{id}`
- **Headers**: `Authorization: Bearer {token}`
- **Body**: `{ title, description, category, difficulty, ... }`
- **Response**: `{ id, title, ... }`

#### Delete Scenario
- **DELETE** `/api/scenarios/{id}`
- **Headers**: `Authorization: Bearer {token}`
- **Response**: `{ message }`

### Sessions `/api/sessions`

#### Start Session
- **POST** `/api/sessions/start`
- **Headers**: `Authorization: Bearer {token}`
- **Body**: `{ scenario_id }`
- **Response**: `{ message, session }`

#### End Session
- **POST** `/api/sessions/{session_id}/end`
- **Headers**: `Authorization: Bearer {token}`
- **Body**: `{ score, feedback, recording_url, duration_ms }`
- **Response**: `{ message, session }`

#### Get User Sessions
- **GET** `/api/sessions?page=1&limit=20`
- **Headers**: `Authorization: Bearer {token}`
- **Response**: `{ sessions, total, page, limit }`

#### Get Session Details
- **GET** `/api/sessions/{session_id}`
- **Headers**: `Authorization: Bearer {token}`
- **Response**: `{ id, user_id, scenario_id, start_time, end_time, score, feedback, ... }`

### Activity Logs `/api/activity-logs`

#### Log Activity
- **POST** `/api/activity-logs`
- **Body**: `{ firebase_uid, timestamp, screen_name, event_type, duration_ms, metadata }`
- **Response**: `{ message, log }`

#### Get Activity Logs
- **GET** `/api/activity-logs?page=1&limit=50&screen_name=SessionScreen`
- **Headers**: `Authorization: Bearer {token}`
- **Query Params**: `page`, `limit`, `screen_name`
- **Response**: `{ logs, total, page, limit }`

#### Get Activity Summary
- **GET** `/api/activity-logs/summary`
- **Headers**: `Authorization: Bearer {token}`
- **Response**: `{ summary, period }`

### Stats `/api/stats`

#### Add Performance Metrics
- **POST** `/api/stats/metrics`
- **Headers**: `Authorization: Bearer {token}`
- **Body**: `{ session_id, clarity_score, confidence_score, fluency_score, pace_score, overall_score, comments }`
- **Response**: `{ message, metrics }`

#### Get Performance Metrics
- **GET** `/api/stats/metrics?days=30&page=1&limit=20`
- **Headers**: `Authorization: Bearer {token}`
- **Response**: `{ metrics, total, page, limit, days }`

#### Get Performance Summary
- **GET** `/api/stats/summary`
- **Headers**: `Authorization: Bearer {token}`
- **Response**: `{ total_assessments, avg_clarity, avg_confidence, avg_fluency, avg_pace, avg_overall }`

#### Get Performance Trend
- **GET** `/api/stats/trend`
- **Headers**: `Authorization: Bearer {token}`
- **Response**: `{ trend, period }`

## Database Schema

### Users Table
```sql
id (PK), firebase_uid (UNIQUE), email (UNIQUE), name, role, 
member_since, total_sessions, practice_streak, best_score, 
avatar_url, bio, created_at, updated_at
```

### Scenarios Table
```sql
id (PK), title, description, category, difficulty, 
duration_seconds, tips, audio_url, created_at, updated_at
```

### User Sessions Table
```sql
id (PK), user_id (FK), scenario_id (FK), start_time, end_time, 
duration_ms, score, feedback, recording_url, status, created_at
```

### Activity Logs Table
```sql
id (PK), user_id (FK), firebase_uid, timestamp, screen_name, 
event_type, duration_ms, metadata (JSONB), created_at
```

### Performance Metrics Table
```sql
id (PK), user_id (FK), session_id (FK), clarity_score, 
confidence_score, fluency_score, pace_score, overall_score, 
comments, created_at
```

## Setup Instructions

### Prerequisites
- Node.js 16+
- PostgreSQL 12+
- Firebase Project
- AWS S3 Bucket

### Installation

1. Clone the repository
```bash
git clone <repo-url>
cd backend
```

2. Install dependencies
```bash
npm install
```

3. Setup environment variables
```bash
cp .env.example .env
# Edit .env with your configuration
```

4. Initialize database
```bash
npm run migrate
```

5. Start the server
```bash
npm run dev  # Development
npm run build && npm start  # Production
```

## Error Handling

All errors return a JSON response with the following format:
```json
{
  "error": "Error message",
  "status": 400
}
```

## Security Features

- **JWT Authentication**: Token-based authentication for protected routes
- **CORS**: Cross-origin resource sharing enabled
- **Helmet**: Security headers middleware
- **Input Validation**: Express-validator for request validation
- **Firebase Security**: Firebase Admin SDK for secure operations

## Deployment

### Docker
```bash
docker build -t samvaad-backend .
docker run -p 8000:8000 --env-file .env samvaad-backend
```

### Environment Variables for Production
- Set `NODE_ENV=production`
- Use strong `JWT_SECRET`
- Configure production database
- Set proper CORS origins
- Configure AWS credentials

## Future Enhancements

1. **Video Processing**: Integrate AWS MediaConvert for video transcoding
2. **Real-time Analytics**: Add WebSocket support for live metrics
3. **Email Notifications**: Send practice reminders and feedback
4. **ML Integration**: Add speech analysis using AWS Transcribe/Comprehend
5. **Admin Dashboard**: Create CMS for scenario management
6. **Rate Limiting**: Add Redis-based rate limiting
7. **Caching**: Implement Redis caching for scenarios

## Support

For issues and questions, please open an issue on GitHub.
