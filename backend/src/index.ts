import express, { Express, Request, Response } from 'express';
import cors from 'cors';
import helmet from 'helmet';
import dotenv from 'dotenv';
import { auth, firestore } from './config/firebase';
import jwt from 'jsonwebtoken';

dotenv.config();

const app: Express = express();
const PORT = process.env.PORT || 8000;

// Middleware
app.use(helmet());
app.use(cors({
  origin: '*',
  credentials: true,
}));
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ limit: '10mb', extended: true }));

// JWT Middleware
const authMiddleware = async (req: any, res: Response, next: any) => {
  try {
    const token = req.headers.authorization?.split(' ')[1];
    if (!token) {
      return res.status(401).json({ error: 'No token provided' });
    }

    const decoded = jwt.verify(token, process.env.JWT_SECRET || 'secret');
    req.user = decoded;
    next();
  } catch (error) {
    res.status(401).json({ error: 'Invalid token' });
  }
};

// Health Check
app.get('/health', (req: Request, res: Response) => {
  res.json({ status: 'ok', timestamp: new Date().toISOString() });
});

// Auth Routes
app.post('/api/auth/register', async (req: Request, res: Response) => {
  try {
    const { email, password, name } = req.body;

    if (!email || !password || !name) {
      return res.status(400).json({ error: 'Email, password, and name are required' });
    }

    const firebaseUser = await auth.createUser({
      email,
      password,
      displayName: name,
    });

    await firestore.collection('users').doc(firebaseUser.uid).set({
      uid: firebaseUser.uid,
      email,
      name,
      role: 'user',
      memberSince: new Date(),
      totalSessions: 0,
      practiceStreak: 0,
      bestScore: 0,
    });

    const token = jwt.sign(
      { uid: firebaseUser.uid, email },
      process.env.JWT_SECRET || 'secret',
      { expiresIn: process.env.JWT_EXPIRY || '7d' } as any
    );

    res.status(201).json({
      message: 'User registered successfully',
      user: { uid: firebaseUser.uid, email, name },
      token,
    });
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
});

app.post('/api/auth/login', async (req: Request, res: Response) => {
  try {
    const { email, password } = req.body;

    if (!email || !password) {
      return res.status(400).json({ error: 'Email and password are required' });
    }

    try {
      await auth.getUserByEmail(email);
    } catch {
      return res.status(401).json({ error: 'Invalid credentials' });
    }

    const usersSnapshot = await firestore
      .collection('users')
      .where('email', '==', email)
      .limit(1)
      .get();

    if (usersSnapshot.empty) {
      return res.status(401).json({ error: 'User not found' });
    }

    const user = usersSnapshot.docs[0].data();

    const token = jwt.sign(
      { uid: user.uid, email: user.email },
      process.env.JWT_SECRET || 'secret',
      { expiresIn: process.env.JWT_EXPIRY || '7d' } as any
    );

    res.json({
      message: 'Login successful',
      user: { uid: user.uid, email: user.email, name: user.name },
      token,
    });
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
});

// Scenario Routes
app.get('/api/scenarios', async (req: Request, res: Response) => {
  try {
    const { limit = 20 } = req.query;

    const snapshot = await firestore
      .collection('scenarios')
      .orderBy('createdAt', 'desc')
      .limit(parseInt(limit as string))
      .get();

    const scenarios = snapshot.docs.map(doc => ({
      id: doc.id,
      ...doc.data(),
    }));

    res.json({ scenarios });
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
});

app.get('/api/scenarios/random', async (req: Request, res: Response) => {
  try {
    const snapshot = await firestore.collection('scenarios').get();

    if (snapshot.empty) {
      return res.status(404).json({ error: 'No scenarios available' });
    }

    const docs = snapshot.docs;
    const randomDoc = docs[Math.floor(Math.random() * docs.length)];

    res.json({ id: randomDoc.id, ...randomDoc.data() });
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
});

app.get('/api/scenarios/:id', async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const doc = await firestore.collection('scenarios').doc(id).get();

    if (!doc.exists) {
      return res.status(404).json({ error: 'Scenario not found' });
    }

    res.json({ id: doc.id, ...doc.data() });
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
});

// User Routes
app.get('/api/users/profile', authMiddleware, async (req: any, res: Response) => {
  try {
    const doc = await firestore.collection('users').doc(req.user.uid).get();

    if (!doc.exists) {
      return res.status(404).json({ error: 'User not found' });
    }

    res.json(doc.data());
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
});

app.put('/api/users/profile', authMiddleware, async (req: any, res: Response) => {
  try {
    const { name, bio } = req.body;

    await firestore.collection('users').doc(req.user.uid).update({
      name,
      bio,
      updatedAt: new Date(),
    });

    res.json({ message: 'Profile updated successfully' });
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
});

// Activity Log
app.post('/api/activity-logs', async (req: Request, res: Response) => {
  try {
    const { firebase_uid, screen_name, event_type, duration_ms } = req.body;

    await firestore.collection('activity_logs').add({
      firebase_uid,
      timestamp: new Date(),
      screen_name,
      event_type,
      duration_ms,
    });

    res.status(201).json({ message: 'Activity logged' });
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
});

app.get('/api/activity-logs', authMiddleware, async (req: any, res: Response) => {
  try {
    const snapshot = await firestore
      .collection('activity_logs')
      .where('firebase_uid', '==', req.user.uid)
      .orderBy('timestamp', 'desc')
      .limit(50)
      .get();

    const logs = snapshot.docs.map(doc => ({
      id: doc.id,
      ...doc.data(),
    }));

    res.json({ logs });
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
});

app.listen(PORT, () => {
  console.log(`✓ Firebase Connected`);
  console.log(`🚀 Samvaad Backend running on http://localhost:${PORT}`);
  console.log(`Environment: ${process.env.NODE_ENV}`);
});
