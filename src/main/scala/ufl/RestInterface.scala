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
import java.util.Calendar

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
  var albumList = Vector[AlbumNode]()
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
            val responder = createResponder(requestContext)
            val resultUser: Option[UserNode] = RestApi.userList.find(_.id == post.userId)

            if(resultUser.isEmpty) {
              //invalid user id
              responder ! NodeNotFound("User")
            } else {
              //Valid user, create a new post.
              var newPostId = RestApi.getId
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
            val responder = createResponder(requestContext)
            val resultUser: Option[UserNode] = RestApi.userList.find(_.email == user.email)
            if(resultUser.isEmpty) {
              responder ! UserAlreadyExists
            } else {
              var newUserId = RestApi.getId
              var newDefaultAlbumId = RestApi.getId

              val albumNode: AlbumNode = new AlbumNode(newDefaultAlbumId, "Timeline Photos", 
                "", newUserId, Calendar.getInstance().getTime().toString) 
              val userNode: UserNode = new UserNode(newUserId, user.email, user.firstname, 
                user.lastname, user.gender)
              
              userNode.albumList = userNode.albumList :+ newDefaultAlbumId

              RestApi.userList = RestApi.userList :+ userNode
              RestApi.albumList = RestApi.albumList :+ albumNode

              println("Created new user with id: " + userNode.id)
              responder ! NodeCreated(newUserId)
            }
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
    } ~
    pathPrefix("photo") {
      pathEnd {
        post {
          entity(as[Photo]) { photo => requestContext =>
            val responder = createResponder(requestContext)

            //find if user exists
            var user: Option[UserNode] = RestApi.userList.find(_.id == photo.creatorId)
            if (user.isEmpty) {
              responder ! NodeNotFound("User")
            } else {
              //find if that user has the album id
              var photoAlbumId: Option[String] = user.get.albumList.find(_ == photo.albumId)
              if (photoAlbumId.isEmpty) {
                responder ! NodeNotFound("Album")
              }
              else {
                var newPhotoId = RestApi.getId
                var resultAlbum: Option[AlbumNode] = RestApi.albumList.find(_.id == photoAlbumId.get)
                val photoNode: PhotoNode = new PhotoNode(newPhotoId, photo.caption, photo.albumId, photo.creatorId, photo.photo)
                RestApi.photoList = RestApi.photoList :+ photoNode
                resultAlbum.get.photos = resultAlbum.get.photos :+ photoNode.id
                println("Created new photo with id: " + photoNode.id + " for Album id " + resultAlbum.get.id)
                responder ! NodeCreated(newPhotoId)
              }
            }
          }
        }
      } ~
      path(Segment) { id =>
        delete { requestContext =>
          println("delete photo " + id)
          val responder = createResponder(requestContext)
          var resultPhoto: Option[PhotoNode] = RestApi.photoList.find(_.id == id)
          if(resultPhoto.isEmpty) {
            responder ! NodeNotFound("Photo")
          } else {
            RestApi.photoList = RestApi.photoList.filterNot(_.id == id)
            responder ! PhotoDeleted
          }
        } ~
        get { requestContext =>
          println("get photo " + id)
          var resultPhoto: Option[PhotoNode] = RestApi.photoList.find(_.id == id)
          val responder = createResponder(requestContext)
          resultPhoto.map(responder ! _.photoResponse())
            .getOrElse(responder ! NodeNotFound("Photo"))
        }
      }
    } ~
    pathPrefix("album") {
      pathEnd {
        post {
          entity(as[Album]) { album => requestContext =>
            val responder = createResponder(requestContext)
            var newAlbumId = RestApi.getId
            val resultUser: Option[UserNode] = RestApi.userList.find(_.id == album.creatorId)
            if(resultUser.isEmpty) {
              responder ! NodeNotFound("User")
            } else {
              val albumNode: AlbumNode = new AlbumNode(newAlbumId, album.name, album.caption, 
              album.creatorId, Calendar.getInstance().getTime().toString)
              RestApi.albumList = RestApi.albumList :+ albumNode
              println("Created new album by: " + album.creatorId + ", id:" + newAlbumId)
              responder ! NodeCreated(newAlbumId)
            }
          }
        }
      } ~
      path(Segment) {  id =>
        delete { requestContext =>
          println("delete album " + id)
          val responder = createResponder(requestContext)
          var resultAlbum: Option[AlbumNode] = RestApi.albumList.find(_.id == id)
          if(resultAlbum.isEmpty) {
            responder ! NodeNotFound("Album")
          } else {
            RestApi.albumList = RestApi.albumList.filterNot(_.id == id)
            responder ! AlbumDeleted
          }
        } ~
        get { requestContext =>
          println("get album " + id)
          var resultAlbum: Option[AlbumNode] = RestApi.albumList.find(_.id == id)
          val responder = createResponder(requestContext)
          resultAlbum.map(responder ! _.albumResponse())
            .getOrElse(responder ! NodeNotFound("Album"))
        }
      }
    } ~
    pathPrefix("friendsList") {
      pathEnd {
        post {
          entity(as[FriendsList]) { friendsList => requestContext =>
            val responder = createResponder(requestContext)
            val resultOwner: Option[UserNode] = RestApi.userList.find(_.id == friendsList.owner)
            val resultFriend: Option[UserNode] = RestApi.userList.find(_.id == friendsList.friend)
            if(resultOwner.isEmpty) {
              responder ! NodeNotFound("User")
            } else if(resultFriend.isEmpty) {
              responder ! NodeNotFound("Friend")
            } else {
              //both owner and friend exist, add in each others friends list.
              resultOwner.get.friendsList = resultOwner.get.friendsList :+ friendsList.friend
              resultFriend.get.friendsList = resultFriend.get.friendsList :+ friendsList.owner
              println("Updated friends list for " + friendsList.owner + " and " + friendsList.friend)
              responder ! FriendsListUpdated
            }
          }
        }
      } ~
      path(Segment) { id =>
        delete { requestContext =>
          //not yet implemented.
        } ~
        get { requestContext =>
          val responder = createResponder(requestContext)
          val resultUser: Option[UserNode] = RestApi.userList.find(_.id == id)
          if(resultUser.isEmpty) {
            responder ! NodeNotFound("User")
          } else {
            responder ! resultUser.get.friendsList
          }
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

    case PageDeleted | PostDeleted | UserDeleted | PhotoDeleted | AlbumDeleted | FriendsListUpdated =>
      requestContext.complete(StatusCodes.OK)
      killYourself

    case UserAlreadyExists =>
      requestContext.complete(StatusCodes.Conflict, "A user with that email is already registered")
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

    case response: Vector[String] =>
      requestContext.complete(StatusCodes.OK, response)
      killYourself
   }

  private def killYourself = self ! PoisonPill
}
