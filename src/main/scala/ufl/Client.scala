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
case class startClient(numOfUsers:Int)

class Client extends Actor {
  def receive = {
    case startClient(numOfUsers) => {
      val actorSystem = ActorSystem("user-system")
      //todo percentages
      for(userNumber <- 0 to numOfUsers - 1) {
        val user = actorSystem.actorOf(Props(new UserActor()), "User" + userNumber)  
        user ! execute
      }
    }
  }  
}

class UserActor extends Actor {
  val system = ActorSystem("ClientSystem")
  val userTimeout = 4 seconds
  import system.dispatcher
  import ufl.FacebookAPI._

  def receive = {
    case `execute` => {
      // for (i <- 0 to 10) {

      // }
      val newUserId: String = createUser

      createPost(newUserId) //create a few new posts
      createPost(newUserId)
      getAllPosts(newUserId) //view all your own posts
      createAlbum //create a new album
      uploadPhoto(newUserId) //upload a photo to the album
      addRandomFriend(newUserId) //add a few friends
      addRandomFriend(newUserId)
      addRandomFriend(newUserId)
      getAllFriendsPost(newUserId) //view your friends posts
      uploadPhoto(newUserId) //upload another photo
      getAllAlbums(newUserId)
      println("done")

      self ! PoisonPill      
    }
  }

  def createUser: String = {
    val pipeline = sendReceive ~> unmarshal[String]
    var user: User = new User(self.path.name + "@actors.com", "Actor", "Scala", "M")
    val responseFuture = pipeline(Post("http://localhost:5000/user", user))
    val result = Await.result(responseFuture, userTimeout)
    var id = result.substring(result.indexOf(":") + 1).trim()
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
    
    if(allUserIds.length == 1) {
      //im the only user on the network
      return "forever alone"
    } else {
      val rand = new Random(System.currentTimeMillis());
      val random_index = rand.nextInt(allUserIds.length);
      var userId = allUserIds(random_index);
      userId = userId.replace("\"","").trim()
      return userId
    }
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

    for(id <- allPostsIds) {
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
    if(randomFriend.equals("forever alone")) {
      //you're all alone. wont add anyone
      println("[CLIENT] youre the first user. Can't add any friend for now.")
    } else {
      while(randomFriend.equals(ownerId)) {
        randomFriend = getRandomUser
      }
      var newFriend: FriendsList = new FriendsList(ownerId, randomFriend)
      println("[CLIENT] adding " + randomFriend + " as a new friend for " + ownerId)
      val responseFuture = pipeline(Post("http://localhost:5000/friendsList", newFriend))
      val result = Await.result(responseFuture, userTimeout)
    }
  }

  def uploadPhoto(userId:String) = {
  	val user = getUser(userId)
  	val rand = new Random(System.currentTimeMillis());
  	val len = user.albums.length
  	if(len > 0) {
	  	val random_index = rand.nextInt(len);
	  	val albumId = user.albums(random_index);
	  	//converting photo to a byte array
	  	var photoArray: Array[Byte] = Files.readAllBytes(Paths.get("img/download.jpeg"))
	  	// TODO: change creator ID
	  	var photo:Photo = new Photo("Caption", albumId, userId, photoArray)

	  	val pipeline = sendReceive ~> unmarshal[String]
	  	val responseFuture = pipeline(Post("http://localhost:5000/photo", photo))
	  	val result = Await.result(responseFuture, userTimeout)
	  	println("[CLIENT] Photo " + result)

	  }
	  else {
	  	createAlbum
	  }
  }

  def getPhoto(id: String) : PhotoResponse = {
  	val pipeline = sendReceive ~> unmarshal[PhotoResponse]
    val responseFuture = pipeline(Get("http://localhost:5000/photo/" + id))
    val result = Await.result(responseFuture, userTimeout)
    println("[CLIENT] Photo received with id " + result.id)
    result
  }

  def createAlbum = {
  	// TODO: change creator ID
  	var album:Album = new Album("album name", "caption of album", "1")

  	val pipeline = sendReceive ~> unmarshal[String]
  	val responseFuture = pipeline(Post("http://localhost:5000/album", album))
  	val result = Await.result(responseFuture, userTimeout)
  	println("[CLIENT] Album " + result)
  }

  def getAlbum (albumId:String) : AlbumResponse = {
  	val pipeline = sendReceive ~> unmarshal[AlbumResponse]
    val responseFuture = pipeline(Get("http://localhost:5000/album/" + albumId))
    val result = Await.result(responseFuture, userTimeout)
    println("[CLIENT] Album received with id " + result.id)
    result
  }

  def getAllAlbums(userId:String) = {
  	val user = getUser(userId)
  	val albums = user.albums
  	for(id <- albums) {
  		val album = getAlbum(id)
  		println("[CLIENT] Album id :" + id + "has photos(ids) : " + album.photos) 
  	}
  }

  def deleteAlbum(albumId:String) = {
  	val pipeline = sendReceive ~> unmarshal[String]
    val responseFuture = pipeline(Delete("http://localhost:5000/album/" + albumId))
    val result = Await.result(responseFuture, userTimeout)
    println("[CLIENT] Album deleted " + result)
    result
  }

}
