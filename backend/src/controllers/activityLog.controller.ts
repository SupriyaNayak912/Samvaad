import { Response } from 'express';
import pool from '../config/database';
import { firestore } from '../config/firebase';
import { CustomRequest } from '../types/express';

export const logActivity = async (req: CustomRequest, res: Response) => {
  try {
    const { firebase_uid, timestamp, screen_name, event_type, duration_ms, metadata } = req.body;

    if (!firebase_uid || !screen_name || !event_type) {
      return res.status(400).json({ error: 'firebase_uid, screen_name, and event_type are required' });
    }

    // Get user ID from firebase_uid
    const userResult = await pool.query(
      'SELECT id FROM users WHERE firebase_uid = $1',
      [firebase_uid]
    );

    const userId = userResult.rows.length > 0 ? userResult.rows[0].id : null;

    // Log to database
    const result = await pool.query(
      `INSERT INTO activity_logs (user_id, firebase_uid, timestamp, screen_name, event_type, duration_ms, metadata)
       VALUES ($1, $2, $3, $4, $5, $6, $7)
       RETURNING *`,
      [userId, firebase_uid, timestamp, screen_name, event_type, duration_ms, JSON.stringify(metadata || {})]
    );

    // Also log to Firestore for real-time access
    try {
      await firestore.collection('activity_logs').add({
        firebase_uid,
        timestamp: timestamp || new Date().getTime(),
        screen_name,
        event_type,
        duration_ms: duration_ms || 0,
        metadata: metadata || {},
        created_at: new Date(),
      });
    } catch (err) {
      console.warn('Failed to log to Firestore:', err);
    }

    res.status(201).json({
      message: 'Activity logged',
      log: result.rows[0],
    });
  } catch (error: any) {
    console.error('Error logging activity:', error);
    res.status(500).json({ error: error.message });
  }
};

export const getActivityLogs = async (req: CustomRequest, res: Response) => {
  try {
    if (!req.user) {
      return res.status(401).json({ error: 'Unauthorized' });
    }

    const { page = 1, limit = 50, screen_name } = req.query;
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

    let query = 'SELECT * FROM activity_logs WHERE user_id = $1';
    const params: any[] = [userId];

    if (screen_name) {
      params.push(screen_name);
      query += ` AND screen_name = $${params.length}`;
    }

    query += ` ORDER BY timestamp DESC LIMIT $${params.length + 1} OFFSET $${params.length + 2}`;
    params.push(limit, offset);

    const result = await pool.query(query, params);

    const countQuery = screen_name
      ? 'SELECT COUNT(*) FROM activity_logs WHERE user_id = $1 AND screen_name = $2'
      : 'SELECT COUNT(*) FROM activity_logs WHERE user_id = $1';

    const countParams = screen_name ? [userId, screen_name] : [userId];
    const countResult = await pool.query(countQuery, countParams);

    res.json({
      logs: result.rows,
      total: parseInt(countResult.rows[0].count),
      page: parseInt(page as string),
      limit: parseInt(limit as string),
    });
  } catch (error: any) {
    console.error('Error fetching activity logs:', error);
    res.status(500).json({ error: error.message });
  }
};

export const getActivitySummary = async (req: CustomRequest, res: Response) => {
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

    // Get summary stats
    const result = await pool.query(
      `SELECT
        screen_name,
        COUNT(*) as event_count,
        SUM(duration_ms) as total_duration_ms,
        AVG(duration_ms) as avg_duration_ms
       FROM activity_logs
       WHERE user_id = $1 AND timestamp > EXTRACT(EPOCH FROM NOW() - INTERVAL '7 days') * 1000
       GROUP BY screen_name
       ORDER BY event_count DESC`,
      [userId]
    );

    res.json({
      summary: result.rows,
      period: 'last_7_days',
    });
  } catch (error: any) {
    console.error('Error fetching activity summary:', error);
    res.status(500).json({ error: error.message });
  }
};
