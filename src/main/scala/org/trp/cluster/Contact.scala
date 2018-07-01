package org.trp.cluster

import akka.actor.{ Actor, ActorLogging }
import org.trp.cluster.v100.RabbitMQEnvelope

class Contact extends Actor with ActorLogging {
  override def receive: Receive = {
    case msg =>
      log.info(s"Message [$msg] arrived at [${self.path}]")
  }
}

object Contact {
  val contactNamePrefix = "contact-"
  def contactName2HandleMessage(cmd: RabbitMQEnvelope) = s"$contactNamePrefix${cmd.payload.get.contact.toString}"
}
