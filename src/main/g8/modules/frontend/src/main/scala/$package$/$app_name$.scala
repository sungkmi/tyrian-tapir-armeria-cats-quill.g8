package $package$
package frontend

import cats.effect.Async
import cats.effect.std.Dispatcher
import cats.syntax.all.*

import tyrian.*
//import tyrian.Html.*

//import org.scalajs.dom

trait $app_name$[F[_]: Async: ApiClient.Ref] extends TyrianAppF[F, AppMsg, AppModel]:

  def init(flags: Map[String, String]): (AppModel, Cmd[F, AppMsg]) = ???

  def update(model: AppModel): AppMsg => (AppModel, Cmd[F, AppMsg]) = ???

  def view(model: AppModel): Html[AppMsg] = ???

  def router: Location => AppMsg = ???

  def subscriptions(model: AppModel): Sub[F, AppMsg] = ???

object $app_name$:
  def apply[F[_]: Async](dispatcher: Dispatcher[F]): F[$app_name$[F]] =
    for given ApiClient.Ref[F] <- ApiClient.Ref.empty[F]
    yield new $app_name$[F]:
      override val run: F[Nothing] => Unit =
        dispatcher.unsafeRunAndForget[Nothing]

sealed trait AppModel
final case class LoginModel(username: String, password: String) extends AppModel
final case class AfterLoginModel() extends AppModel

sealed trait AppMsg
case object NoOp extends AppMsg
sealed trait LoginMsg extends AppMsg
object LoginMsg:
  final case class UpdateUsername(username: String) extends LoginMsg
  final case class UpdatePassword(password: String) extends LoginMsg
  object Submit                                     extends LoginMsg
