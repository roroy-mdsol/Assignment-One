/*References used for assignment: https://github.com/akka/akka-http-quickstart-scala.g8
The Main class in particular is very strongly based off of the Main class in the project linked above
*/
package com.AssignmentOne

//akka imports
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.{Http, unmarshalling}

import scala.util.{Failure, Success}
//Route Imports
import akka.http.scaladsl.server.Route

object Main {
  val portNo = 8080;
  private def startHttpServer(routes: Route)(implicit system: ActorSystem[_]): Unit = {
    import system.executionContext

    val futureBinding = Http().newServerAt("localhost", portNo).bind(routes)
    futureBinding.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info("Server online at http://{}:{}/", address.getHostString, address.getPort)
      case Failure(exception) =>
        system.log.error("Failed to bind HTTP endpoint, terminating system", exception)
        system.terminate()
    }
  }
  def main(args: Array[String]): Unit = {
    //On start up, we create an ActorSystem with a UserAuthenticationActor as its guardian actor. Actors aren't created
    //using a standard constructor, but using a factory spawn method which is called on the ActorContext parameter.

    val rootBehavior = Behaviors.setup[Nothing] { context =>
      val userAuthenticationActor = context.spawn(UserAuthenticationActor()(context.system.executionContext), "UserRegistryActor") //spawning our root actor
      context.watch(userAuthenticationActor)

      val routes = new UserRoutes(userAuthenticationActor)(context.system)
      startHttpServer(routes.userRoutes)(context.system)

      Behaviors.empty
    }
    val system = ActorSystem[Nothing](rootBehavior, "UserAuthenticationSystem")

  }
}


