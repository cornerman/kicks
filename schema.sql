-- This file represents the database schema. You can edit it.
-- After editing, we create a new migration file.

CREATE TABLE thread (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    post_id INTEGER NOT NULL REFERENCES Post(id) ON DELETE RESTRICT ON UPDATE RESTRICT
);

CREATE TABLE post (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    parent_id INTEGER REFERENCES Post(id) ON DELETE SET NULL ON UPDATE RESTRICT,
    text TEXT NOT NULL
);
