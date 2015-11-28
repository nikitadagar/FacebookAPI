package ufl

class PostNode(postId:String, postUser:UserNode, postContent:String)  {
    val id = postId
    val user = postUser
    val content = postContent
}

class PageNode(pageId:String, pageName:String, pageAbout:String)  {
    val id = pageId
    val name = pageName
    val about = pageAbout
}

class UserNode(userId:String, firstName:String, lastName:String, userGender:String) {
    val id = userId
    val first_name = firstName
    val last_name = lastName
    val gender = userGender
    var postList = Vector[PostNode]
}