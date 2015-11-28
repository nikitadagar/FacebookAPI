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

  case class User()
  case object UserCreated
  case object UserAlreadyExists
  
  /* json (un)marshalling */
  object Page extends DefaultJsonProtocol {
    implicit val format = jsonFormat2(Page.apply)
  }

  object Post extends DefaultJsonProtocol {
    implicit val format = jsonFormat3(Post.apply)
  }
}
