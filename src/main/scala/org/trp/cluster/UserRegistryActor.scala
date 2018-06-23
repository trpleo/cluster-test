package org.trp.cluster

import akka.actor.{ Actor, ActorLogging, Props }
import org.trp.cluster.v100._

object UserRegistryActor {
  def props: Props = Props[UserRegistryActor]
}

class UserRegistryActor extends Actor with ActorLogging {
  var users = Set.empty[User]

  def receive: Receive = {
    case GetUsers =>
      sender() ! Users(users.toSeq)
    case CreateUser(Some(user)) =>
      users += user
      sender() ! ActionPerformed(s"User ${user.name} created.")
    case CreateUser(None) =>
      log.error(s"Not allowed CreateUser command.")
    case GetUser(name) =>
      sender() ! users.find(_.name == name)
    case DeleteUser(name) =>
      users.find(_.name == name) foreach { user => users -= user }
      sender() ! ActionPerformed(s"User ${name} deleted.")
  }
}