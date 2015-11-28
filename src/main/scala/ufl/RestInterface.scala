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
  var pageId = 0;
  var postId = 0;
  private def getPageId = {
    pageId = pageId + 1
    pageId.toString
  }
  private def getPostId = {
    postId = postId + 1
    postId.toString
  }

  var pageList = Vector[PageNode]()
  var postList = Vector[PostNode]()
}


trait RestApi extends HttpService with ActorLogging { actor: Actor =>
  import ufl.FacebookAPI._

  implicit val timeout = Timeout(10 seconds)

  def routes: Route =
    
    pathPrefix("page") {
      pathEnd {
        post {
          entity(as[Page]) { page => requestContext =>
            var newPageId = RestApi.getPageId
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
          var resultPage : Option[PageNode] = RestApi.pageList.find(_.id == id)
          val responder = createResponder(requestContext)
          resultPage.map(responder ! pageNodeToJson(_))
            .getOrElse(responder ! PageNotFound)
        }
      }
    } ~
    pathPrefix("post") {
      pathEnd {
        post {
          entity(as[Post]) { post => requestContext =>
            var newPostId = RestApi.getPostId
            //TODO: get user object from post.userId
            val postNode: PostNode = new PostNode(newPostId, post.userId, post.content)
            RestApi.postList = RestApi.postList :+ postNode

            println("Created new post by: " + postNode.id + ", id: " + postNode.id)
            val responder = createResponder(requestContext)
            responder ! PostCreated
          }
        }
      } ~
      path(Segment) { id =>
        delete { requestContext =>

        } ~
        get { requestContext =>

        }
      }
    }

  private def createResponder(requestContext:RequestContext) = {
    context.actorOf(Props(new Responder(requestContext)))
  }

  private def pageNodeToJson(pageNode:PageNode): Map[String, String] = {
    var result:Map[String, String] = Map[String, String]()
    result += ("id" -> pageNode.id)
    result += ("name" -> pageNode.name)
    result += ("about" -> pageNode.about)
    result
  }
}

class Responder(requestContext:RequestContext) extends Actor with ActorLogging {
  import ufl.FacebookAPI._
  
  def receive = {

    case PageCreated, PostCreated =>
      requestContext.complete(StatusCodes.Created)
      killYourself

    case PageDeleted =>
      requestContext.complete(StatusCodes.OK)
      killYourself

    case PageAlreadyExists =>
      requestContext.complete(StatusCodes.Conflict)
      killYourself

    case PageNotFound =>
      requestContext.complete(StatusCodes.NotFound)

    case jsonMap: Map[String, String] =>
      requestContext.complete(StatusCodes.OK, jsonMap)
      killYourself
   }

  private def killYourself = self ! PoisonPill
}
