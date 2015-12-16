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
          val responder = createResponder(requestContext)
          var pageDeleted = deletePage(id)
          if(pageDeleted)
            responder ! PageDeleted
          else
            responder ! NodeNotFound("Page")
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
          entity(as[FBPost]) { post => requestContext =>
            val responder = createResponder(requestContext)
            val resultUser: Option[UserNode] = RestApi.userList.find(_.id == post.userId)

            if(resultUser.isEmpty) {
              //invalid user id
              responder ! NodeNotFound("User")
            } else {
              //Valid user, create a new post.
              var newPostId = RestApi.getId
              val postNode: PostNode = new PostNode(newPostId, resultUser.get.id, post.content, post.authUsers)
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
          val responder = createResponder(requestContext)
          var postDeleted = deletePost(id)
          if(postDeleted)
            responder ! PostDeleted
          else
            responder ! NodeNotFound("Post")
        } ~
        get{
          parameter('requesterId) { requesterId =>
          requestContext =>
          var resultPost: Option[PostNode] = RestApi.postList.find(_.id == id)
          val responder = createResponder(requestContext)
          resultPost.map(responder ! _.postResponse(requesterId))
            .getOrElse(responder ! NodeNotFound("Post"))
          }
        }
      }
    } ~
    pathPrefix("user") {
      pathEnd {
        post {
          entity(as[User]) { user => requestContext =>
            val responder = createResponder(requestContext)
            val resultUser: Option[UserNode] = RestApi.userList.find(_.email == user.email)
            if(!resultUser.isEmpty) {
              responder ! UserAlreadyExists
            } else {
              var newUserId = RestApi.getId
              val userNode: UserNode = new UserNode(newUserId, user.email, user.firstname, 
                user.lastname, user.gender, user.publicKey)
              RestApi.userList = RestApi.userList :+ userNode

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
          var deletedUser = deleteUser(id)
          if(deletedUser)
            responder ! UserDeleted
          else
            responder ! NodeNotFound("User")
        } ~
        get { requestContext =>
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
                val photoNode: PhotoNode = new PhotoNode(newPhotoId, photo.caption, photo.albumId,
                 photo.creatorId, photo.photo, photo.authUsers)
                RestApi.photoList = RestApi.photoList :+ photoNode
                resultAlbum.get.photos = resultAlbum.get.photos :+ photoNode.id
                responder ! NodeCreated(newPhotoId)
              }
            }
          }
        }
      } ~
      path(Segment) { id =>
        delete { requestContext =>
          val responder = createResponder(requestContext)
          var resultPhoto: Option[PhotoNode] = RestApi.photoList.find(_.id == id)
          if(resultPhoto.isEmpty) {
            responder ! NodeNotFound("Photo")
          } else {
            RestApi.photoList = RestApi.photoList.filterNot(_.id == id)
            responder ! PhotoDeleted
          }
        } ~
        get{
          parameter('requesterId) { requesterId =>
          requestContext =>
          var resultPhoto: Option[PhotoNode] = RestApi.photoList.find(_.id == id)
          val responder = createResponder(requestContext)
          resultPhoto.map(responder ! _.photoResponse(requesterId))
            .getOrElse(responder ! NodeNotFound("Photo"))
          }
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
                album.creatorId, Calendar.getInstance().getTime().toString, album.authUsers)
              RestApi.albumList = RestApi.albumList :+ albumNode
              resultUser.get.albumList = resultUser.get.albumList :+ newAlbumId
              responder ! NodeCreated(newAlbumId)
            }
          }
        }
      } ~
      path(Segment) {  id =>
        delete { requestContext =>
          val responder = createResponder(requestContext)
          if(deleteAlbum(id))
            responder ! UserDeleted
          else
            responder ! NodeNotFound("User")
        } ~
        get{
          parameter('requesterId) { requesterId =>
          requestContext =>
          var resultAlbum: Option[AlbumNode] = RestApi.albumList.find(_.id == id)
          val responder = createResponder(requestContext)
          resultAlbum.map(responder ! _.albumResponse())
            .getOrElse(responder ! NodeNotFound("Album"))
          }
        }
      }
    } ~
    pathPrefix("friendsList") {
      pathEnd {
        post {
          entity(as[FriendsList]) { friendRequest => requestContext =>
            val responder = createResponder(requestContext)
            val resultOwner: Option[UserNode] = RestApi.userList.find(_.id == friendRequest.owner)
            val resultFriend: Option[UserNode] = RestApi.userList.find(_.id == friendRequest.friend)
            if(resultOwner.isEmpty) {
              responder ! NodeNotFound("User")
            } else if(resultFriend.isEmpty) {
              responder ! NodeNotFound("Friend")
            } else {
              //both owner and friend exist, check if they're not friends already
              val alreadyFriend: Option[String] = resultOwner.get.friendsList.find(_ == friendRequest.friend)
              if(alreadyFriend.isEmpty) {
                //not a friend yet
                //add in each others friends list.
                resultOwner.get.friendsList = resultOwner.get.friendsList :+ friendRequest.friend
                resultFriend.get.friendsList = resultFriend.get.friendsList :+ friendRequest.owner
                println("Updated friends list for " + friendRequest.owner + " and " + friendRequest.friend)
                responder ! FriendsListUpdated
              } else {
                //already a friend
                responder ! FriendExists
              }
              
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
    } ~
    pathPrefix("AllUsers") {
      pathEnd {
        get { requestContext =>
          val responder = createResponder(requestContext)
          var userIdList: Vector[String] = Vector[String]()
          for(userObject: UserNode <- RestApi.userList) {
            userIdList = userIdList :+ userObject.id
          }
          responder ! userIdList
        }
      }
    }

  private def createResponder(requestContext:RequestContext) = {
    context.actorOf(Props(new Responder(requestContext)))
  }

  private def deletePage(id:String): Boolean = {
    println("delete page " + id)
    var resultPage: Option[PageNode] = RestApi.pageList.find(_.id == id)
    if(resultPage.isEmpty) {
      return false
    } else {
      RestApi.pageList = RestApi.pageList.filterNot(_.id == id)
      return true
    }
  }

  private def deletePost(id:String): Boolean = {
    println("delete user " + id)
    var resultPost: Option[PostNode] = RestApi.postList.find(_.id == id)
    if(resultPost.isEmpty) {
      return false
    } else {
      var resultUser: Option[UserNode] = RestApi.userList.find(_.id == resultPost.get.creatorId) //get the creator user
      resultUser.get.postList = resultUser.get.postList.filterNot(_ == id) //delete post from the post list of the creator user
      RestApi.postList = RestApi.postList.filterNot(_.id == id) //delete post from the list of posts
      return true
    }
  }

  private def deleteUser(id:String): Boolean = {
    println("delete user " + id)
    var resultUser: Option[UserNode] = RestApi.userList.find(_.id == id)
    if(resultUser.isEmpty) {
      return false
    } else {
      resultUser.get.postList.foreach {deletePhoto(_)}
      resultUser.get.albumList.foreach {deleteAlbum(_)}
      resultUser.get.friendsList.foreach {deleteFromFriendList(_, id)} //delete myself from the friend list of my friends.
      RestApi.userList = RestApi.userList.filterNot(_.id == id)
      return true
    }
  }

  private def deletePhoto(id:String) : Boolean = {
    var photo: Option[PhotoNode] = RestApi.photoList.find(_.id == id)
    if(photo.isEmpty) {
      return false
      } else {
      val album = RestApi.albumList.find(_.id == photo.get.album) //get album of the photo
      album.get.photos = album.get.photos.filterNot(_ == id)  //delete photo form albums's photo list
      RestApi.photoList = RestApi.photoList.filterNot(_.id == id) //delete photo from photoList
      return true
    }
  }

  private def deleteAlbum(id:String) : Boolean = {
    var resultAlbum: Option[AlbumNode] = RestApi.albumList.find(_.id == id)
      if(resultAlbum.isEmpty) {
          return false
      } else {
        resultAlbum.get.photos.foreach { deletePhoto(_) } //delete all photos from album
        val user = RestApi.userList.find(_.id == resultAlbum.get.creatorId) //get creator of album
        user.get.albumList = user.get.albumList.filterNot(_ == id) //delete album from users album list
        RestApi.albumList = RestApi.albumList.filterNot(_.id == id) //delete album from albumList
        return true
      }
  }

  private def deleteFromFriendList(friendListOfId:String, deleteUserId:String) {
    //delete deleteUserId from the list of friendListOfId
    var resultUser: Option[UserNode] = RestApi.userList.find(_.id == friendListOfId)
    resultUser.get.friendsList = resultUser.get.friendsList.filterNot(_ == deleteUserId)
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

    case FriendExists =>
      requestContext.complete(StatusCodes.OK, "Requested user is already a friend.")
      killYourself

    case NodeNotFound(nodeType: String) =>
      requestContext.complete(StatusCodes.NotFound, nodeType + " not found")  
      killYourself    

    case response: PageResponse =>
      requestContext.complete(StatusCodes.OK, response)
      killYourself

    case Left(response: PostResponse) =>
      println("found your key")
      requestContext.complete(StatusCodes.OK, response)
      killYourself

    case response: UserResponse =>
      requestContext.complete(StatusCodes.OK, response)
      killYourself

    case response: PhotoResponse =>
      requestContext.complete(StatusCodes.OK, response)
      killYourself

    case response: AlbumResponse =>
      requestContext.complete(StatusCodes.OK, response)
      killYourself

    case response: Vector[String] =>
      requestContext.complete(StatusCodes.OK, response)
      killYourself

    case Right(notAuthorizedError:String) =>
      requestContext.complete(StatusCodes.Unauthorized, notAuthorizedError)
      killYourself
   }

  private def killYourself = self ! PoisonPill
}
