# Example

HttpServer with sqlite database (litefs). Deployable to fly.io

## Development

### Change HTTP API

We generate our HTTP routes from smithy files.
Edit `./httpServer/src/main/resources/kicks.smithy` to define the API.
Then compile your code (`httpServer/compile`), this will regenerate scala files, and guide you.

An exception: smithy4s does not properly generate code for streaming.
That is why, we manually write define our `subscribe` streaming endpoint for ServerSentEvents - it is also not shown in the generated docs.
You can change it here: `httpServer/src/main/scala/kicks/http/ServerRoutes.scala`.

### Change DB Schema

We generate our database objects from an sql file.
Edit `./schema.sql` to define the schema.
Then compile your code (`db/compile`), this will regenerate scala files, and guide you.
