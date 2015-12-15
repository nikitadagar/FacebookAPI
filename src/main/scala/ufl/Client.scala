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
import java.security._
import javax.crypto._
import javax.crypto.spec.SecretKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import scala.collection.immutable.Map
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
  import system.dispatcher
  import ufl.FacebookAPI._

  val system = ActorSystem("ClientSystem")
  val userTimeout = 4 seconds

  val keyGen = KeyPairGenerator.getInstance("RSA")
  keyGen.initialize(1024)
  val keypair = keyGen.genKeyPair()
  val privateKey: PrivateKey = keypair.getPrivate()
  val publicKey: PublicKey = keypair.getPublic();

  def receive = {
    case `execute` => {
      var test = "Alok Sharma."
      // var encrypted: String = encrypt(test, privateKey)
      // println(encrypted)
      // println(decrypt(encrypted, publicKey))

      var c: Cipher = Cipher.getInstance("AES")
      val symkey: SecretKeySpec = getSymKey()
      c.init(Cipher.ENCRYPT_MODE, symkey)
      var encryptedData: Array[Byte] = c.doFinal(test.getBytes())

      c.init(Cipher.DECRYPT_MODE, symkey)
      var data: Array[Byte] = c.doFinal(encryptedData)
      println(new String(data))

      val newUserId: String = createUser

      // createPost(newUserId) //create a few new posts
      // createPost(newUserId)
      // getAllPosts(newUserId) //view all your own posts
      // createAlbum //create a new album
      // uploadPhoto(newUserId) //upload a photo to the album
      // addRandomFriend(newUserId) //add a few friends
      // addRandomFriend(newUserId)
      // addRandomFriend(newUserId)
      // getAllFriendsPost(newUserId) //view your friends posts
      // uploadPhoto(newUserId) //upload another photo
      // getAllAlbums(newUserId)
      // getAlbumOfFriend(newUserId)
      println("done")

      self ! PoisonPill      
    }
  }

  def createUser: String = {
    val pipeline = sendReceive ~> unmarshal[String]
    var user: User = new User(self.path.name + "@actors.com", "Actor", "Scala", "M", publicKeyToString(publicKey))
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

  def createPost(userId: String, shareWith: Array[String]) = {
    val pipeline = sendReceive ~> unmarshal[String]
    var postContent = "my name is " + self.path.name + " and I'm so cool."

    var key: SecretKeySpec = getSymKey()
    var keyMap = getPublicKeyMap(shareWith, key)
    if(keyMap.size > 0) {
      postContent = encryptSym(postContent, key)
    }
    var fbpost: FBPost = new FBPost(userId, postContent, keyMap)
    val responseFuture = pipeline(Post("http://localhost:5000/post", fbpost))
    val result = Await.result(responseFuture, userTimeout)

    println("[CLIENT] creating new fbpost")
  }

  def getAllPosts(userId: String) = {
    //if userId not same as my own id, then get that users public key
    val userResponse: UserResponse = getUser(userId)
    val allPostsIds: Vector[String] = userResponse.posts
    var pipeline = sendReceive ~> unmarshal[PostResponse]

    for(id <- allPostsIds) {
      var responseFuture = pipeline(Get("http://localhost:5000/post/" + id))
      var result: PostResponse = Await.result(responseFuture, userTimeout)
      //from result, iterate over keyMap, and find the key for your id.

      //decrypt with your private key, and get the AES key.

      //decrypt content with AES key.
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

  //gets all pictures of a random friend from given user's friends list
  def getAlbumOfFriend(userId: String) = {
    val userResponse: UserResponse = getUser(userId)
    val allFriendsIds: Vector[String] = userResponse.friends
    var pipeline = sendReceive ~> unmarshal[AlbumResponse]

    if (allFriendsIds.size > 0) {
      val friendId = (allFriendsIds(0)) + ""
      val friend: UserResponse = getUser(friendId)
      
      if (friend.albums.length > 0) {
        val album: AlbumResponse = getAlbum(friend.albums(0))
        if (album.photos.length > 0){
          for(photoId <- album.photos) {
            getPhoto(photoId)
          }
        }
        else {
          println("[CLIENT] No photos in album : " + album.id)
        }
      }
      else {println("[CLIENT] No Album found")}
    }
    else {
      println("[CLIENT] No friends. Can't get Album")
    }
  }

  def addRandomFriend(ownerId:String) {
    val pipeline = sendReceive ~> unmarshal[String]
    
    var randomFriend:String = getRandomUser
    if(randomFriend.equals("forever alone")) {
      //you're all alone. wont add anyone
      println("[CLIENT] you're the first user. Can't add any friend for now.")
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

  //Asymmetrically Encrypts input, and converts encrypted byte array to base64 encoded string.
  def encryptAsym(text: String, key: PublicKey): String = {
    val cipher = Cipher.getInstance("RSA") //can be RSA, DES, AES
    cipher.init(Cipher.ENCRYPT_MODE, key)
    val encryptedBytes: Array[Byte] = cipher.doFinal(text.getBytes())
    val encryptedString: String = Base64.getEncoder().encodeToString(encryptedBytes)
    encryptedString
  }

  //Converts base64 encoded input to a byte array first, and then asymmetrically decrypts.
  def decryptAsym(text: String, key: PrivateKey): String = {
    val encryptedBytes: Array[Byte] = Base64.getDecoder().decode(text);
    val cipher = Cipher.getInstance("RSA")
    cipher.init(Cipher.DECRYPT_MODE, key)
    var textBytes:Array[Byte] = cipher.doFinal(encryptedBytes)
    new String(textBytes)
  }

  //Generates a new random AES key for symmetric encryption.
  def getSymKey(): SecretKeySpec = {
    val random: SecureRandom  = new SecureRandom()
    val randomBytes = new Array[Byte](16)
    random.nextBytes(randomBytes)
    val k: SecretKeySpec = new SecretKeySpec(randomBytes, "AES")
    return k
  }

  //AES symmetric encryption
  def encryptSym(text: String, symkey: SecretKeySpec): String = {
    var c: Cipher = Cipher.getInstance("AES")
    c.init(Cipher.ENCRYPT_MODE, symkey)
    var encryptedBytes: Array[Byte] = c.doFinal(text.getBytes())
    val encryptedString: String = Base64.getEncoder().encodeToString(encryptedBytes)
    return encryptedString
  }

  //AES symmetric decryption
  def decryptSym(text:String, symkey: SecretKeySpec): String = {
    var c: Cipher = Cipher.getInstance("AES");
    c.init(Cipher.DECRYPT_MODE, symkey);
    var decryptedBytes: Array[Byte] = c.doFinal(text.getBytes());
    val decryptedString: String = Base64.getEncoder().encodeToString(decryptedBytes)
    return decryptedString
  }

  //Returns base64 encoded string of a symmetric key.
  def secretKeyToString(symkey: SecretKeySpec): String = {
    // get base64 encoded version of the key
    val key:String = Base64.getEncoder().encodeToString(symkey.getEncoded());
    return key
  }

  //Given a base64 encoded string of a SecretKeySpec, returns the original key
  def stringToSecretKey(symkey: String): SecretKeySpec = {
    val key:SecretKeySpec = new SecretKeySpec(symkey.getBytes(), 0, symkey.length, "AES"); 
    return key
  }

  //Returns base64 encoded string of a public key
  def publicKeyToString(publicKey: PublicKey): String = {
    val key:String = Base64.getEncoder().encodeToString(publicKey.getEncoded());
    return key
  }

  def stringToPublicKey(publicKey: String): PublicKey = {
    var publicBytes: Array[Byte] = Base64.getDecoder().decode(publicKey);
    val keySpec: X509EncodedKeySpec = new X509EncodedKeySpec(publicBytes);
    val keyFactory: KeyFactory = KeyFactory.getInstance("RSA");
    val key: PublicKey = keyFactory.generatePublic(keySpec);
    return key
  }

  def getPublicKeyMap(shareWith: Array[String], keyToEncrypt: SecretKeySpec): Map[String, String] = {
    var keyMap = Map[String, String]()
    if(shareWith.length == 0) {
      return keyMap
    } else {
      for(userId <- shareWith) {
        var friendUser:UserResponse = getUser(userId)
        var encryptedKey:String = encryptAsym(secretKeyToString(keyToEncrypt), stringToPublicKey(friendUser.publicKey))
        keyMap += (friendUser.id -> encryptedKey)
      }
      return keyMap
    }
  }

}
