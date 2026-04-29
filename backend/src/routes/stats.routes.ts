import { Router } from 'express';
import {
  addPerformanceMetrics,
  getUserPerformanceMetrics,
  getPerformanceSummary,
  getPerformanceTrend
} from '../controllers/stats.controller';
import { authMiddleware } from '../middleware/auth';

const router = Router();

router.post('/metrics', authMiddleware, addPerformanceMetrics);
router.get('/metrics', authMiddleware, getUserPerformanceMetrics);
router.get('/summary', authMiddleware, getPerformanceSummary);
router.get('/trend', authMiddleware, getPerformanceTrend);

export default router;
