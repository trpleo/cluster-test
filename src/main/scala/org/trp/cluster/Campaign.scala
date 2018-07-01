package org.trp.cluster

import akka.actor.{ Actor, ActorLogging }
import org.trp.cluster.ProcessManager.{ NACK, ProcessDone }
import org.trp.cluster.v100.RabbitMQEnvelope

class Campaign extends Actor with ActorLogging {

  log.debug("[{}] initialized.", self.path)

  override def receive: Receive = {
    case msg: RabbitMQEnvelope =>
      log.debug("RabbitMQEnvelope message arrived [{}] from [{}]", msg, sender.path)
      sender() ! ProcessDone

    case msg: NACK =>
      log.warning(s"NACK message [{}] was received.", msg)

    case msg =>
      log.info(s"Message [$msg] arrived at [${self.path}], which is unhandled.")
      unhandled(msg)
  }
}

object Campaign {
  val campaignNamePrefix = "campaign-"
  def campaignName2HandleMessage(cmd: RabbitMQEnvelope) = s"$campaignNamePrefix${cmd.payload.get.campaign.toString}"
}