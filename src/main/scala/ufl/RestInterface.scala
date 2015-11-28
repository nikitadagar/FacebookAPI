package ufl

import akka.actor._
import akka.util.Timeout
import spray.http.StatusCodes
import spray.httpx.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._
import spray.routing._

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
            responder ! PageCreated
          }
        }
      } ~
      path(Segment) { id =>
        delete { requestContext =>
          println("delete page " + id)
          RestApi.pageList = RestApi.pageList.filterNot(_.id == id)
          val responder = createResponder(requestContext)
          responder ! PageDeleted
        } ~
        get { requestContext =>
          println("get page " + id)
          var resultPage: Option[PageNode] = RestApi.pageList.find(_.id == id)
          val responder = createResponder(requestContext)
          resultPage.map(responder ! _.toMap())
            .getOrElse(responder ! PageNotFound)
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
              responder ! UserNotFound
            } else {
              //Valid user, create a new post.
              val postNode: PostNode = new PostNode(newPostId, resultUser.get, post.content)
              RestApi.postList = RestApi.postList :+ postNode

              println("Created new post by: " + postNode.id + ", id: " + postNode.id)
              responder ! PostCreated
            }
          }
        }
      } ~
      path(Segment) { id =>
        delete { requestContext =>
          println("delete user " + id)
          RestApi.userList = RestApi.userList.filterNot(_.id == id)
          val responder = createResponder(requestContext)
          responder ! PageDeleted
        } ~
        get { requestContext =>

        }
      }
    } ~
    pathPrefix("user") {
      pathEnd {
        post {
          entity(as[User]) { user => requestContext =>
            var newUserId = RestApi.getId
            //TODO: get user object from post.userId
            val userNode: UserNode = new UserNode(newUserId, user.firstname, user.lastname, user.gender)
            RestApi.userList = RestApi.userList :+ userNode

            println("Created new user with id: " + userNode.id)
            val responder = createResponder(requestContext)
            responder ! UserCreated
          }
        }
      } ~
      path(Segment) { id =>
        delete { requestContext =>
          println("delete user " + id)
          RestApi.userList = RestApi.userList.filterNot(_.id == id)
          val responder = createResponder(requestContext)
          responder ! PageDeleted
        } ~
        get { requestContext =>

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

    case PageCreated | PostCreated =>
      requestContext.complete(StatusCodes.Created)
      killYourself

    case PageDeleted =>
      requestContext.complete(StatusCodes.OK)
      killYourself

    case PageAlreadyExists =>
      requestContext.complete(StatusCodes.Conflict)
      killYourself

    case PageNotFound | PostNotFound | UserNotFound=>
      requestContext.complete(StatusCodes.NotFound)

    case jsonMap: Map[String, String] =>
      requestContext.complete(StatusCodes.OK, jsonMap)
      killYourself
   }

  private def killYourself = self ! PoisonPill
}
