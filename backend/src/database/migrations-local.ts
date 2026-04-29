import db from '../config/database-local';

export async function createTables() {
  return new Promise<void>((resolve, reject) => {
    db.serialize(() => {
      try {
        // Users table
        db.run(`
          CREATE TABLE IF NOT EXISTS users (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            firebase_uid TEXT UNIQUE NOT NULL,
            email TEXT UNIQUE NOT NULL,
            name TEXT,
            password_hash TEXT,
            role TEXT DEFAULT 'user',
            member_since INTEGER DEFAULT (strftime('%s', 'now') * 1000),
            total_sessions INTEGER DEFAULT 0,
            practice_streak INTEGER DEFAULT 0,
            best_score REAL DEFAULT 0,
            avatar_url TEXT,
            bio TEXT,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
            updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
          )
        `);
        console.log('✓ Users table created');

        // Scenarios table
        db.run(`
          CREATE TABLE IF NOT EXISTS scenarios (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            title TEXT NOT NULL,
            description TEXT,
            category TEXT,
            difficulty TEXT,
            duration_seconds INTEGER,
            tips TEXT,
            audio_url TEXT,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
            updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
          )
        `);
        console.log('✓ Scenarios table created');

        // User Sessions table
        db.run(`
          CREATE TABLE IF NOT EXISTS user_sessions (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            user_id INTEGER NOT NULL,
            scenario_id INTEGER,
            start_time DATETIME DEFAULT CURRENT_TIMESTAMP,
            end_time DATETIME,
            duration_ms INTEGER,
            score REAL,
            feedback TEXT,
            recording_url TEXT,
            status TEXT DEFAULT 'in_progress',
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
            FOREIGN KEY (scenario_id) REFERENCES scenarios(id)
          )
        `);
        console.log('✓ User Sessions table created');

        // Activity Logs table
        db.run(`
          CREATE TABLE IF NOT EXISTS activity_logs (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            user_id INTEGER,
            firebase_uid TEXT,
            timestamp INTEGER,
            screen_name TEXT,
            event_type TEXT,
            duration_ms INTEGER,
            metadata TEXT,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
          )
        `);
        console.log('✓ Activity Logs table created');

        // Performance Metrics table
        db.run(`
          CREATE TABLE IF NOT EXISTS performance_metrics (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            user_id INTEGER NOT NULL,
            session_id INTEGER,
            clarity_score REAL,
            confidence_score REAL,
            fluency_score REAL,
            pace_score REAL,
            overall_score REAL,
            comments TEXT,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
            FOREIGN KEY (session_id) REFERENCES user_sessions(id)
          )
        `);
        console.log('✓ Performance Metrics table created');

        // Create indexes
        db.run(`CREATE INDEX IF NOT EXISTS idx_users_firebase_uid ON users(firebase_uid)`);
        db.run(`CREATE INDEX IF NOT EXISTS idx_users_email ON users(email)`);
        db.run(`CREATE INDEX IF NOT EXISTS idx_sessions_user_id ON user_sessions(user_id)`);
        db.run(`CREATE INDEX IF NOT EXISTS idx_activity_logs_user_id ON activity_logs(user_id)`);
        db.run(`CREATE INDEX IF NOT EXISTS idx_activity_logs_firebase_uid ON activity_logs(firebase_uid)`);
        db.run(`CREATE INDEX IF NOT EXISTS idx_performance_metrics_user_id ON performance_metrics(user_id)`, () => {
          console.log('✓ Indexes created');
          console.log('✅ Database schema initialized successfully');
          resolve();
        });
      } catch (error) {
        console.error('❌ Error creating tables:', error);
        reject(error);
      }
    });
  });
}

// Run migrations
createTables().catch(err => {
  console.error('Migration failed:', err);
  process.exit(1);
});
