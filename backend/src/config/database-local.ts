import sqlite3 from 'sqlite3';
import path from 'path';
import dotenv from 'dotenv';

dotenv.config();

// SQLite database file path (local storage)
const dbPath = path.join(__dirname, '../../samvaad.db');

const db = new sqlite3.Database(dbPath, (err) => {
  if (err) {
    console.error('Error opening database:', err);
  } else {
    console.log('✓ Connected to SQLite database at:', dbPath);
  }
});

// Enable foreign keys
db.run('PRAGMA foreign_keys = ON');

export default db;
