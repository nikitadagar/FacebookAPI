package ufl

import akka.actor._
import akka.util.Timeout
import spray.http.StatusCodes
import spray.httpx.SprayJsonSupport._
import spray.routing._

import scala.concurrent.duration._
import scala.language.postfixOps

class RestInterface extends HttpServiceActor
  with RestApi {
  def receive = runRoute(routes)
}

object RestApi {
  var id = 0;
  private def getId = {
    id = id + 1
    id
  }
}


trait RestApi extends HttpService with ActorLogging { actor: Actor =>
  import ufl.FacebookAPI._

  implicit val timeout = Timeout(10 seconds)

  var page = Vector[Page]()

  def routes: Route =
    
    pathPrefix("page") {
      pathEnd {
        post { requestContext =>
          val responder = createResponder(requestContext)
          val page : PageNode = new PageNode(RestApi.getId)
          // println("id: " + RestInterface.getId)
          responder ! PageCreated
      } ~
      path(Segment) { id =>
        delete { requestContext =>
          println("delete page " + id)
          val responder = createResponder(requestContext)
          responder ! PageDeleted
        }

        get { requestContext =>
          println("get page " + id)
          println("id: " + RestApi.getId)
          val responder = createResponder(requestContext)
          responder ! PageCreated
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

    case PageCreated =>
      requestContext.complete(StatusCodes.Created)
      killYourself

    case PageDeleted =>
      requestContext.complete(StatusCodes.OK)
      killYourself

    case PageAlreadyExists =>
      requestContext.complete(StatusCodes.Conflict)
      killYourself
  }

  private def killYourself = self ! PoisonPill
}
