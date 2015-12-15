package ufl

import Array._
import ufl.FacebookAPI._
import scala.collection.immutable.Map

class PageNode(pageId:String, pageName:String, pageAbout:String)  {
    val id = pageId
    var name = pageName
    var about = pageAbout

    def pageResponse(): PageResponse = {
        val pageResponse = new PageResponse(id, name, about)
        pageResponse
    }
}

class PostNode(postId:String, postUserId:String, postContent:String, keys: Map[String, String])  {
    val id = postId
    var creatorId = postUserId
    var content = postContent
    var keyMap = keys

    def postResponse(requesterId:String): Either[PostResponse, String] = {
        val encryptedKey = keyMap.get(requesterId)
        println("[SERVER]: within postResponse " + encryptedKey)
        if(encryptedKey == None) {
            //Requester is not authorized to view this post.
            Right("fuck off")
        } else {
            val postResponse = new PostResponse(id, creatorId, content, encryptedKey.get)    
            Left(postResponse)
        }
    }
}

class UserNode(userId:String, userEmail:String, firstName:String,
    lastName:String, userGender:String, public_Key:String) {
    val id = userId
    val publicKey = public_Key
    var email = userEmail
    var first_name = firstName
    var last_name = lastName
    var gender = userGender
    var postList = Vector[String]()
    var albumList = Vector[String]()
    var friendsList = Vector[String]()

    def userResponse(): UserResponse = {
        val userResponse = new UserResponse(id, publicKey, email, first_name, last_name
            , gender, postList, albumList, friendsList)
        userResponse
    }
}

class PhotoNode(photoId:String, photoCaption:String, photoAlbum:String, creator:String, photoArray:Array[Byte]){
    val id = photoId
    var caption = photoCaption
    var album = photoAlbum
    var from = creator
    var photo = photoArray

    def photoResponse(): PhotoResponse = {
        val photoResponse = new PhotoResponse(id, caption, album , from, photo)
        photoResponse
    }
}

class AlbumNode(albumId: String, albumName: String, albumCaption: String, albumCreatorId: String, albumCreated_time:String) {
    val id = albumId
    var name = albumName
    var caption = albumCaption
    var creatorId = albumCreatorId
    var created_time = albumCreated_time
    var photos = Vector[String]()

    def albumResponse(): AlbumResponse = {
        val albumResponse = new AlbumResponse(id, photos.length, name, caption, 
            creatorId, created_time, photos)
        albumResponse
    }
}