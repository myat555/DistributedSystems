-- Creates one database per saga participant that needs persistence.
-- Runs once, automatically, on first postgres container start (docker-entrypoint-initdb.d).
CREATE DATABASE orders;
CREATE DATABASE inventory;
CREATE DATABASE payments;
