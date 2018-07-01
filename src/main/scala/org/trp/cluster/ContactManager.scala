package org.trp.cluster

import akka.actor.{ Actor, ActorLogging }
import org.trp.cluster.ProcessManager.ProcessDone

// the contact is the actual natural person, who will get the messages, and
// open the emails.
class ContactManager extends Actor with ActorLogging {
  override def receive: Receive = {
    case msg =>
      log.info(s"Message [$msg] arrived at [${self.path}]")
      sender() ! ProcessDone
  }
}

object ContactManager {
  val defineContactManagerName = "contactman"
}