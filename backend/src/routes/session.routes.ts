import { Router } from 'express';
import {
  startSession,
  endSession,
  getUserSessions,
  getSessionDetails
} from '../controllers/session.controller';
import { authMiddleware } from '../middleware/auth';

const router = Router();

// Session routes
router.post('/start', authMiddleware, startSession);
router.post('/:session_id/end', authMiddleware, endSession);
router.get('/', authMiddleware, getUserSessions);
router.get('/:session_id', authMiddleware, getSessionDetails);

export default router;
