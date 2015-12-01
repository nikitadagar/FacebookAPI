package ufl

import scala.util.{ Success, Failure } 
import akka.actor._
import ufl.FacebookAPI._
import spray.client.pipelining._
import spray.httpx.SprayJsonSupport._
import spray.http._
import spray.routing._
import spray.json._
import akka.io.IO
import spray.can.Http
import java.nio.file.{Files, Paths}
import scala.concurrent._
import duration._
// import scala.concurrent.ExecutionContext.Implicits.global
case object execute
case object init
case class startClient(numOfUsers:Int, heavyUsers:Int)

class Client extends Actor {
  def receive = {
    case startClient(numOfUsers, heavy) => {
      var heavyUsers = heavy
      val actorSystem = ActorSystem("user-system")
      //todo percentages
      for(userNumber <- 0 to numOfUsers - 1) {
        if(heavyUsers > 0) {
          val user = actorSystem.actorOf(Props(new UserActor(true)), "User" + userNumber)  
          user ! execute
        } else {
          val user = actorSystem.actorOf(Props(new UserActor(false)), "User" + userNumber)
          user ! execute
        }
        heavyUsers = heavyUsers - 1
      }
    }
  }  
}

class UserActor(isHeavy:Boolean) extends Actor {
  val system = ActorSystem("ClientSystem")
  val userTimeout = 2 seconds
  import system.dispatcher
  import ufl.FacebookAPI._
  var id:String = _

  def receive = {
    case `execute` => {
      createUser
      getUser
    }
  }

  def createUser = {
    val pipeline = sendReceive ~> unmarshal[String]
    var user: User = new User(self.path.name + "@actors.com", "chotu", "baby", "B")
    val responseFuture = pipeline(Post("http://localhost:5000/user", user))
    val result = Await.result(responseFuture, userTimeout)
    id = result.substring(result.indexOf(":") + 1).trim()
    println("new user has id: " + id)
  }

  def getUser = {
    val pipeline = sendReceive ~> unmarshal[UserResponse]
    val responseFuture = pipeline(Get("http://localhost:5000/user/1"))
    val result: UserResponse = Await.result(responseFuture, userTimeout)
    println("Get User " + result.email)
  }

  def createPost = {
    val pipeline = sendReceive ~> unmarshal[String]
    var fbpost: FBPost = new FBPost(id, "my name is " + self.path.name + " and I'm so cool.")
    println("creating new fbpost")
    val responseFuture = pipeline(Post("http://localhost:5000/post", fbpost))
    val result = Await.result(responseFuture, userTimeout)
  }

  def getAllPosts = {
    val pipelineUser = sendReceive ~> unmarshal[UserResponse]
    val pipelinePosts = sendReceive ~> unmarshal[PostResponse]
    val UserResponseFuture = pipelineUser(Get("http://localhost:5000/user/1"))

    UserResponseFuture onComplete {
      case Success(UserResponse(id, email, first_name, last_name, gender, posts, albums, friends)) =>

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
