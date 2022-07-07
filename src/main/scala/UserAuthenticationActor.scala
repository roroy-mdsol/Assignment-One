package com.AssignmentOne

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import slick.lifted.{Rep, Tag}

//Database operation imports
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

//Concurrency and handling Futures imports
import scala.concurrent.Future
import scala.util.{Failure, Success}

//UUID generation
import java.util.UUID;

//Case class defining a user entry (without a newly generated user_uuid - what the user sends as a post request)
final case class NewUser(username: String, password: String)
//Case class defining an authenticated user - one with a uuid and is stored in the database
final case class User(user_uuid: String, username: String, password: String)

//Case classes defining a Response objects (the message returned to the user)
case class Response(message: String)
case class GetUserResponse(maybeUser: Option[User], response : Response)

//Defining the structure of the MySQL table using slick.
class Users(tag: Tag) extends Table[(User)](tag, "Users") {
  def user_uuid: Rep[String] = column[String]("user_uuid", O.PrimaryKey, O.SqlType("VARCHAR(36)"))

  def username: Rep[String] = column[String]("username")

  def password: Rep[String] = column[String]("password")

  def * = (user_uuid, username, password) <> (User.tupled, User.unapply)
}

/*Here we define the UserAuthenticationActor.
*It receives the messages representing our POST and GET requests and responds accordingly.*/
object UserAuthenticationActor {
  import scala.concurrent.ExecutionContext
  /*Note: It's good practice to define the classes representing the messages an actor can respond to within that actor object
  We define all such messages as case classes to allow us to perform pattern matching on them*/


  sealed trait Command //Super class of the two messages that our UserAuthenticationActor can receive
  final case class CreateUser(username: String, password: String, replyTo: ActorRef[Response]) extends Command //CreateUser message - POST method - subclass of command
  final case class GetUser(name: String, replyTo: ActorRef[GetUserResponse]) extends Command //GetUser message - GET method - subclass of command


  val db = Database.forConfig("db") //Initialise database object. Connection to the database is defined in Applcation.conf

  val users = TableQuery[Users] //Representation of the Users table in our database.

  /*Here we define the actor's initial behaviour, in which we create the User table if it does not exist.
  * A Behaviour[T] object defines the way in which our Actor responds to a message of type T. Hence, whenever
  * our UserAuthenticationActor receives a Command message, it will trigger some sort of response.*/
  def apply()(implicit ec : ExecutionContext) : Behavior[Command] = {
    db.run(
      users.schema.createIfNotExists
    )
    receiveMessage(users)
  }

  private def receiveMessage(users: TableQuery[Users])(implicit ec: ExecutionContext): Behavior[Command] =
    Behaviors.receiveMessage {

      case CreateUser(username, password, replyTo) =>
        val q2 = users.filter(_.username === username)
        val action2 = q2.result
        val result2: Future[Seq[(User)]] = db.run(action2)

        result2 onComplete{
          case Success(returnedUsers) =>
            if (returnedUsers.length >= 1) {
              replyTo ! Response(s"Username $username already exists")
            }
            else {
              val uuid : String = UUID.randomUUID().toString()
              replyTo ! Response(s"User $username created with user_uuid $uuid")
              val action = users.insertOrUpdate(User(uuid, username, password))
              db.run(action)
            }
          case Failure(exception) => replyTo ! Response(exception.getMessage)
        }
        Behaviors.same

     case GetUser(user_uuid, replyTo: ActorRef[GetUserResponse]) =>
        val q = users.filter(_.user_uuid === user_uuid) //q is a query
        val action = q.result
        val result: Future[Seq[(User)]] = db.run(action)

        /*On successful completion of this future, we'll receive a sequence of users. If the length of this sequence isn't 1 then
        * we have an error and send the appropriate response*/
        result onComplete{
          case Success(users) => replyTo ! (if (users.length == 1) GetUserResponse(Option(users(0)), Response(s"Welcome, ${users(0).username}")) else GetUserResponse(None, Response(s"User with uuid $user_uuid not found")))
          case Failure(exception) => replyTo ! GetUserResponse(None, Response(exception.getMessage))
        }
        Behaviors.same
    }
}
