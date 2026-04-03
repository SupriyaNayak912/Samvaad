use t timport pool from '../config/database';

export async function createTables() {
  try {
    // Users table
    await pool.query(`
      CREATE TABLE IF NOT EXISTS users (
        id SERIAL PRIMARY KEY,
        firebase_uid VARCHAR(255) UNIQUE NOT NULL,
        email VARCHAR(255) UNIQUE NOT NULL,
        name VARCHAR(255),
        role VARCHAR(50) DEFAULT 'user',
        member_since BIGINT DEFAULT EXTRACT(EPOCH FROM NOW()) * 1000,
        total_sessions INT DEFAULT 0,
        practice_streak INT DEFAULT 0,
        best_score FLOAT DEFAULT 0,
        avatar_url TEXT,
        bio TEXT,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      )
    `);
    console.log('✓ Users table created');

    // Scenarios table
    await pool.query(`
      CREATE TABLE IF NOT EXISTS scenarios (
        id SERIAL PRIMARY KEY,
        title VARCHAR(255) NOT NULL,
        description TEXT,
        category VARCHAR(100),
        difficulty VARCHAR(50),
        duration_seconds INT,
        tips TEXT,
        audio_url TEXT,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      )
    `);
    console.log('✓ Scenarios table created');

    // User Sessions table
    await pool.query(`
      CREATE TABLE IF NOT EXISTS user_sessions (
        id SERIAL PRIMARY KEY,
        user_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        scenario_id INT REFERENCES scenarios(id),
        start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        end_time TIMESTAMP,
        duration_ms INT,
        score FLOAT,
        feedback TEXT,
        recording_url TEXT,
        status VARCHAR(50) DEFAULT 'in_progress',
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      )
    `);
    console.log('✓ User Sessions table created');

    // Activity Logs table
    await pool.query(`
      CREATE TABLE IF NOT EXISTS activity_logs (
        id SERIAL PRIMARY KEY,
        user_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        firebase_uid VARCHAR(255),
        timestamp BIGINT,
        screen_name VARCHAR(255),
        event_type VARCHAR(100),
        duration_ms INT,
        metadata JSONB,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      )
    `);
    console.log('✓ Activity Logs table created');

    // Performance Metrics table
    await pool.query(`
      CREATE TABLE IF NOT EXISTS performance_metrics (
        id SERIAL PRIMARY KEY,
        user_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        session_id INT REFERENCES user_sessions(id),
        clarity_score FLOAT,
        confidence_score FLOAT,
        fluency_score FLOAT,
        pace_score FLOAT,
        overall_score FLOAT,
        comments TEXT,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      )
    `);
    console.log('✓ Performance Metrics table created');

    // Create indexes for better performance
    await pool.query(`CREATE INDEX IF NOT EXISTS idx_users_firebase_uid ON users(firebase_uid)`);
    await pool.query(`CREATE INDEX IF NOT EXISTS idx_sessions_user_id ON user_sessions(user_id)`);
    await pool.query(`CREATE INDEX IF NOT EXISTS idx_activity_logs_user_id ON activity_logs(user_id)`);
    await pool.query(`CREATE INDEX IF NOT EXISTS idx_activity_logs_firebase_uid ON activity_logs(firebase_uid)`);
    await pool.query(`CREATE INDEX IF NOT EXISTS idx_performance_metrics_user_id ON performance_metrics(user_id)`);
    console.log('✓ Indexes created');

    console.log('✅ Database schema initialized successfully');
  } catch (error) {
    console.error('❌ Error creating tables:', error);
    throw error;
  }
}

// Run migrations
createTables().catch(err => {
  console.error('Migration failed:', err);
  process.exit(1);
});
