package org.trp.cluster

import akka.actor.{ Actor, ActorLogging, Props }
import org.trp.cluster.v100._

class STOManager extends Actor with ActorLogging with Config {

  def stoId(sto: StandingOrder) = sto.id

  override def receive: Receive = {
    case GetSTOs(tx) =>
      // note: it is expected, that there should not be any collision with names.
      val coordActor = context.system.actorOf(Props[GetAllSTOCoordinator], s"getSTOsCord-$tx")
      val children = context.children
      coordActor.tell(WaitForSTOs(children.size, tx), sender())
      children.map(ch => ch.tell(GetSTO(ch.path.name), coordActor))

    case roCmd @ GetSTO(id) =>
      context.children.find(_.path.name == id) match {
        case Some(sto) => sto forward roCmd
        case None => sender() ! None
      }

    case cmd @ UpsertSTO(Some(sto)) =>
      val stoActor = context.children.find(_.path.name == stoId(sto)).getOrElse(context.actorOf(Props[STO], stoId(sto)))
      stoActor ! cmd

    case UpsertSTO(None) =>
      log.error(s"None is unaccepted value for upsert command.")
  }
}
