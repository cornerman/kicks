# Example

HttpServer with sqlite database (litefs). Deployable to fly.io.

## Development

### DB Schema

We use sqlite for this application.
The deployed version (and the docker-compose version) use litefs to replicate sqlite to multiple instances.

We generate our scala database objects and schema definitions from an sql file.
Edit `./schema.sql` to define the schema. This is the source of truth.
Then compile your code (`db/compile`), this will regenerate scala files, and guide you.

We use the scala library `quill` for querying the database (see `./projects/db/src/main/scala/kicks/db/Db.scala`).

We run our migrations with flyway inside our scala application (see `./projects/db/src/main/resources/migrations/` and `./projects/db/src/main/scala/kicks/db/DbMigrations.scala`).
After editing `./schema.sql`, you need to write a migration file. You can diff the schema against the current migrations and generate a migration file automatically: `./scripts/new-db-migration <title>`. You might need to edit the generated migration afterwards.

### HTTP API

We generate our scala HTTP routes and json codecs from smithy files.
Edit `./projects/api/src/main/smithy/kicks.smithy` to define the API.
Then compile your code (`api/compile`), this will regenerate scala files, and guide you.

An exception from the generation: smithy4s does not properly generate code for streaming.
That is why, we manually define our `subscribe` streaming endpoint for ServerSentEvents - it is also not shown in the generated docs.

Our http server in scala is `http4s` (see `./projects/httpServer/src/main/scala/kicks/http/Server.scala` and `./projects/httpServer/src/main/scala/kicks/http/ServerRoutes.scala`)


## Gotchas

- sqlite has disabled foreign keys by default (backwards compatability ftw). So be sure to always use `jdbc:sqlite:filename?foreign_keys=ON` in your jdbc connection strings. Or alternatively set `#PRAGMA foreign_keys = ON` in each sqlite connection.
