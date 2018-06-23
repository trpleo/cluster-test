package org.trp.cluster

import akka.actor.{ Actor, ActorLogging, Props }
import org.trp.cluster.v100._

class STOManager extends Actor with ActorLogging with Config {

  def stoId(sto: StandingOrder) = sto.id

  override def receive: Receive = {
    case GetSTOs(tx) =>
      // note: it is expected, that there should not be any collision with names.
      val getAllSTOCoordinator = context.system.actorOf(Props[GetAllSTOCoordinator], s"getSTOsCord-$tx")
      val children = context.children
      getAllSTOCoordinator.tell(WaitForSTOs(children.size), sender())
      children.map(ch => ch.tell(GetSTO(ch.path.name), getAllSTOCoordinator))

    case msg @ GetSTO(id) =>
      context.children.find(_.path.name == id) match {
        case Some(sto) => sto forward msg
        case None => sender() ! None
      }

    case UpsertSTO(Some(sto)) =>
      context.children.find(_.path.name == stoId(sto)).getOrElse(context.actorOf(Props[STO], stoId(sto)))

    case UpsertSTO(None) =>
      log.error(s"None is unaccepted value for upsert command.")
  }
}
