-- This file represents the database schema. You can edit it.
-- After editing, we create a new migration file.

CREATE TABLE foo(wolf text PRIMARY key);

-- Create Address table
CREATE TABLE Address (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL
);

-- Create Person table
CREATE TABLE Person (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    foo TEXT,
    name TEXT NOT NULL,
    age Integer,
    wolfgang TEXT,
    haha TEXT not null
    --address_id INTEGER,
    --bar text,
    --blub INTEGER NOT NULL,
    --FOREIGN KEY (address_id) REFERENCES Address(id)
);

create index person_name_and_age on person(name, age);
create index person_haha on person(haha);

create view my_view as select id from Person;
--create index my_view_name_and_age on my_view(name, age);
--create unique index my_view_foo on my_view(foo);

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
