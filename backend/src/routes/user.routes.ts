import { Router } from 'express';
import {
  getUserProfile,
  updateUserProfile,
  getUserStats,
  getAllUsers
} from '../controllers/user.controller';
import { authMiddleware } from '../middleware/auth';

const router = Router();

// User routes
router.get('/profile', authMiddleware, getUserProfile);
router.put('/profile', authMiddleware, updateUserProfile);
router.get('/stats', authMiddleware, getUserStats);
router.get('/', getAllUsers);

export default router;
