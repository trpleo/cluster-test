package org.trp.cluster

import akka.actor.{ ActorRef, ActorSystem }
import akka.event.Logging
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.{ get, post }
import akka.http.scaladsl.server.directives.PathDirectives.path
import akka.http.scaladsl.server.directives.RouteDirectives.complete

import scala.concurrent.duration._
import akka.pattern.ask
import akka.util.Timeout
import org.trp.cluster.v100._

import scala.util.Random

trait ReadSideEndpointsRoutes extends ProtobufSupport {

  implicit def system: ActorSystem

  lazy val log = Logging(system, classOf[ReadSideEndpointsRoutes])

  //  def stoManager: ActorRef

  implicit lazy val timeout = Timeout(5.seconds) // usually we'd obtain the timeout from the system's configuration

  lazy val userRoutes: Route =
    get {
      complete("OK")
    }
  //    (get & path("sto" / "all")) {
  //      val stos = (stoManager ? GetSTOs(txId)).mapTo[StandingOrders]
  //      complete(stos)
  //    } ~ (get & path("sto" / Segment)) { id =>
  //      val maybeSTO = (stoManager ? GetSTO(id)).mapTo[Option[StandingOrder]]
  //      rejectEmptyResponse {
  //        complete(maybeSTO)
  //      }
  //    } ~ (post & path("sto")) {
  //      entity(as[StandingOrder]) { sto =>
  //        val fsto = (stoManager ? UpsertSTO(Some(sto))).mapTo[StandingOrder]
  //        onSuccess(fsto) {
  //          case so if (sto.id.isEmpty) => complete((StatusCodes.Created, so))
  //          case so => complete((StatusCodes.OK, so))
  //        }
  //      }
  //    }
}
