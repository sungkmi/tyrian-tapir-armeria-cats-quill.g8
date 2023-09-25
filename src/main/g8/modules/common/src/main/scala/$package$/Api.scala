package $package$.common

import model.*

import io.circe.generic.auto.*
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*

object Api:
  val loginEndpoint = endpoint.post
    .in("login")
    .in(jsonBody[LoginRequest])
    .errorOut(stringBody)
    .out(jsonBody[LoginResponse])

  val rootEndpoint = endpoint.get.in("").out(stringBody)
