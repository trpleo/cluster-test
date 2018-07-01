package org.trp.cluster

import akka.actor.{ Actor, ActorLogging }
import org.trp.cluster.v100._

class Campaign extends Actor with ActorLogging {

  import Campaign._

  log.debug("Campaign [{}] is initialized.", self.path)

  context.become(processing(State(0, 0, List.empty[OpenEvent])))

  override def receive: Receive = { case msg => unhandled(msg) }

  def processing(s0: State): Receive = {
    case msg: RabbitMQEnvelope =>
      log.debug("RabbitMQEnvelope message arrived [{}] from [{}]", msg, sender.path)
      sender() ! ProcessDone
      val s1 = State(s0.openCnt + 1, s0.uniqueOpenCnt, s0.contacts :+ (msg.payload.get))
      val s2 = s1.contacts.filter(_.contact == msg.payload.get.contact).isEmpty match {
        case true => s1.copy(uniqueOpenCnt = s1.uniqueOpenCnt + 1)
        case false => s1
      }
      context.become(processing(s2))

    case msg: GetOpenCnt =>
      log.debug("GetOpenCnt command arrived. [{}]", msg)
      sender ! s0.openCnt

    case msg: GetUniqueOpenCnt =>
      log.debug("GetOpenCnt command arrived. [{}]", msg)
      sender ! s0.uniqueOpenCnt

    case msg: GetCampaignContacts =>
      log.debug("GetOpenCnt command arrived. [{}]", msg)
      sender ! CampaignsContacts(s0.contacts)

    case msg: NACK =>
      log.warning(s"NACK message [{}] was received.", msg)

    case msg =>
      log.info(s"Message [$msg] arrived at [${self.path}], which is unhandled.")
      unhandled(msg)
  }
}

object Campaign {
  final case class State(openCnt: Int, uniqueOpenCnt: Int, contacts: List[OpenEvent])
  val campaignNamePrefix = "campaign-"
  def campaignName2HandleMessage(cmd: RabbitMQEnvelope) = s"$campaignNamePrefix${cmd.payload.get.campaign.toString}"
}