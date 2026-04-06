import { Router } from 'express';
import { register, login, googleLogin, logout, verifyToken } from '../controllers/auth.controller';
import { authMiddleware } from '../middleware/auth';

const router = Router();

// Auth routes
router.post('/register', register);
router.post('/login', login);
router.post('/google-login', googleLogin);
router.post('/logout', authMiddleware, logout);
router.post('/verify-token', authMiddleware, verifyToken);

export default router;
