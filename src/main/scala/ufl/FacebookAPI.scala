package ufl

import scala.collection.immutable.Map

object FacebookAPI {
  
  import spray.json._

  case class NodeCreated(id: String)
  case class NodeNotFound(nodeType: String)
  case class SignNumberRequest(number: String)
  case object AuthFailed

  case class Page(name: String, about: String)
  case object PageDeleted
  case class PageResponse(id: String, name: String, about: String)
  
  case class FBPost(userId: String, content: String, authUsers: Map[String, String], auth:String)
  case object PostDeleted
  case class PostResponse(id: String, userId: String, content: String, encryptedKey: String)

  case class User(email: String, firstname:String, lastname:String, gender:String, publicKey:String)
  case object UserAlreadyExists
  case object UserDeleted
  case class UserResponse(id:String, publicKey:String, email: String, first_name:String, last_name:String, gender:String, posts:Vector[String], albums:Vector[String], friends:Vector[String])

  case class Photo(caption:String, albumId:String, creatorId:String, photo:Array[Byte], authUsers: Map[String, String])
  case object PhotoDeleted
  case class PhotoResponse(id:String, caption:String, album:String, from:String, photo:Array[Byte], encryptedKey: String)

  case class Album(name:String, caption: String, creatorId: String, authUsers: Map[String, String])
  case object AlbumDeleted
  case class AlbumResponse(id: String, count: Int, name:String, caption: String, creatorId: String, created_time:String, photos: Vector[String], encryptedKey: String)

  case class FriendsList(owner:String, friend:String)
  case object FriendsListUpdated
  case object FriendExists
  
  /* json (un)marshalling */
  object Page extends DefaultJsonProtocol {
    implicit val format = jsonFormat2(Page.apply)
  }

  object FBPost extends DefaultJsonProtocol {
    implicit val format = jsonFormat4(FBPost.apply)
  }

  object User extends DefaultJsonProtocol {
    implicit val format = jsonFormat5(User.apply)
  }

  object Photo extends DefaultJsonProtocol {
    implicit val format = jsonFormat5(Photo.apply)
  }

  object Album extends DefaultJsonProtocol {
    implicit val format = jsonFormat4(Album.apply)
  }

  object FriendsList extends DefaultJsonProtocol {
    implicit val format = jsonFormat2(FriendsList.apply)
  }

  object PageResponse extends DefaultJsonProtocol {
    implicit val format = jsonFormat3(PageResponse.apply)
  }

  object PostResponse extends DefaultJsonProtocol {
    implicit val format = jsonFormat4(PostResponse.apply)
  }

  object UserResponse extends DefaultJsonProtocol {
    implicit val format = jsonFormat9(UserResponse.apply)
  }

  object PhotoResponse extends DefaultJsonProtocol {
    implicit val format = jsonFormat6(PhotoResponse.apply)
  }

  object AlbumResponse extends DefaultJsonProtocol {
    implicit val format = jsonFormat7(AlbumResponse.apply)
  }
}
