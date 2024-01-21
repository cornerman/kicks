-- This file represents the database schema. You can edit it.
-- After editing, we create a new migration file with liquibase.
--
-- Info:
-- - PRIMARY KEY needs NOT NULL, otherwise liquibase will oscillate between create/drop not-null-constraint.

CREATE TABLE foo(wolf text);

-- Create Address table
CREATE TABLE Address (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    street TEXT,
    city TEXT NOT NULL
);

-- Create Person table
CREATE TABLE Person (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    name TEXT NOT NULL,
    age INTEGER,
    address_id INTEGER,
    bar text,
    FOREIGN KEY (address_id) REFERENCES Address(id)
);
