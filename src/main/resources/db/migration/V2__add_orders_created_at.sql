-- Adds a creation timestamp to orders, populated automatically by R2dbcConfig's
-- @EnableR2dbcAuditing (@CreatedDate on Order.createdAt). StockReportScheduler uses this
-- column to aggregate the day's orders; it did not exist in V1 since no scheduled reporting
-- was needed yet at that point.

ALTER TABLE orders ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT now();
