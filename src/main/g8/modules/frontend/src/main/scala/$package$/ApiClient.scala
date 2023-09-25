package $package$
package frontend

import cats.data.EitherT
import cats.effect.{Async, Ref as CERef}
import cats.syntax.all.*

import sttp.client3.*
import sttp.client3.impl.cats.FetchCatsBackend
import sttp.tapir.DecodeResult
import sttp.tapir.client.sttp.SttpClientInterpreter

import common.Api
import common.model.*

trait ApiClient[F[_]]:
  def login(request: LoginRequest): EitherT[F, String, LoginResponse]

object ApiClient:
  def apply[F[_]: ApiClient]: ApiClient[F] = summon

  def build[F[_]: Async](backendUrl: String): ApiClient[F] =
    println(s"Build ApiClient with backendUrl: \$backendUrl")
    new ApiClient[F]:
      val backend = FetchCatsBackend[F]()
      val loginClient = SttpClientInterpreter().toClient(
        Api.loginEndpoint,
        Some(uri"\$backendUrl"),
        backend,
      )
      def login(request: LoginRequest): EitherT[F, String, LoginResponse] =
        EitherT:
          loginClient(request).map:
            case DecodeResult.Value(value) =>
              value.leftMap(_ => s"Error response in login request: \$request")
            case f: DecodeResult.Failure =>
              Left(f.toString)

  type Ref[F[_]] = CERef[F, Option[ApiClient[F]]]
  object Ref:
    def apply[F[_]: Ref]: Ref[F]      = summon
    def empty[F[_]: Async]: F[Ref[F]] = CERef.of[F, Option[ApiClient[F]]](None)
