-- Create "post" table
CREATE TABLE `post` (`id` integer NOT NULL PRIMARY KEY AUTOINCREMENT, `text` text NOT NULL);
-- Create "post_conn" table
CREATE TABLE `post_conn` (`child_id` integer NOT NULL, `parent_id` integer NOT NULL, PRIMARY KEY (`child_id`, `parent_id`), CONSTRAINT `0` FOREIGN KEY (`parent_id`) REFERENCES `Post` (`id`) ON UPDATE CASCADE ON DELETE CASCADE, CONSTRAINT `1` FOREIGN KEY (`child_id`) REFERENCES `Post` (`id`) ON UPDATE CASCADE ON DELETE CASCADE);

CREATE VIEW post_root AS
    SELECT * from post where not exists(select 1 from post_conn where post_conn.child_id = post.id);

CREATE VIEW post_chain AS
    WITH RECURSIVE post_tree(id, text, root_post_id, parent_post_id, depth) AS (
        SELECT post_root.*, post_root.id as root_post_id, NULL as parent_post_id, 0 as depth
        FROM post_root
        UNION ALL
        SELECT post.*, post_tree.root_post_id, post_tree.id as parent_post_id, post_tree.depth + 1 as depth
        FROM post
        INNER JOIN post_conn ON post_conn.child_id = post.id
        INNER JOIN post_tree ON post_tree.id = post_conn.parent_id
        WHERE post_tree.depth < 100
    )
    SELECT id, text, root_post_id, CAST(parent_post_id as INTEGER) as parent_post_id FROM post_tree where id is not null; -- type cast needed for jdbc to detect the correct typea
