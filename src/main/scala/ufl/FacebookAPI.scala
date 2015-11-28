package ufl

object FacebookAPI {
  
  import spray.json._

  case class Page(name: String, about: String)
  case object PageCreated
  case object PageAlreadyExists
  case object PageDeleted
  case object PageNotFound
  
  case class Post(userId: String, content: String)
  case object PostCreated
  case object PostDeleted
  case object PostNotFound

  case class User(firstname:String, lastname:String, gender:String)
  case object UserCreated
  case object UserAlreadyExists
  case object UserDeleted
  
  /* json (un)marshalling */
  object Page extends DefaultJsonProtocol {
    implicit val format = jsonFormat2(Page.apply)
  }

  object Post extends DefaultJsonProtocol {
    implicit val format = jsonFormat2(Post.apply)
  }
}
