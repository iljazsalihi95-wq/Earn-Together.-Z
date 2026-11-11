-- Native Quiz Arena sessions and progress. Wallet amounts remain in NativeAccounts.
CREATE TABLE IF NOT EXISTS arena_players (
  player_id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  locale TEXT NOT NULL DEFAULT 'sq',
  updated_at INTEGER NOT NULL
);
CREATE TABLE IF NOT EXISTS arena_sessions (
  session_id TEXT PRIMARY KEY,
  player_id TEXT NOT NULL,
  state_json TEXT NOT NULL,
  revision INTEGER NOT NULL DEFAULT 0,
  finished INTEGER NOT NULL DEFAULT 0,
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS arena_sessions_owned ON arena_sessions(player_id, finished, updated_at DESC);
CREATE UNIQUE INDEX IF NOT EXISTS arena_one_active_session ON arena_sessions(player_id) WHERE finished = 0;
CREATE TABLE IF NOT EXISTS arena_progress (
  player_id TEXT NOT NULL,
  level INTEGER NOT NULL,
  score INTEGER NOT NULL,
  correct INTEGER NOT NULL,
  total INTEGER NOT NULL,
  session_id TEXT NOT NULL,
  completed_at INTEGER NOT NULL,
  PRIMARY KEY(player_id, level)
);
CREATE TABLE IF NOT EXISTS arena_wallet_audit (
  player_id TEXT NOT NULL,
  level INTEGER NOT NULL,
  session_id TEXT NOT NULL,
  transaction_id TEXT NOT NULL,
  coins INTEGER NOT NULL CHECK(coins >= 0),
  status TEXT NOT NULL,
  awarded_at INTEGER NOT NULL,
  PRIMARY KEY(player_id, level)
);
CREATE TABLE IF NOT EXISTS arena_legacy_progress (
  player_id TEXT PRIMARY KEY,
  progress_json TEXT NOT NULL,
  resume_id TEXT,
  imported_at INTEGER NOT NULL
);
