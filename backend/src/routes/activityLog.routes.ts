import { Router } from 'express';
import {
  logActivity,
  getActivityLogs,
  getActivitySummary
} from '../controllers/activityLog.controller';
import { authMiddleware } from '../middleware/auth';

const router = Router();

// Activity Log routes
router.post('/', logActivity);
router.get('/', authMiddleware, getActivityLogs);
router.get('/summary', authMiddleware, getActivitySummary);

export default router;
