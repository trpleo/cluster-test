package org.trp.cluster

import akka.actor.{ Actor, ActorLogging }
import org.trp.cluster.v100._

class Contact extends Actor with ActorLogging {

  import Contact._

  log.debug("Contact [{}] is initialized.", self.path)

  context.become(processing(State(List.empty[OpenEvent])))

  override def receive: Receive = { case msg => unhandled(msg) }

  def processing(s0: State): Receive = {
    case msg: RabbitMQEnvelope =>
      log.debug("RabbitMQEnvelope message arrived [{}] from [{}]", msg, sender.path)
      context.become(processing(State(s0.openEvents :+ (msg.payload.get))))
      sender() ! ProcessDone

    case msg @ NACK(env) =>
      log.warning(s"NACK message [{}] was received.", msg)
      val s1 = State(s0.openEvents.filterNot(env.get.payload.get == _))
      context.become(processing(s1))

    case IfCampaignWasOpen(siteId, campaignId) =>
      val result = OpenedCampaignsByContact(s0.openEvents.filter(e => e.campaign == campaignId && e.site == siteId))
      sender ! result

    case msg =>
      log.info(s"Message [$msg] arrived at [${self.path}], which is unhandled.")
      unhandled(msg)
  }
}

object Contact {
  final case class State(openEvents: List[OpenEvent])
  val contactNamePrefix = "contact-"
  def contactName2HandleMessage(cmd: RabbitMQEnvelope) = s"$contactNamePrefix${cmd.payload.get.contact.toString}"
}
