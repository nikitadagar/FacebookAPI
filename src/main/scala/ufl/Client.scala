package ufl

import akka.actor._

case object postPost
case object getPost

class Client extends Actor {
  
  def receive = {

  }
}

class UserActor extends Actor {
  var id:String = _

  def receive = {
    case postPost => {

    }

    case getPost => {

    }
  }
}
