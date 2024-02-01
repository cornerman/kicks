$version: "2"
namespace kicks.api

use alloy#simpleRestJson

@simpleRestJson
service KicksService {
  version: "1.0.0",
  operations: [Hello]
}

@simpleRestJson
service KicksStreamService {
  version: "1.0.0",
  operations: [Subscribe]
}

@http(method: "POST", uri: "/hello/{name}", code: 200)
operation Hello {
  input: Person,
  output: Greeting
  errors: [AppError]
}

structure Person {
  @httpLabel
  @required
  name: String,

  @httpQuery("town")
  town: String
}

structure Greeting {
  @required
  message: String
}

@http(method: "POST", uri: "/subscribe/{name}", code: 200)
operation Subscribe {
  input: SubscribeInput,
  output: SubscribeOutput
}

structure SubscribeInput {
  @httpLabel
  @required
  name: String
}

structure SubscribeOutput {
  @httpHeader("X-wolf")
  wolf: String

  @httpPayload
  event: MyEvent
}

@streaming
union MyEvent {
  one: MyStructure
  error: MyError
}

structure MyStructure {
  @required
  thing: String
}

@error("client")
@retryable(throttling: true)
structure MyError {}

@error("client")
structure AppError {}
