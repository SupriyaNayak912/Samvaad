import { Response } from 'express';
import pool from '../config/database';
import { CustomRequest } from '../types/express';

export const getScenarios = async (req: CustomRequest, res: Response) => {
  try {
    const { page = 1, limit = 20, category, difficulty } = req.query;
    const offset = ((parseInt(page as string) - 1) * parseInt(limit as string));

    let query = 'SELECT * FROM scenarios WHERE 1=1';
    const params: any[] = [];

    if (category) {
      params.push(category);
      query += ` AND category = $${params.length}`;
    }

    if (difficulty) {
      params.push(difficulty);
      query += ` AND difficulty = $${params.length}`;
    }

    query += ` ORDER BY created_at DESC LIMIT $${params.length + 1} OFFSET $${params.length + 2}`;
    params.push(limit, offset);

    const result = await pool.query(query, params);

    const countParams: any[] = [];
    let countQuery = 'SELECT COUNT(*) FROM scenarios WHERE 1=1';

    if (category) {
      countParams.push(category);
      countQuery += ` AND category = $${countParams.length}`;
    }

    if (difficulty) {
      countParams.push(difficulty);
      countQuery += ` AND difficulty = $${countParams.length}`;
    }

    const countResult = await pool.query(countQuery, countParams);

    res.json({
      scenarios: result.rows,
      total: parseInt(countResult.rows[0].count),
      page: parseInt(page as string),
      limit: parseInt(limit as string),
    });
  } catch (error: any) {
    console.error('Error fetching scenarios:', error);
    res.status(500).json({ error: error.message });
  }
};

export const getScenarioById = async (req: CustomRequest, res: Response) => {
  try {
    const { id } = req.params;

    const result = await pool.query(
      'SELECT * FROM scenarios WHERE id = $1',
      [id]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Scenario not found' });
    }

    res.json(result.rows[0]);
  } catch (error: any) {
    console.error('Error fetching scenario:', error);
    res.status(500).json({ error: error.message });
  }
};

export const getRandomScenario = async (req: CustomRequest, res: Response) => {
  try {
    const result = await pool.query(
      'SELECT * FROM scenarios ORDER BY RANDOM() LIMIT 1'
    );

    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'No scenarios available' });
    }

    res.json(result.rows[0]);
  } catch (error: any) {
    console.error('Error fetching random scenario:', error);
    res.status(500).json({ error: error.message });
  }
};

export const createScenario = async (req: CustomRequest, res: Response) => {
  try {
    const { title, description, category, difficulty, duration_seconds, tips, audio_url } = req.body;

    if (!title || !category || !difficulty) {
      return res.status(400).json({ error: 'Title, category, and difficulty are required' });
    }

    const result = await pool.query(
      `INSERT INTO scenarios (title, description, category, difficulty, duration_seconds, tips, audio_url)
       VALUES ($1, $2, $3, $4, $5, $6, $7)
       RETURNING *`,
      [title, description, category, difficulty, duration_seconds, tips, audio_url]
    );

    res.status(201).json(result.rows[0]);
  } catch (error: any) {
    console.error('Error creating scenario:', error);
    res.status(500).json({ error: error.message });
  }
};

export const updateScenario = async (req: CustomRequest, res: Response) => {
  try {
    const { id } = req.params;
    const { title, description, category, difficulty, duration_seconds, tips, audio_url } = req.body;

    const result = await pool.query(
      `UPDATE scenarios
       SET title = COALESCE($1, title),
           description = COALESCE($2, description),
           category = COALESCE($3, category),
           difficulty = COALESCE($4, difficulty),
           duration_seconds = COALESCE($5, duration_seconds),
           tips = COALESCE($6, tips),
           audio_url = COALESCE($7, audio_url),
           updated_at = CURRENT_TIMESTAMP
       WHERE id = $8
       RETURNING *`,
      [title, description, category, difficulty, duration_seconds, tips, audio_url, id]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Scenario not found' });
    }

    res.json(result.rows[0]);
  } catch (error: any) {
    console.error('Error updating scenario:', error);
    res.status(500).json({ error: error.message });
  }
};

export const deleteScenario = async (req: CustomRequest, res: Response) => {
  try {
    const { id } = req.params;

    const result = await pool.query(
      'DELETE FROM scenarios WHERE id = $1 RETURNING id',
      [id]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Scenario not found' });
    }

    res.json({ message: 'Scenario deleted successfully' });
  } catch (error: any) {
    console.error('Error deleting scenario:', error);
    res.status(500).json({ error: error.message });
  }
};
