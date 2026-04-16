-- V1: Initial baseline migration.
-- Real tables (accounts, users, crops, transactions, etc.) will be added in
-- subsequent migrations as each feature lands.
--
-- This file exists so Flyway has a starting point and the build doesn't fail
-- with "No migrations found" when the app boots for the first time.

SELECT 1;