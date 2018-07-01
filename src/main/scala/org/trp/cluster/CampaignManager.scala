package org.trp.cluster

import akka.actor.{ Actor, ActorLogging }
import org.trp.cluster.ProcessManager.ProcessDone
import org.trp.cluster.v100.RabbitMQEnvelope

// the campaign is the email, which is sent out to specified Contacts of the Site
class CampaignManager extends Actor with ActorLogging {
  override def receive: Receive = {
    case msg =>
      log.info(s"Message [$msg] arrived at [${self.path}]")
      sender() ! ProcessDone
  }
}

object CampaignManager {
  val campaignManagerName = "campman"
  val campaignNamePrefix = "site-"
  def campaignName2HandleMessage(cmd: RabbitMQEnvelope) = s"$campaignNamePrefix${cmd.payload.get.site.toString}"
}
