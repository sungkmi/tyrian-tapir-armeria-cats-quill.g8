package $package$.backend

import java.util.UUID

import scala.util.Try

import cats.syntax.either.*

import pureconfig.*
import pureconfig.generic.derivation.default.*
import pureconfig.error.CannotConvert
import scodec.bits.ByteVector

import BackendConfig.*

final case class BackendConfig(
    login: Map[String, LoginConfig],
    server: ServerConfig,
) derives ConfigReader

object BackendConfig:
  def load: BackendConfig = ConfigSource.default.loadOrThrow[BackendConfig]

  final case class LoginConfig(
      salt: UUID,
      passwordHash: ByteVector,
  ) derives ConfigReader

  final case class ServerConfig(
      port: Int,
  ) derives ConfigReader

  given uuidConfigReader: ConfigReader[UUID] =
    ConfigReader[String].emap: str =>
      Try(UUID.fromString(str)).toEither.leftMap: msg =>
        CannotConvert(str, "UUID", msg.getMessage)

  given byteVectorConfigReader: ConfigReader[ByteVector] =
    ConfigReader[String].emap: str =>
      ByteVector.fromHexDescriptive(str).leftMap: msg =>
        CannotConvert(str, "ByteVector", msg)
