package org.trp.cluster

import akka.actor.SupervisorStrategy.Resume
import akka.actor.{ Actor, ActorLogging, OneForOneStrategy, Props }
import org.trp.cluster.v100.RabbitMQEnvelope

import scala.concurrent.duration._
import scala.language.postfixOps

class Site extends Actor with ActorLogging {

  // Note: intentionally var, since CM's parent is this actor, therefore
  //        with a 'var' the unnecessary state complexity can be reduced.
  var contactman = context.actorOf(Props[ContactManager], ContactManager.defineContactManagerName)
  var campaignman = context.actorOf(Props[CampaignManager], CampaignManager.campaignManagerName)

  override def receive: Receive = {
    case cmd: RabbitMQEnvelope =>
      contactman forward cmd
      campaignman forward cmd

    case msg =>
      log.warning(s"Unhandled message was received: [{}]", msg)
      unhandled(msg)
  }

  override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 seconds) {
    case _: Exception => Resume
  }
}

object Site {
  val siteNamePrefix = "site-"
  def siteName2HandleMessage(cmd: RabbitMQEnvelope) = s"$siteNamePrefix${cmd.payload.get.site.toString}"
}