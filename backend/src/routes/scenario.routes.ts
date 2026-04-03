import { Router } from 'express';
import {
  getScenarios,
  getScenarioById,
  getRandomScenario,
  createScenario,
  updateScenario,
  deleteScenario
} from '../controllers/scenario.controller';
import { optionalAuthMiddleware, authMiddleware } from '../middleware/auth';

const router = Router();

// Scenario routes
router.get('/', optionalAuthMiddleware, getScenarios);
router.get('/random', optionalAuthMiddleware, getRandomScenario);
router.get('/:id', optionalAuthMiddleware, getScenarioById);
router.post('/', authMiddleware, createScenario);
router.put('/:id', authMiddleware, updateScenario);
router.delete('/:id', authMiddleware, deleteScenario);

export default router;
