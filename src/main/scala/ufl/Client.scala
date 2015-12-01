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
import java.nio.file.{Files, Paths}
import scala.concurrent._
import duration._
// import scala.concurrent.ExecutionContext.Implicits.global

case object postUser
case object postPost
case object getUser
case object execute
case class startClient(numOfUsers:Int, heavyUsers:Int)

// class Client extends Actor {
//   def receive = {
//     case startClient(numOfUsers, heavy) => {
//       var heavyUsers = heavy
//       val actorSystem = ActorSystem("user-system")
//       //todo percentages
//       for(userNumber <- 0 to numOfUsers) {
//         heavyUsers = heavyUsers - 1
//         if(heavyUsers > 0) {
//           val user = actorSystem.actorOf(Props(new UserActor(true)), userNumber)  
//           user ! execute
//         } else {
//           val user = actorSystem.actorOf(Props(new UserActor(false)), userNumber)
//           user ! execute
//         }
        
//       }
//     }
//   }  
// }

class UserActor(isHeavy:Boolean) extends Actor {
  val system = ActorSystem("ClientSystem")
  import system.dispatcher
  import ufl.FacebookAPI._
  var id:String = _
  val pipeline2 = sendReceive ~> unmarshal[String]
  val pipeline1 = sendReceive ~> unmarshal[UserResponse]

  createUser
  createAlbum
  uploadPhoto

  def receive = {

    case `execute` => {

    }
  }

  def createUser = {
    var user: User = new User("nikita@babies.com", "chotu", "baby", "B")
    println("creating user")
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

  def uploadPhoto = {
  	println("CLIENT : Posting photo")
  	
  	//converting photo to a byte array
  	var photoArray: Array[Byte] = Files.readAllBytes(Paths.get("img/download.jpeg"))

  	// TODO: change creator ID
  	var photo:Photo = new Photo("Caption", "2", "1", photoArray)

  	val pipeline = sendReceive ~> unmarshal[String]
  	val responseFuture = pipeline(Post("http://localhost:5000/photo", photo))
  	val result = Await.result(responseFuture, 5 seconds)
  	println(result)

  }

  def getPhoto(id: String) = {
  	
  }

  def createAlbum = {
  	println("creating album")

  	// TODO: change creator ID
  	var album:Album = new Album("album name", "caption of album", "1")

  	val pipeline = sendReceive ~> unmarshal[String]
  	val responseFuture = pipeline(Post("http://localhost:5000/album", album))
  	val result = Await.result(responseFuture, 3 seconds)
  	println(result)
  }

}
