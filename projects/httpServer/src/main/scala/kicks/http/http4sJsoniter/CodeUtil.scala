package kicks.http.http4sJsoniter

import org.http4s.{DecodeFailure, MalformedMessageBodyFailure}

private[http4sJsoniter] object CodeUtil {

  def decodeResultFailure(error: Throwable): DecodeFailure =
    MalformedMessageBodyFailure("Invalid JSON", Some(error))

}
