import { Response } from 'express';
import pool from '../config/database';
import { CustomRequest } from '../types/express';

export const addPerformanceMetrics = async (req: CustomRequest, res: Response) => {
  try {
    if (!req.user) {
      return res.status(401).json({ error: 'Unauthorized' });
    }

    const { session_id, clarity_score, confidence_score, fluency_score, pace_score, overall_score, comments } = req.body;

    // Get user
    const userResult = await pool.query(
      'SELECT id FROM users WHERE firebase_uid = $1',
      [req.user.uid]
    );

    if (userResult.rows.length === 0) {
      return res.status(404).json({ error: 'User not found' });
    }

    const userId = userResult.rows[0].id;

    // Calculate overall score if not provided
    let calculatedScore = overall_score;
    if (!calculatedScore && clarity_score && confidence_score && fluency_score && pace_score) {
      calculatedScore = (clarity_score + confidence_score + fluency_score + pace_score) / 4;
    }

    const result = await pool.query(
      `INSERT INTO performance_metrics (user_id, session_id, clarity_score, confidence_score, fluency_score, pace_score, overall_score, comments)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
       RETURNING *`,
      [userId, session_id, clarity_score, confidence_score, fluency_score, pace_score, calculatedScore, comments]
    );

    res.status(201).json({
      message: 'Performance metrics added',
      metrics: result.rows[0],
    });
  } catch (error: any) {
    console.error('Error adding performance metrics:', error);
    res.status(500).json({ error: error.message });
  }
};

export const getUserPerformanceMetrics = async (req: CustomRequest, res: Response) => {
  try {
    if (!req.user) {
      return res.status(401).json({ error: 'Unauthorized' });
    }

    const { days = 30, page = 1, limit = 20 } = req.query;
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
      `SELECT * FROM performance_metrics
       WHERE user_id = $1 AND created_at > NOW() - INTERVAL '${days} days'
       ORDER BY created_at DESC
       LIMIT $2 OFFSET $3`,
      [userId, limit, offset]
    );

    const countResult = await pool.query(
      `SELECT COUNT(*) FROM performance_metrics
       WHERE user_id = $1 AND created_at > NOW() - INTERVAL '${days} days'`,
      [userId]
    );

    res.json({
      metrics: result.rows,
      total: parseInt(countResult.rows[0].count),
      page: parseInt(page as string),
      limit: parseInt(limit as string),
      days: parseInt(days as string),
    });
  } catch (error: any) {
    console.error('Error fetching performance metrics:', error);
    res.status(500).json({ error: error.message });
  }
};

export const getPerformanceSummary = async (req: CustomRequest, res: Response) => {
  try {
    if (!req.user) {
      return res.status(401).json({ error: 'Unauthorized' });
    }

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
      `SELECT
        COUNT(*) as total_assessments,
        AVG(clarity_score) as avg_clarity,
        AVG(confidence_score) as avg_confidence,
        AVG(fluency_score) as avg_fluency,
        AVG(pace_score) as avg_pace,
        AVG(overall_score) as avg_overall,
        MAX(overall_score) as highest_score,
        MIN(overall_score) as lowest_score
       FROM performance_metrics
       WHERE user_id = $1 AND created_at > NOW() - INTERVAL '30 days'`,
      [userId]
    );

    res.json(result.rows[0]);
  } catch (error: any) {
    console.error('Error fetching performance summary:', error);
    res.status(500).json({ error: error.message });
  }
};

export const getPerformanceTrend = async (req: CustomRequest, res: Response) => {
  try {
    if (!req.user) {
      return res.status(401).json({ error: 'Unauthorized' });
    }

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
      `SELECT
        DATE_TRUNC('day', created_at) as date,
        AVG(overall_score) as avg_score,
        COUNT(*) as session_count
       FROM performance_metrics
       WHERE user_id = $1 AND created_at > NOW() - INTERVAL '30 days'
       GROUP BY DATE_TRUNC('day', created_at)
       ORDER BY date ASC`,
      [userId]
    );

    res.json({
      trend: result.rows,
      period: 'last_30_days',
    });
  } catch (error: any) {
    console.error('Error fetching performance trend:', error);
    res.status(500).json({ error: error.message });
  }
};
