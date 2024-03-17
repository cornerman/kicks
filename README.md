# Example

HttpServer with sqlite database (litefs). Deployable to fly.io.

## Development

Compile scala code (includes code generation for database schema and smithy4s api):
```sh
sbt dev  # compile - same as "webapp/fastLinkJS; httpServer/reStart"
sbt ~dev # or watch and compile
```

Run development server for frontend:
```
cd ./modules/webapp
yarn dev
```

## Production Build

Compile scala code:
```sh
sbt prod  # compile - same as "webapp/fullLinkJS; httpServer/assembly"
sbt ~prod # or watch and compile
```

Run development server for frontend:
```
cd ./modules/webapp
yarn build
```

### Auth

We use `@keratin/authn-server` for handling authentication.

### Database

We use sqlite for this application.
The deployed version (and the docker-compose version) use litefs to replicate sqlite to multiple instances.

We generate our scala database objects and schema definitions from an sql file.
Edit `./schema.sql` to define the schema. This is the source of truth.
Then compile your code (`db/compile`), this will regenerate scala files, and guide you.

We use the scala library `quill` for querying the database (see `./modules/db/src/main/scala/kicks/db/Db.scala`).

We run our migrations with flyway inside our scala application (see `./modules/db/src/main/resources/migrations/` and `./modules/db/src/main/scala/kicks/db/DbMigrations.scala`).
After editing `./schema.sql`, you need to write a migration file. You can diff the schema against the current migrations and generate a migration file automatically: `./scripts/new-db-migration <title>`. You might need to edit the generated migration afterwards.

### HTTP API

We generate our scala HTTP routes and json codecs from smithy files.
Edit `./modules/api/src/main/smithy/kicks.smithy` to define the API.
Then compile your code (`api/compile`), this will regenerate scala files, and guide you.

An exception from the generation: smithy4s does not properly generate code for streaming.
That is why, we manually define our `subscribe` streaming endpoint for ServerSentEvents - it is also not shown in the generated docs.

Our http server in scala is `http4s` (see `./modules/httpServer/src/main/scala/kicks/http/Server.scala` and `./modules/httpServer/src/main/scala/kicks/http/ServerRoutes.scala`)

### RPC API

We use `sloth` to have run RPC from frontend to backend via a scala trait. Check out `./modules/rpc/src/main/scala/kicks/rpc/Rpc.scala`.

The http server routes request to an implementation of that trait in `./modules/httpServer/src/main/scala/kicks/http/ServerRoutes.scala`.

The webapp client connects to the backend through method calls on that trait in `./modules/webapp/src/main/scala/kicks/webapp/RpcClient.scala`.

## Gotchas

- sqlite has disabled foreign keys by default (backwards compatability ftw). So be sure to always use `jdbc:sqlite:filename?foreign_keys=ON` in your jdbc connection strings. Or alternatively set `#PRAGMA foreign_keys = ON` in each sqlite connection.
