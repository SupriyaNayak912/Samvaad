import { Response } from 'express';
import pool from '../config/database';
import { CustomRequest } from '../types/express';

export const getUserProfile = async (req: CustomRequest, res: Response) => {
  try {
    if (!req.user) {
      return res.status(401).json({ error: 'Unauthorized' });
    }

    const result = await pool.query(
      `SELECT id, firebase_uid, email, name, role, member_since, total_sessions,
              practice_streak, best_score, avatar_url, bio, created_at, updated_at
       FROM users WHERE firebase_uid = $1`,
      [req.user.uid]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'User not found' });
    }

    res.json(result.rows[0]);
  } catch (error: any) {
    console.error('Error fetching user profile:', error);
    res.status(500).json({ error: error.message });
  }
};

export const updateUserProfile = async (req: CustomRequest, res: Response) => {
  try {
    if (!req.user) {
      return res.status(401).json({ error: 'Unauthorized' });
    }

    const { name, bio, avatar_url } = req.body;

    const result = await pool.query(
      `UPDATE users
       SET name = COALESCE($1, name),
           bio = COALESCE($2, bio),
           avatar_url = COALESCE($3, avatar_url),
           updated_at = CURRENT_TIMESTAMP
       WHERE firebase_uid = $4
       RETURNING *`,
      [name, bio, avatar_url, req.user.uid]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'User not found' });
    }

    res.json({ message: 'Profile updated successfully', user: result.rows[0] });
  } catch (error: any) {
    console.error('Error updating user profile:', error);
    res.status(500).json({ error: error.message });
  }
};

export const getUserStats = async (req: CustomRequest, res: Response) => {
  try {
    if (!req.user) {
      return res.status(401).json({ error: 'Unauthorized' });
    }

    // Get user
    const userResult = await pool.query(
      'SELECT * FROM users WHERE firebase_uid = $1',
      [req.user.uid]
    );

    if (userResult.rows.length === 0) {
      return res.status(404).json({ error: 'User not found' });
    }

    const user = userResult.rows[0];

    // Get session statistics
    const sessionsResult = await pool.query(
      `SELECT
        COUNT(*) as total_sessions,
        AVG(score) as average_score,
        MAX(score) as highest_score,
        COUNT(CASE WHEN status = 'completed' THEN 1 END) as completed_sessions
       FROM user_sessions WHERE user_id = $1`,
      [user.id]
    );

    // Get recent performance
    const performanceResult = await pool.query(
      `SELECT
        AVG(clarity_score) as avg_clarity,
        AVG(confidence_score) as avg_confidence,
        AVG(fluency_score) as avg_fluency,
        AVG(pace_score) as avg_pace
       FROM performance_metrics WHERE user_id = $1 AND created_at > NOW() - INTERVAL '30 days'`,
      [user.id]
    );

    res.json({
      user: {
        name: user.name,
        email: user.email,
        member_since: user.member_since,
        practice_streak: user.practice_streak,
        best_score: user.best_score,
      },
      sessions: sessionsResult.rows[0],
      performance: performanceResult.rows[0],
    });
  } catch (error: any) {
    console.error('Error fetching user stats:', error);
    res.status(500).json({ error: error.message });
  }
};

export const getAllUsers = async (req: CustomRequest, res: Response) => {
  try {
    const { page = 1, limit = 20 } = req.query;
    const offset = ((parseInt(page as string) - 1) * parseInt(limit as string));

    const result = await pool.query(
      `SELECT id, firebase_uid, email, name, role, total_sessions, practice_streak, best_score
       FROM users
       LIMIT $1 OFFSET $2`,
      [limit, offset]
    );

    const countResult = await pool.query('SELECT COUNT(*) FROM users');

    res.json({
      users: result.rows,
      total: parseInt(countResult.rows[0].count),
      page: parseInt(page as string),
      limit: parseInt(limit as string),
    });
  } catch (error: any) {
    console.error('Error fetching users:', error);
    res.status(500).json({ error: error.message });
  }
};
