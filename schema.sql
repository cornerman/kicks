-- This file represents the database schema. You can edit it.
-- After editing, we create a new migration file with liquibase.
--
-- Info:
-- - PRIMARY KEY needs NOT NULL, otherwise liquibase will oscillate between create/drop not-null-constraint.
-- - CREATE TABLE misses foreign keys

CREATE TABLE foo(wolf text);

-- Create Address table
CREATE TABLE Address (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    street_number Integer
);

-- Create Person table
CREATE TABLE Person (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    foo TEXT,
    name TEXT NOT NULL,
    age Integer
    --address_id INTEGER,
    --bar text,
    --blub INTEGER NOT NULL,
    --FOREIGN KEY (address_id) REFERENCES Address(id)
);

--CREATE INDEX idx_posts_user_id ON Address (id);
--CREATE INDEX idx_comments_post_id ON Person (id);
--CREATE VIEW user_post_counts AS SELECT name from Person;
--CREATE TRIGGER update_last_active_after_post
--AFTER INSERT ON Person
--BEGIN
--    insert into foo values('person');
--END;

--create table user(
--    id INTEGER PRIMARY KEY NOT NULL,
--    name TEXT NOT NULL
--);
--
--create table user_password(
--    user_id INTEGER PRIMARY KEY NOT NULL,
--    hashed_password TEXT NOT NULL,
--    FOREIGN KEY (user_id) REFERENCES user(id)
--);
