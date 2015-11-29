package ufl

import Array._
import ufl.FacebookAPI._

class PageNode(pageId:String, pageName:String, pageAbout:String)  {
    var id = pageId
    var name = pageName
    var about = pageAbout

    def pageResponse(): PageResponse = {
        val pageResponse = new PageResponse(id, name, about)
        pageResponse
    }
}

class PostNode(postId:String, postUser:UserNode, postContent:String)  {
    val id = postId
    val user = postUser
    val content = postContent

    def postResponse(): PostResponse = {
        val username = user.first_name + " " + user.last_name
        val postResponse = new PostResponse(id, user.id, username, content)
        postResponse
    }
}

class UserNode(userId:String, firstName:String, lastName:String, userGender:String) {
    val id = userId
    val first_name = firstName
    val last_name = lastName
    val gender = userGender
    var postList = Vector[String]()

    def userResponse(): UserResponse = {
        val userResponse = new UserResponse(id, first_name, last_name
            , gender, postList)
        userResponse
    }
}

class PhotoNode(photoId:String, photoCaption:String, photoAlbum:String, creator:String){
    var id = photoId
    var caption = photoCaption
    var album = photoAlbum
    var from = creator 
}


