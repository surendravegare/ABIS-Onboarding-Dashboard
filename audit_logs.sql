-- Run this ONCE on the server's ABIS_db database to add the audit log table.
-- Safe to run more than once (it only creates the table if it is missing).
-- Your existing tables and data are not touched.

CREATE TABLE IF NOT EXISTS audit_logs (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  created_at  DATETIME(6)  NOT NULL,
  actor       VARCHAR(100) NOT NULL,
  action      VARCHAR(50)  NOT NULL,
  target_type VARCHAR(50)  NULL,
  target_id   VARCHAR(50)  NULL,
  details     VARCHAR(2000) NULL,
  ip_address  VARCHAR(64)  NULL,
  PRIMARY KEY (id),
  KEY idx_audit_created (created_at),
  KEY idx_audit_action (action),
  KEY idx_audit_actor (actor)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
