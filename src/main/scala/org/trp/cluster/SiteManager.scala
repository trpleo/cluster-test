package org.trp.cluster

import akka.actor.{ Actor, ActorLogging, Props }
import org.trp.cluster.v100.{ OpenEvent, RabbitMQEnvelope }

// the actual legal person, which has a contract.
class SiteManager extends Actor with ActorLogging {

  override def receive: Receive = {
    case cmd: RabbitMQEnvelope =>
      val siteName = Site.siteName2HandleMessage(cmd)
      val handlerActor = context.child(siteName).getOrElse(context.actorOf(Props[Site], siteName))

      handlerActor forward cmd

    case msg =>
      log.warning(s"Unhandled message was received: [{}]", msg)
      unhandled(msg)
  }
}

object SiteManager {
  val siteManagerName = "SiteManager"
}