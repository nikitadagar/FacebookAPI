package ufl

object FacebookAPI {
  
  import spray.json._

  case class Page(id: String, name: String, about: String)
  case object PageCreated
  case object PageAlreadyExists
  case object PageDeleted
  
  
  case class Post(id: String, creator: String, description: String)
  case object PostCreated
  case object PostDeleted
  
  /* json (un)marshalling */
  
  object Page extends DefaultJsonProtocol {
    implicit val format = jsonFormat3(Page.apply)
  }

  object Post extends DefaultJsonProtocol {
    implicit val format = jsonFormat3(Post.apply)
  }
  
  /* implicit conversions */

  // implicit def toQuestion(quiz: Quiz): Question = Question(id = quiz.id, question = quiz.question)

  // implicit def toAnswer(quiz: Quiz): Answer = Answer(answer = quiz.correctAnswer)
}
