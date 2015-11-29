package ufl

object FacebookAPI {
  
  import spray.json._

  case class NodeCreated(id: String)
  case class NodeNotFound(nodeType: String)

  case class Page(name: String, about: String)
  case object PageAlreadyExists
  case object PageDeleted
  case class PageResponse(id: String, name: String, about: String)
  
  case class Post(userId: String, content: String)
  case object PostDeleted
  case class PostResponse(id: String, userId: String, userName: String, content: String)

  case class User(firstname:String, lastname:String, gender:String)
  case object UserAlreadyExists
  case object UserDeleted
  case class UserResponse(id:String, first_name:String, last_name:String, gender:String, posts:Vector[String])

  case class Photo(caption:String, album:String, from:String, photo:Array[Byte])
  case object PhotoAlreadyExists
  case object PhotoDeleted
  case class PhotoResponse()
  
  /* json (un)marshalling */
  object Page extends DefaultJsonProtocol {
    implicit val format = jsonFormat2(Page.apply)
  }

  object Post extends DefaultJsonProtocol {
    implicit val format = jsonFormat2(Post.apply)
  }

  object User extends DefaultJsonProtocol {
    implicit val format = jsonFormat3(User.apply)
  }

  object Photo extends DefaultJsonProtocol {
    implicit val format = jsonFormat3(Photo.apply)
  }

  object PageResponse extends DefaultJsonProtocol {
    implicit val format = jsonFormat3(PageResponse.apply)
  }

  object PostResponse extends DefaultJsonProtocol {
    implicit val format = jsonFormat4(PostResponse.apply)
  }

  object UserResponse extends DefaultJsonProtocol {
    implicit val format = jsonFormat5(UserResponse.apply)
  }
}
