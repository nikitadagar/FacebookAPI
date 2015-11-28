package ufl

import scala.collection.immutable.Map
import Array._

class PostNode(postId:String, postUser:UserNode, postContent:String)  {
    val id = postId
    val user = postUser
    val content = postContent

    //Put all post data in a map, used for conversion to JSON.
    def toMap(): Map[String, String] = {
        var result:Map[String, String] = Map[String, String]()
        result += ("id" -> id)
        result += ("userId" -> user.id)
        result += ("userFirstName" -> user.first_name)
        result += ("userLastName" -> user.last_name)
        result += ("content" -> content)
        result
    }
}

class PageNode(pageId:String, pageName:String, pageAbout:String)  {
    val id = pageId
    val name = pageName
    val about = pageAbout

    def toMap(): Map[String, String] = {
        var result:Map[String, String] = Map[String, String]()
        result += ("id" -> id)
        result += ("name" -> name)
        result += ("about" -> about)
        result
    }
}

class UserNode(userId:String, firstName:String, lastName:String, userGender:String) {
    val id = userId
    val first_name = firstName
    val last_name = lastName
    val gender = userGender
    var postList = Vector[PostNode]()

    def postsToListString(): String = {
        var result: Array[String] = Array[String]()
        postList.foreach {
            result:+ _.id
        }
        val resultString = "[" + result.mkString(", ") + "]"
        resultString
    }

    def toMap(): Map[String, String] = {
        var result:Map[String, String] = Map[String, String]()
        result += ("id" -> id)
        result += ("first_name" -> first_name)
        result += ("last_name" -> last_name)
        result += ("gender" -> gender)
        result += ("posts" -> postsToListString())
        result
    }
}