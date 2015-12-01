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
import scala.util.Random
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
  val userTimeout = 10 seconds
  import system.dispatcher
  import ufl.FacebookAPI._

  def receive = {
    case `execute` => {

      for (i <- 0 to 10) {

      }
      val newUserId: String = createUser("1")
      createUser("2")
      createUser("3")
      addRandomFriend(newUserId)
      // createPost(newUserId)
      // createPost(newUserId)
      // getAllPosts(newUserId)
      // createAlbum
      // uploadPhoto("1", "2")
      // getPhoto("3")
    }
  }

  def createUser: String = {
    val pipeline = sendReceive ~> unmarshal[String]
    var user: User = new User(self.path.name + "@actors.com", "Actor", "Scala", "M")
    val responseFuture = pipeline(Post("http://localhost:5000/user", user))
    val result = Await.result(responseFuture, userTimeout)
    id = result.substring(result.indexOf(":") + 1).trim()
    println("[CLIENT] new user has id: " + id)
    return id
  }

  def getUser(userId:String): UserResponse = {
    val pipeline = sendReceive ~> unmarshal[UserResponse]
    val responseFuture = pipeline(Get("http://localhost:5000/user/" + userId))
    val result: UserResponse = Await.result(responseFuture, userTimeout)
    println("[CLIENT] Get User " + result.email)
    return result
  }

  def getRandomUser: String = {
    val pipeline = sendReceive ~> unmarshal[String]
    val responseFuture = pipeline(Get("http://localhost:5000/AllUsers"))
    val result = Await.result(responseFuture, userTimeout)
    var allUserIds: Array[String] = result.substring(1, result.length -1).split(",")
    val rand = new Random(System.currentTimeMillis());
    val random_index = rand.nextInt(allUserIds.length);
    var userId = allUserIds(random_index);
    userId = userId.replace("\"","").trim()
    return userId
  }

  def createPost(userId: String) = {
    val pipeline = sendReceive ~> unmarshal[String]
    var fbpost: FBPost = new FBPost(userId, "my name is " + self.path.name + " and I'm so cool.")
    println("[CLIENT] creating new fbpost")
    val responseFuture = pipeline(Post("http://localhost:5000/post", fbpost))
    val result = Await.result(responseFuture, userTimeout)
  }

  def getAllPosts(userId: String) = {
    val userResponse: UserResponse = getUser(userId)
    val allPostsIds: Vector[String] = userResponse.posts
    var pipeline = sendReceive ~> unmarshal[PostResponse]

    for(importd <- allPostsIds) {
      var responseFuture = pipeline(Get("http://localhost:5000/post/" + id))
      var result: PostResponse = Await.result(responseFuture, userTimeout)
      println("[CLIENT] Post: " + result.content)
    }
  }

  def getAllFriendsPost(userId: String) = {
    val userResponse: UserResponse = getUser(userId)
    val allFriendsIds: Vector[String] = userResponse.friends
    var pipeline = sendReceive ~> unmarshal[PostResponse]

    for(friendId <- allFriendsIds) {
      getAllPosts(friendId)
    }
  }

  def addRandomFriend(ownerId:String) {
    val pipeline = sendReceive ~> unmarshal[String]
    
    var randomFriend:String = getRandomUser
    while(randomFriend.equals(ownerId)) {
      randomFriend = getRandomUser //make sure I'm not trying to add myself as a friend :(  
    }

    var newFriend: FriendsList = new FriendsList(ownerId, randomFriend)
    println("[CLIENT] adding " + randomFriend + " as a new friend for " + ownerId)
    val responseFuture = pipeline(Post("http://localhost:5000/friendsList", newFriend))
    val result = Await.result(responseFuture, userTimeout)
  }

  def uploadPhoto(userId:String, albumId:String) = {
  	
  	println("[CLIENT] Posting photo with album ID " + albumId)
  	//converting photo to a byte array
  	var photoArray: Array[Byte] = Files.readAllBytes(Paths.get("img/download.jpeg"))
  	// TODO: change creator ID
  	var photo:Photo = new Photo("Caption", albumId, userId, photoArray)

  	val pipeline = sendReceive ~> unmarshal[String]
  	val responseFuture = pipeline(Post("http://localhost:5000/photo", photo))
  	val result = Await.result(responseFuture, userTimeout)
  	println("[CLIENT] Photo " + result)
  }

  def getPhoto(id: String) = {
  	val pipeline = sendReceive ~> unmarshal[PhotoResponse]
    val responseFuture = pipeline(Get("http://localhost:5000/photo/" + id))
    val result = Await.result(responseFuture, userTimeout)
    println("[CLIENT] Photo received with id " + result.id)
  }

  def createAlbum = {
  	// TODO: change creator ID
  	var album:Album = new Album("album name", "caption of album", "1")

  	val pipeline = sendReceive ~> unmarshal[String]
  	val responseFuture = pipeline(Post("http://localhost:5000/album", album))
  	val result = Await.result(responseFuture, userTimeout)
  	println("[CLIENT] Album " + result)
  }

  def getAllAlbums() = {
  	// val albumsList = 
  }
}
