package $package$
package backend

import java.util.UUID

import cats.effect.{Async, Ref, Resource}
import cats.effect.std.Dispatcher
import cats.syntax.all.*

import com.linecorp.armeria.server.Server
import sttp.tapir.server.armeria.cats.{
  ArmeriaCatsServerInterpreter,
  ArmeriaCatsServerOptions,
}
import sttp.tapir.server.interceptor.cors.{CORSConfig, CORSInterceptor}
import sttp.tapir.server.interceptor.log.DefaultServerLog

import common.Api
import common.model.*

object BackendApp:

  def rootServerEndpoint[F[_]: Async] =
    Api.rootEndpoint.serverLogicSuccess: _ =>
      Async[F].delay("$name$ API Server is running!")

  def loginServerEndpoint[F[_]: Async](
      config: BackendConfig,
      session: Ref[F, Set[UUID]],
  ) =
    Api.loginEndpoint.serverLogic: (loginRequest: LoginRequest) =>
      ???

  def allEndpoints[F[_]: Async](
      config: BackendConfig,
      session: Ref[F, Set[UUID]],
  ) = List(
    rootServerEndpoint[F],
    loginServerEndpoint[F](config, session),
  )

  @SuppressWarnings(Array("org.wartremover.warts.Null"))
  def getServer[F[_]: Async](
      config: BackendConfig,
      dispatcher: Dispatcher[F],
      session: Ref[F, Set[UUID]],
  ): F[Server] = Async[F].async_[Server]: cb =>
    def log[F[_]: Async](
        level: scribe.Level,
    )(msg: String, exOpt: Option[Throwable])(using
        mdc: scribe.data.MDC,
    ): F[Unit] = Async[F].delay:
      exOpt match
        case None     => scribe.log(level, mdc, msg)
        case Some(ex) => scribe.log(level, mdc, msg, ex)
    val serverLog = DefaultServerLog(
      doLogWhenReceived = log(scribe.Level.Info)(_, None),
      doLogWhenHandled = log(scribe.Level.Info),
      doLogAllDecodeFailures = log(scribe.Level.Info),
      doLogExceptions =
        (msg: String, ex: Throwable) => Async[F].delay(scribe.warn(msg, ex)),
      noLog = Async[F].pure(()),
    )

    val serverOptions = ArmeriaCatsServerOptions
      .customiseInterceptors[F](dispatcher)
      .corsInterceptor(
        CORSInterceptor.customOrThrow(
          CORSConfig.default.copy(
            allowedMethods = CORSConfig.AllowedMethods.All,
          ),
        ),
      )
      .serverLog(serverLog)
      .options
    val tapirService = ArmeriaCatsServerInterpreter[F](serverOptions)
      .toService(allEndpoints[F](config, session))
    val server = Server.builder
      .maxRequestLength(128 * 1024 * 1024)
      .requestTimeout(java.time.Duration.ofMinutes(10))
      .http(config.server.port)
      .service(tapirService)
      .build
    server.start.handle[Unit]:
      case (_, null)  => cb(server.asRight[Throwable])
      case (_, cause) => cb(cause.asLeft[Server])

    ()

  def resource[F[_]: Async](
      config: BackendConfig,
      session: Ref[F, Set[UUID]],
  ): Resource[F, Server] =
    for
      dispatcher <- Dispatcher.parallel[F]
      server <- Resource.make(getServer(config, dispatcher, session)): server =>
        Async[F]
          .fromCompletableFuture(Async[F].delay(server.closeAsync()))
          .map(_ => ())
    yield server
