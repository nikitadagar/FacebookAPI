package ufl

import akka.actor._
import akka.util.Timeout
import spray.http.StatusCodes
import spray.httpx.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._
import spray.routing._
import spray.json._

import scala.concurrent.duration._
import scala.language.postfixOps
import ufl.FacebookAPI._
import scala.collection.immutable.Map

class RestInterface extends HttpServiceActor
  with RestApi {
  def receive = runRoute(routes)
}

object RestApi {
  var id = 0;
  private def getId = {
    id = id + 1
    id.toString
  }

  var pageList = Vector[PageNode]()
  var postList = Vector[PostNode]()
  var userList = Vector[UserNode]()
  var photoList = Vector[PhotoNode]()
}


trait RestApi extends HttpService with ActorLogging { actor: Actor =>
  import ufl.FacebookAPI._

  implicit val timeout = Timeout(10 seconds)

  def routes: Route =
    
    pathPrefix("page") {
      pathEnd {
        post {
          entity(as[Page]) { page => requestContext =>
            var newPageId = RestApi.getId
            val pageNode: PageNode = new PageNode(newPageId, page.name, page.about)
            RestApi.pageList = RestApi.pageList :+ pageNode

            println("Created new page: " + pageNode.name + ", id: " + pageNode.id)
            val responder = createResponder(requestContext)
            responder ! NodeCreated(newPageId)
          }
        }
      } ~
      path(Segment) { id =>
        delete { requestContext =>
          println("delete page " + id)
          val responder = createResponder(requestContext)
          var resultPage: Option[PageNode] = RestApi.pageList.find(_.id == id)
          if(resultPage.isEmpty) {
            responder ! NodeNotFound("Page")
          } else {
            RestApi.pageList = RestApi.pageList.filterNot(_.id == id)
            responder ! PageDeleted
          }
        } ~
        get { requestContext =>
          println("get page " + id)
          var resultPage: Option[PageNode] = RestApi.pageList.find(_.id == id)
          val responder = createResponder(requestContext)
          resultPage.map(responder ! _.pageResponse())
            .getOrElse(responder ! NodeNotFound("Page"))
        }
      }
    } ~
    pathPrefix("post") {
      pathEnd {
        post {
          entity(as[Post]) { post => requestContext =>
            var newPostId = RestApi.getId
            val responder = createResponder(requestContext)

            val resultUser: Option[UserNode] = RestApi.userList.find(_.id == post.userId)
            if(resultUser.isEmpty) {
              //invalid user id
              responder ! NodeNotFound("User")
            } else {
              //Valid user, create a new post.
              val postNode: PostNode = new PostNode(newPostId, resultUser.get, post.content)
              RestApi.postList = RestApi.postList :+ postNode
              resultUser.get.postList = resultUser.get.postList :+ postNode.id
              println("Created new post by: " + resultUser.get.id + ", id: " + postNode.id + ", posts: " + resultUser.get.postList.length)
              responder ! NodeCreated(newPostId)
            }
          }
        }
      } ~
      path(Segment) { id =>
        delete { requestContext =>
          println("delete user " + id)
          val responder = createResponder(requestContext)
          var resultPost: Option[PostNode] = RestApi.postList.find(_.id == id)
          if(resultPost.isEmpty) {
            responder ! NodeNotFound("Post")
          } else {
            RestApi.postList = RestApi.postList.filterNot(_.id == id)
            responder ! PostDeleted
          }
        } ~
        get { requestContext =>
          println("get post " + id)
          var resultPost: Option[PostNode] = RestApi.postList.find(_.id == id)
          val responder = createResponder(requestContext)
          resultPost.map(responder ! _.postResponse())
            .getOrElse(responder ! NodeNotFound("Post"))
        }
      }
    } ~
    pathPrefix("user") {
      pathEnd {
        post {
          entity(as[User]) { user => requestContext =>
            var newUserId = RestApi.getId
            val userNode: UserNode = new UserNode(newUserId, user.firstname, user.lastname, user.gender)
            RestApi.userList = RestApi.userList :+ userNode
            // TODO: send user already exists error code
            println("Created new user with id: " + userNode.id)
            val responder = createResponder(requestContext)
            responder ! NodeCreated(newUserId)
          }
        }
      } ~
      path(Segment) { id =>
        delete { requestContext =>
          println("delete user " + id)
          val responder = createResponder(requestContext)
          var resultUser: Option[UserNode] = RestApi.userList.find(_.id == id)
          if(resultUser.isEmpty) {
            responder ! NodeNotFound("User")
          } else {
            RestApi.userList = RestApi.userList.filterNot(_.id == id)
            responder ! UserDeleted
          }
        } ~
        get { requestContext =>
          println("get user " + id)
          var resultUser: Option[UserNode] = RestApi.userList.find(_.id == id)
          val responder = createResponder(requestContext)
          resultUser.map(responder ! _.userResponse())
            .getOrElse(responder ! NodeNotFound("User"))
        }
      }
    }~
    pathPrefix("photo") {
      pathEnd {
        post {
          entity(as[Photo]) { photo => requestContext =>
            var newPhotoId = RestApi.getId
            val photoNode: PhotoNode = new PhotoNode(newPhotoId, photo.caption, photo. album, photo.from)
            RestApi.photoList = RestApi.photoList :+ photoNode
            // TODO: send photo already exists error code
            println("Created new photo with id: " + photoNode.id)
            val responder = createResponder(requestContext)
            responder ! NodeCreated(newPhotoId)
          }
        }
      } ~
      path(Segment) { id =>
        delete { requestContext =>
          // println("delete user " + id)
          // val responder = createResponder(requestContext)
          // var resultUser: Option[UserNode] = RestApi.userList.find(_.id == id)
          // if(resultUser.isEmpty) {
          //   responder ! UserNotFound
          // } else {
          //   RestApi.userList = RestApi.userList.filterNot(_.id == id)
          //   responder ! UserDeleted
          // }
        } ~
        get { requestContext =>
          // println("get user " + id)
          // var resultUser: Option[UserNode] = RestApi.userList.find(_.id == id)
          // val responder = createResponder(requestContext)
          // resultUser.map(responder ! _.toMap())
          //   .getOrElse(responder ! UserNotFound)
        }
      }
    }

  private def createResponder(requestContext:RequestContext) = {
    context.actorOf(Props(new Responder(requestContext)))
  }
}

class Responder(requestContext:RequestContext) extends Actor with ActorLogging {
  import ufl.FacebookAPI._
  
  def receive = {

    case NodeCreated(id: String) =>
      requestContext.complete(StatusCodes.Created, "Node created with id:" + id)
      killYourself

    case PageDeleted | PostDeleted | UserDeleted =>
      requestContext.complete(StatusCodes.OK)
      killYourself

    case PageAlreadyExists =>
      requestContext.complete(StatusCodes.Conflict, "The page already exists")
      killYourself

    case NodeNotFound(nodeType: String) =>
      requestContext.complete(StatusCodes.NotFound, nodeType + " not found")  
      killYourself    

    case response: PageResponse =>
      requestContext.complete(StatusCodes.OK, response)
      killYourself

    case response: PostResponse =>
      requestContext.complete(StatusCodes.OK, response)
      killYourself

    case response: UserResponse =>
      requestContext.complete(StatusCodes.OK, response)
      killYourself
   }

  private def killYourself = self ! PoisonPill
}
