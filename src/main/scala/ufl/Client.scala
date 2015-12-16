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
import javax.crypto.SecretKey
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

      val newUserId: String = createUser

      // getAllPosts(newUserId, newUserId) //view all your own posts
      // createAlbum //create a new album
      // uploadPhoto(newUserId) //upload a photo to the album
      // addRandomFriend(newUserId) //add a few friends
      createPost(newUserId, getShareWithArray("allFriends", newUserId))
      createPost(newUserId, getShareWithArray("allFriendswho", newUserId))
      // addRandomFriend(newUserId) //add a few friends
      // createPost(newUserId, "allFriends")
      // addRandomFriend(newUserId)
      // addRandomFriend(newUserId)
      getAllFriendsPost(newUserId, newUserId) //view your friends posts
      // uploadPhoto(newUserId, getShareWithArray("allFriends", newUserId)) //upload another photo
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

  def createPost(userId: String, shareWithArray: Vector[String]) = {
    val pipeline = sendReceive ~> unmarshal[String]
    var postContent = "my name is " + self.path.name + " and I'm so cool."
    var key: SecretKey = getSymKey()
    var authUsers = getPublicKeyMap(shareWithArray, key)
    if(authUsers.size > 0) {
      postContent = encryptSym(postContent, key)
    }
    var fbpost: FBPost = new FBPost(userId, postContent, authUsers, getSignedAuth(userId))
    val responseFuture = pipeline(Post("http://localhost:5000/post", fbpost))
    val result = Await.result(responseFuture, userTimeout)

    println("[CLIENT] creating new fbpost")
    println(result)
  }

  def getAllPosts(userId: String, requesterId: String) = {
    //if userId not same as my own id, then get that users public key
    val userResponse: UserResponse = getUser(userId)
    val allPostsIds: Vector[String] = userResponse.posts
    var pipeline = sendReceive ~> unmarshal[PostResponse]

    for(id <- allPostsIds) {
      var responseFuture = pipeline(Get("http://localhost:5000/post/" + id + "?requesterId=" + requesterId))
      var result: PostResponse = Await.result(responseFuture, userTimeout)
      
      //decrypt with your private key, and get the AES key.
      val encryptedKey:String = result.encryptedKey
      val aesKeyString:String = decryptAsym(encryptedKey, privateKey)
      val aesKey:SecretKey = stringToSecretKey(aesKeyString)

      //decrypt content with AES key.
      val postContent = decryptSym(result.content, aesKey)
      println("[CLIENT] Post: " + postContent)
    }
  }

  def getAllFriendsPost(userId: String, requesterId: String) = {
    val userResponse: UserResponse = getUser(userId)
    val allFriendsIds: Vector[String] = userResponse.friends
    var pipeline = sendReceive ~> unmarshal[PostResponse]

    for(friendId <- allFriendsIds) {
      getAllPosts(friendId, requesterId)
    }
  }

  //gets all pictures of a random friend from given user's friends list
  def getAlbumOfFriend(userId: String, requesterId: String) = {
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
            getPhoto(photoId, requesterId)
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
      println("result for add friend: " + result)
    }
  }

  def uploadPhoto(userId:String, shareWithArray: Vector[String]) = {
  	val user = getUser(userId)
  	val rand = new Random(System.currentTimeMillis());
    var caption = self.path.name + " is travelling!"
  	val len = user.albums.length
  	if(len > 0) {
      //choose a random album id to upload to
	  	val random_index = rand.nextInt(len);
	  	val albumId = user.albums(random_index);
	  	//converting photo to a byte array
	  	var photoArray: Array[Byte] = Files.readAllBytes(Paths.get("img/download.jpeg"))
      var key: SecretKey = getSymKey()
      var authUsers = getPublicKeyMap(shareWithArray, key)
      if(authUsers.size > 0) {
        photoArray = encryptSym(photoArray, key)
        caption = encryptSym(caption, key)
      }

	  	var photo:Photo = new Photo(caption, albumId, userId, photoArray, authUsers)
	  	val pipeline = sendReceive ~> unmarshal[String]
	  	val responseFuture = pipeline(Post("http://localhost:5000/photo", photo))
	  	val result = Await.result(responseFuture, userTimeout)
	  	println("[CLIENT] Photo " + result)

	  }
	  else {
	  	createAlbum
	  }
  }

  def getPhoto(id: String, requesterId: String) = {
  	val pipeline = sendReceive ~> unmarshal[PhotoResponse]
    val responseFuture = pipeline(Get("http://localhost:5000/photo/" + id + "?requesterId=" + requesterId))
    val result = Await.result(responseFuture, userTimeout)

    //decrypt key with your private key
    val encryptedKey:String = result.encryptedKey
    val aesKeyString:String = decryptAsym(encryptedKey, privateKey)
    val aesKey: SecretKey = stringToSecretKey(aesKeyString)

    //then decrypt photo and caption
    val photoConent: Array[Byte] = decryptSym(result.photo, aesKey)
    val photoCaption: String = decryptSym(result.caption, aesKey)

    println("[CLIENT] Photo received with caption " + photoCaption)
  }

  def createAlbum(creatorId:String, shareWithArray: Vector[String]) = {
  	// TODO: change creator ID
    var albumName: String = self.path.name + "'s photo album"
    var albumCaption: String = self.path.name + "'s travel journal"
    var authUsers = getPublicKeyMap(shareWithArray, key)
    if(authUsers.size > 0) {
      albumName = encryptSym(albumName, key)
      albumCaption = encryptSym(albumCaption, key)
    }

  	var album:Album = new Album(albumName, albumCaption, creatorId, authUsers)

  	val pipeline = sendReceive ~> unmarshal[String]
  	val responseFuture = pipeline(Post("http://localhost:5000/album", album))
  	val result = Await.result(responseFuture, userTimeout)
  	println("[CLIENT] Album " + result)
  }

  def getAlbum (albumId:String): AlbumResponse = {
  	val pipeline = sendReceive ~> unmarshal[AlbumResponse]
    val responseFuture = pipeline(Get("http://localhost:5000/album/" + albumId))
    val result = Await.result(responseFuture, userTimeout)
    println("[CLIENT] Album received with id " + result.id)

    //decrypt with your private key, and get the AES key.
    val encryptedKey:String = result.encryptedKey
    val aesKeyString:String = decryptAsym(encryptedKey, privateKey)
    val aesKey:SecretKey = stringToSecretKey(aesKeyString)

    //then decrypt album name and album caption
    val albumName: String = decryptSym(result.name, aesKey)
    val albumCaption: String = decryptSym(result.caption, aesKey)

    val resultResponse = new AlbumResponse(result.id, result.count, albumName, 
      albumCaption, result.creatorId, result.created_time, result.photos, result.encryptedKey)
    return resultResponse
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
  }

  //Asymmetrically Encrypts input, and converts encrypted byte array to base64 encoded string.
  def encryptAsym(text: String, key: PublicKey): String = {
    val cipher = Cipher.getInstance("RSA") //can be RSA, DES, AES
    cipher.init(Cipher.ENCRYPT_MODE, key)
    val encryptedBytes: Array[Byte] = cipher.doFinal(text.getBytes())
    val encryptedString: String = Base64.getEncoder().encodeToString(encryptedBytes)
    encryptedString
  }

  def encryptAsym(text: String, key: PrivateKey): String = {
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
  def getSymKey(): SecretKey = {
    var secretKey: SecretKey = KeyGenerator.getInstance("AES").generateKey();
    return secretKey
  }

  //AES symmetric encryption
  def encryptSym(text: String, symkey: SecretKey): String = {
    var c: Cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING")
    c.init(Cipher.ENCRYPT_MODE, symkey)
    var encryptedBytes: Array[Byte] = c.doFinal(text.getBytes())
    val encryptedString: String = Base64.getEncoder().encodeToString(encryptedBytes)
    return encryptedString
  }

  //AES symmetric encryption for a byte array.
  def encryptSym(bytes: Array[Byte], symkey: SecretKey): Array[Byte] = {
    var c: Cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING")
    c.init(Cipher.ENCRYPT_MODE, symkey)
    var encryptedBytes: Array[Byte] = c.doFinal(bytes)
    return encryptedBytes
  }

  //AES symmetric decryption
  def decryptSym(text:String, symkey: SecretKey): String = {
    var c: Cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING")
    c.init(Cipher.DECRYPT_MODE, symkey)
    val decodedText = Base64.getDecoder().decode(text)
    var decryptedBytes: Array[Byte] = c.doFinal(decodedText)
    val decryptedString: String = new String(decryptedBytes)
    return decryptedString
  }

  //AES symmetric decryption for a byte array
  def decryptSym(bytes: Array[Byte], symkey: SecretKey): Array[Byte] = {
    var c: Cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING")
    c.init(Cipher.DECRYPT_MODE, symkey)
    var decryptedBytes: Array[Byte] = c.doFinal(bytes)
    return decryptedBytes
  }

  //Returns base64 encoded string of a symmetric key.
  def secretKeyToString(symkey: SecretKey): String = {
    // get base64 encoded version of the key
    val key:String = Base64.getEncoder().encodeToString(symkey.getEncoded());
    return key
  }

  //Given a base64 encoded string of a SecretKey, returns the original key
  def stringToSecretKey(symkey: String): SecretKey = {
    val decodedKey: Array[Byte] = Base64.getDecoder().decode(symkey)
    val key:SecretKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES")
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

  def getPublicKeyMap(shareWith: Vector[String], keyToEncrypt: SecretKey): Map[String, String] = {
    var keyMap:Map[String, String] = Map[String, String]()
    if(shareWith.length == 0) {
      return keyMap
    } else {
      for(userId <- shareWith) {
        var friendUser:UserResponse = getUser(userId)
        var encryptedKey:String = encryptAsym(secretKeyToString(keyToEncrypt), stringToPublicKey(friendUser.publicKey))
        keyMap += (userId -> encryptedKey)
      }
      return keyMap
    }
  }

  //Generates a list of frends
  def getShareWithArray(shareWith: String, creatorId: String): Vector[String] = {
    var shareWithArray = Vector[String]()
    if(shareWith.equals("public")) {
      return shareWithArray
    } else if(shareWith.equals("allFriends")) {
      val userResponseAll = getUser(creatorId)
      shareWithArray = userResponseAll.friends
      shareWithArray = shareWithArray :+ creatorId
      return shareWithArray
    } else if(shareWith.equals("randomFriends")) {
      val userResponse = getUser(creatorId)
      var allFriends = userResponse.friends

      //get a random number between 0 to allFriends.length
      val rand = new Random(System.currentTimeMillis());
      val randomNumber = rand.nextInt(allFriends.length)

      //pick those many random friends, and add their ids to sharewith list
      for(i <- 0 to randomNumber) {
        var randomIndex = rand.nextInt(allFriends.length)
        shareWithArray = shareWithArray :+ allFriends(randomIndex)
      }
      shareWithArray = shareWithArray :+ creatorId //add yourself to list of people authorized to view this post.
      return shareWithArray
    }
    return shareWithArray
  }

  def getSignNumber(userId:String) = {
    val pipeline = sendReceive ~> unmarshal[String]
    val responseFuture = pipeline(Get("http://localhost:5000/signNumber/" + userId))
    val result = Await.result(responseFuture, userTimeout)
    result
  }

  def getSignedAuth(userId:String) : String = {
    var number: String = getSignNumber(userId)
    var encrypNum = encryptAsym(number, privateKey)
    encrypNum
  }

}
