import { Response } from 'express';
import jwt from 'jsonwebtoken';
import { auth, firestore } from '../config/firebase';
import { CustomRequest } from '../types/express';

export const register = async (req: CustomRequest, res: Response) => {
  try {
    const { email, password, name } = req.body;

    if (!email || !password || !name) {
      return res.status(400).json({ error: 'Email, password, and name are required' });
    }

    // Create Firebase user
    const firebaseUser = await auth.createUser({
      email,
      password,
      displayName: name,
    });

    // Store user data in Firestore
    await firestore.collection('users').doc(firebaseUser.uid).set({
      uid: firebaseUser.uid,
      email,
      name,
      role: 'user',
      memberSince: new Date(),
      totalSessions: 0,
      practiceStreak: 0,
      bestScore: 0,
      createdAt: new Date(),
      updatedAt: new Date(),
    });

    // Generate JWT token
    const token = jwt.sign(
      { uid: firebaseUser.uid, email },
      process.env.JWT_SECRET || 'secret',
      { expiresIn: process.env.JWT_EXPIRY || '7d' }
    );

    res.status(201).json({
      message: 'User registered successfully',
      user: {
        uid: firebaseUser.uid,
        email,
        name,
      },
      token,
    });
  } catch (error: any) {
    console.error('Registration error:', error);
    res.status(500).json({ error: error.message });
  }
};

export const login = async (req: CustomRequest, res: Response) => {
  try {
    const { email, password } = req.body;

    if (!email || !password) {
      return res.status(400).json({ error: 'Email and password are required' });
    }

    // Verify with Firebase
    try {
      await auth.getUserByEmail(email);
    } catch (err) {
      return res.status(401).json({ error: 'Invalid credentials' });
    }

    // Get user from Firestore
    const usersSnapshot = await firestore
      .collection('users')
      .where('email', '==', email)
      .limit(1)
      .get();

    if (usersSnapshot.empty) {
      return res.status(401).json({ error: 'User not found' });
    }

    const user = usersSnapshot.docs[0].data();

    // Generate JWT token
    const token = jwt.sign(
      { uid: user.uid, email: user.email },
      process.env.JWT_SECRET || 'secret',
      { expiresIn: process.env.JWT_EXPIRY || '7d' }
    );

    res.json({
      message: 'Login successful',
      user: {
        uid: user.uid,
        email: user.email,
        name: user.name,
      },
      token,
    });
  } catch (error: any) {
    console.error('Login error:', error);
    res.status(500).json({ error: error.message });
  }
};

export const googleLogin = async (req: CustomRequest, res: Response) => {
  try {
    const { idToken } = req.body;

    if (!idToken) {
      return res.status(400).json({ error: 'ID token is required' });
    }

    // Verify token with Firebase
    const decodedToken = await auth.verifyIdToken(idToken);
    const { uid, email, name } = decodedToken;

    // Check if user exists in Firestore
    const userDoc = await firestore.collection('users').doc(uid).get();

    if (!userDoc.exists) {
      // Create new user
      await firestore.collection('users').doc(uid).set({
        uid,
        email,
        name,
        role: 'user',
        memberSince: new Date(),
        totalSessions: 0,
        practiceStreak: 0,
        bestScore: 0,
        createdAt: new Date(),
        updatedAt: new Date(),
      });
    }

    // Generate JWT token
    const token = jwt.sign(
      { uid, email },
      process.env.JWT_SECRET || 'secret',
      { expiresIn: process.env.JWT_EXPIRY || '7d' }
    );

    res.json({
      message: 'Google login successful',
      user: {
        uid,
        email,
        name,
      },
      token,
    });
  } catch (error: any) {
    console.error('Google login error:', error);
    res.status(500).json({ error: error.message });
  }
};

export const logout = async (req: CustomRequest, res: Response) => {
  // JWT-based auth doesn't require server-side logout
  // Token is invalidated on client side
  res.json({ message: 'Logout successful' });
};

export const verifyToken = async (req: CustomRequest, res: Response) => {
  try {
    if (!req.user) {
      return res.status(401).json({ error: 'No user found' });
    }

    res.json({ valid: true, user: req.user });
  } catch (error: any) {
    res.status(401).json({ error: error.message });
  }
};
