-- Create "thread" table
CREATE TABLE `thread` (`id` integer NOT NULL PRIMARY KEY AUTOINCREMENT, `post_id` integer NOT NULL, CONSTRAINT `0` FOREIGN KEY (`post_id`) REFERENCES `Post` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT);
-- Create "post" table
CREATE TABLE `post` (`id` integer NOT NULL PRIMARY KEY AUTOINCREMENT, `parent_id` integer NULL, `text` text NOT NULL, CONSTRAINT `0` FOREIGN KEY (`parent_id`) REFERENCES `Post` (`id`) ON UPDATE RESTRICT ON DELETE SET NULL);

CREATE VIEW post_chain AS
    WITH RECURSIVE post_tree(id, parent_id, text) AS (
        SELECT * FROM post WHERE parent_id IS NULL
        UNION ALL
        SELECT p.* FROM post p INNER JOIN post_tree pt ON p.parent_id = pt.id
    )
    SELECT id, parent_id, text FROM post_tree
