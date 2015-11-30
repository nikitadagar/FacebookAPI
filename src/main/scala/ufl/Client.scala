package ufl

import scala.util.{ Success, Failure } 
import akka.actor._
import ufl.FacebookAPI._
import spray.client.pipelining._
import spray.httpx.SprayJsonSupport._
import spray.http._
import spray.json._
import akka.io.IO
import spray.can.Http
// import scala.concurrent.ExecutionContext.Implicits.global

case object postUser
case object postPost
case object getUser
case object execute
case class startClient(numOfUsers:Int, heavyUsers:Int)

class Client extends Actor {
  def receive = {
    case startClient(numOfUsers, heavy) => {
      var heavyUsers = heavy
      val actorSystem = ActorSystem("user-system")
      //todo percentages
      for(userNumber <- 0 to numOfUsers) {
        heavyUsers = heavyUsers - 1
        if(heavyUsers > 0) {
          val user = actorSystem.actorOf(Props(new UserActor(true)), userNumber)  
          user ! execute
        } else {
          val user = actorSystem.actorOf(Props(new UserActor(false)), userNumber)
          user ! execute
        }
        
      }
    }
  }  
}

class UserActor(isHeavy:Boolean) extends Actor {
  val system = ActorSystem("ClientSystem")
  import system.dispatcher
  import ufl.FacebookAPI._
  var id:String = _
  val pipeline2 = sendReceive ~> unmarshal[String]
  val pipeline1 = sendReceive ~> unmarshal[UserResponse]

  createUser

  def receive = {

    case `execute` => {

    }
  }

  def createUser = {
    var user: User = new User("nikita@babies.com", "chotu", "baby", "B")
    println("sent post")
    val responseFuture = pipeline2(Post("http://localhost:5000/user", user))
    responseFuture onComplete {
      case Success(result) =>
        println(result)
    }
  }

  def getUser = {
    println("get user")
    val responseFuture1 = pipeline1(Get("http://localhost:5000/user/1"))

    responseFuture1 onComplete {
      case Success(UserResponse(id, email, first_name, last_name, gender, posts, albums, friends)) =>
        println(email)
    }
  }
}
