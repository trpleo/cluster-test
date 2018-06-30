package org.trp.cluster

import akka.actor.{ Actor, ActorLogging }
import org.trp.cluster.v100.{ GetSTOs, StandingOrder, UpsertSTO }

class STO extends Actor with ActorLogging {

  override def preStart(): Unit = context.become(processing(Option.empty[StandingOrder]))

  override def receive: Receive = { case msg => unhandled(msg) }

  def processing(s0: Option[StandingOrder]): Receive = {
    case UpsertSTO(Some(sto)) =>
      ???

    case GetSTOs(tx) =>
      ???
  }
}
