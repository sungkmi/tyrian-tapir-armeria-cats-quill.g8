package $package$
package frontend

import cats.effect.{IO, Resource, ResourceApp}
import cats.effect.std.Dispatcher
import tyrian.*

object FrontendMain extends ResourceApp.Forever:
  def run(args: List[String]): Resource[IO, Unit] =
    for
      dispatcher <- Dispatcher.parallel[IO]
      app <- Resource.eval:
        $app_name$[IO](dispatcher)
    yield TyrianApp.onLoad("$app_name$" -> app)
