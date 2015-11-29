package ufl

import Array._
import ufl.FacebookAPI._

class PageNode(pageId:String, pageName:String, pageAbout:String)  {
    val id = pageId
    var name = pageName
    var about = pageAbout

    def pageResponse(): PageResponse = {
        val pageResponse = new PageResponse(id, name, about)
        pageResponse
    }
}

class PostNode(postId:String, postUser:UserNode, postContent:String)  {
    val id = postId
    var user = postUser
    var content = postContent

    def postResponse(): PostResponse = {
        val username = user.first_name + " " + user.last_name
        val postResponse = new PostResponse(id, user.id, username, content)
        postResponse
    }
}

class UserNode(userId:String, userEmail:String, firstName:String, lastName:String, userGender:String) {
    val id = userId
    var email = userEmail
    var first_name = firstName
    var last_name = lastName
    var gender = userGender
    var postList = Vector[String]()
    var albumList = Vector[String]()

    def userResponse(): UserResponse = {
        val userResponse = new UserResponse(id, email, first_name, last_name
            , gender, postList, albumList)
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

class FriendsListNode(listId:String, ownerId:String, friend:String) {
    val id = listId
    val owner = ownerId
    friends = Array[String] //stores IDs of friends

    def friendsListResponse(): FriendsListResponse = {
        val friendslistResponse = new FriendsListResponse(id, owner, friends)
        friendslistResponse
    }    
}


