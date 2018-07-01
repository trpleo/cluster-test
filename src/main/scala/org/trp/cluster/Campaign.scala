package org.trp.cluster

import akka.actor.{Actor, ActorLogging}

class Campaign extends Actor with ActorLogging {
  override def receive: Receive = {
    case msg =>
      log.info(s"Message [$msg] arrived at [${self.path}]")
  }
}
