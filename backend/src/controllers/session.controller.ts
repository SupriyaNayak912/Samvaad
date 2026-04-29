import { Response } from 'express';
import pool from '../config/database';
import { CustomRequest } from '../types/express';

export const startSession = async (req: CustomRequest, res: Response) => {
  try {
    if (!req.user) {
      return res.status(401).json({ error: 'Unauthorized' });
    }

    const { scenario_id } = req.body;

    // Get user
    const userResult = await pool.query(
      'SELECT id FROM users WHERE firebase_uid = $1',
      [req.user.uid]
    );

    if (userResult.rows.length === 0) {
      return res.status(404).json({ error: 'User not found' });
    }

    const userId = userResult.rows[0].id;

    // Create session
    const result = await pool.query(
      `INSERT INTO user_sessions (user_id, scenario_id, status)
       VALUES ($1, $2, $3)
       RETURNING id, user_id, scenario_id, start_time, status`,
      [userId, scenario_id || null, 'in_progress']
    );

    res.status(201).json({
      message: 'Session started',
      session: result.rows[0],
    });
  } catch (error: any) {
    console.error('Error starting session:', error);
    res.status(500).json({ error: error.message });
  }
};

export const endSession = async (req: CustomRequest, res: Response) => {
  try {
    if (!req.user) {
      return res.status(401).json({ error: 'Unauthorized' });
    }

    const { session_id } = req.params;
    const { score, feedback, recording_url, duration_ms } = req.body;

    // Get user
    const userResult = await pool.query(
      'SELECT id FROM users WHERE firebase_uid = $1',
      [req.user.uid]
    );

    if (userResult.rows.length === 0) {
      return res.status(404).json({ error: 'User not found' });
    }

    const userId = userResult.rows[0].id;

    // Update session
    const result = await pool.query(
      `UPDATE user_sessions
       SET end_time = CURRENT_TIMESTAMP,
           score = $1,
           feedback = $2,
           recording_url = $3,
           duration_ms = $4,
           status = 'completed'
       WHERE id = $5 AND user_id = $6
       RETURNING *`,
      [score, feedback, recording_url, duration_ms, session_id, userId]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Session not found' });
    }

    // Update user stats
    await pool.query(
      `UPDATE users
       SET total_sessions = total_sessions + 1,
           best_score = GREATEST(best_score, $1),
           updated_at = CURRENT_TIMESTAMP
       WHERE id = $2`,
      [score || 0, userId]
    );

    res.json({
      message: 'Session ended',
      session: result.rows[0],
    });
  } catch (error: any) {
    console.error('Error ending session:', error);
    res.status(500).json({ error: error.message });
  }
};

export const getUserSessions = async (req: CustomRequest, res: Response) => {
  try {
    if (!req.user) {
      return res.status(401).json({ error: 'Unauthorized' });
    }

    const { page = 1, limit = 20 } = req.query;
    const offset = ((parseInt(page as string) - 1) * parseInt(limit as string));

    // Get user
    const userResult = await pool.query(
      'SELECT id FROM users WHERE firebase_uid = $1',
      [req.user.uid]
    );

    if (userResult.rows.length === 0) {
      return res.status(404).json({ error: 'User not found' });
    }

    const userId = userResult.rows[0].id;

    const result = await pool.query(
      `SELECT us.*, s.title, s.category, s.difficulty
       FROM user_sessions us
       LEFT JOIN scenarios s ON us.scenario_id = s.id
       WHERE us.user_id = $1
       ORDER BY us.created_at DESC
       LIMIT $2 OFFSET $3`,
      [userId, limit, offset]
    );

    const countResult = await pool.query(
      'SELECT COUNT(*) FROM user_sessions WHERE user_id = $1',
      [userId]
    );

    res.json({
      sessions: result.rows,
      total: parseInt(countResult.rows[0].count),
      page: parseInt(page as string),
      limit: parseInt(limit as string),
    });
  } catch (error: any) {
    console.error('Error fetching sessions:', error);
    res.status(500).json({ error: error.message });
  }
};

export const getSessionDetails = async (req: CustomRequest, res: Response) => {
  try {
    if (!req.user) {
      return res.status(401).json({ error: 'Unauthorized' });
    }

    const { session_id } = req.params;

    // Get user
    const userResult = await pool.query(
      'SELECT id FROM users WHERE firebase_uid = $1',
      [req.user.uid]
    );

    if (userResult.rows.length === 0) {
      return res.status(404).json({ error: 'User not found' });
    }

    const userId = userResult.rows[0].id;

    const result = await pool.query(
      `SELECT us.*, s.title, s.category, s.difficulty, s.tips
       FROM user_sessions us
       LEFT JOIN scenarios s ON us.scenario_id = s.id
       WHERE us.id = $1 AND us.user_id = $2`,
      [session_id, userId]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Session not found' });
    }

    res.json(result.rows[0]);
  } catch (error: any) {
    console.error('Error fetching session details:', error);
    res.status(500).json({ error: error.message });
  }
};
