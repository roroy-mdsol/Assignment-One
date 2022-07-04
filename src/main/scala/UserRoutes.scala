package com.AssignmentOne

//akka imports
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout

//scala concurrency imports
import scala.concurrent.Future

//Route Imports
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route


import UserAuthenticationActor._
//#json-formats and marshalling
import spray.json.DefaultJsonProtocol
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

//For the purposes of this class, our userAuthenticationActor Parameter is defined with type ActorRef[command], meaning that
//any actor instance who responds to message of type Command could be used in place of our UserAuthenticationActor object.
/*This class uses the Ask Pattern (which requires implicit system and Timeout parameters) to send the 
* Post and Get Requests to our UserAuthentication Actor.*/
class UserRoutes(userAuthenticationActor : ActorRef[Command])(implicit val system: ActorSystem[_]) extends DefaultJsonProtocol {

  /*Marshalling - allows for the conversion of high level objects to their equivalent JSON representations*/
  implicit val userJsonFormat = jsonFormat3(User)
  implicit val newUserJsonFormat = jsonFormat2(NewUser)
  implicit val ResponseJsonFormat = jsonFormat1(Response)
  implicit val GetUserResponseJsonFormat = jsonFormat2(GetUserResponse)

  private implicit val timeout : Timeout  = Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))


  def getUser(user_uuid: String): Future[GetUserResponse] = {
    userAuthenticationActor.ask(ref => GetUser(user_uuid, ref))
  }
  def createUser(newUser: NewUser): Future[Response] = {
    userAuthenticationActor.ask(ref => CreateUser(newUser.username, newUser.password, ref))
  }



  val userRoutes: Route = {
    pathPrefix("login"){
      concat(
        pathEnd{
          post{
            entity(as[NewUser]){ newUser =>
              onSuccess(createUser(newUser)) { response =>
                complete((StatusCodes.OK) -> response)
              }
            }
          }
        },
        path(Segment) { user_uuid =>
          get {
            rejectEmptyResponse {
              onSuccess(getUser(user_uuid)) { userResponse => {
                userResponse.maybeUser match {
                  case Some(user) => {
                    complete(StatusCodes.OK, userResponse.response)
                  }
                  case None => {
                    complete(StatusCodes.NotFound, userResponse.response)
                  }
                }
              }
              }
            }
          }
        }
      )
    }
  }
}