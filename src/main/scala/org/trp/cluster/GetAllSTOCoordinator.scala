package org.trp.cluster

import akka.actor.{ Actor, ActorLogging, ActorRef, Stash }
import org.trp.cluster.v100.{ StandingOrder, StandingOrders, WaitForSTOs }

import scala.concurrent.ExecutionContext.Implicits.global

class GetAllSTOCoordinator extends Actor with ActorLogging with Stash with Config {

  type Cardinality = Int

  object Timeout

  def updateWithStandingOrder(so: StandingOrder)(implicit sos: StandingOrders) = StandingOrders(sos.stos :+ so)

  override def receive: Receive = {

    case WaitForSTOs(cardinality) =>
      context.become(processing(sender(), cardinality)(StandingOrders()))
      context.system.scheduler.scheduleOnce(getStosTimeout, self, Timeout)
      unstashAll()

    case _ =>
      stash()
  }

  def processing(responseTo: ActorRef, cardinality: Cardinality)(implicit response: StandingOrders): Receive = {

    case Some(so: StandingOrder) if (cardinality == 1) =>
      responseTo ! updateWithStandingOrder(so)
      context.stop(self)

    case Some(so: StandingOrder) =>
      context.become(processing(responseTo, cardinality)(updateWithStandingOrder(so)))

    case Timeout =>
      log.warning(s"Timeout happened. Still [$cardinality] message is missing.")
      responseTo ! response
      context.stop(self)

    case msg =>
      log.warning(s"Unexpected message [$msg]")
      unhandled(msg)
  }
}
