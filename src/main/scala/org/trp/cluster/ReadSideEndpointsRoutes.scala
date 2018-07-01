package org.trp.cluster

import akka.actor.{ ActorRef, ActorSystem }
import akka.cluster.sharding.ClusterSharding
import akka.event.Logging
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.get
import akka.http.scaladsl.server.directives.PathDirectives.path
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.pattern.ask
import akka.util.Timeout
import org.trp.cluster.v100._

// todo: proper dispatcher should be created for future responses
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

trait ReadSideEndpointsRoutes extends ProtobufSupport {

  implicit def system: ActorSystem

  lazy val log = Logging(system, classOf[ReadSideEndpointsRoutes])

  def campaignRegion: ActorRef

  def contactRegion: ActorRef

  implicit lazy val timeout = Timeout(5.seconds) // usually we'd obtain the timeout from the system's configuration

  lazy val userRoutes: Route =
    (get & path("campaign" / "opens")) {
      parameters('site.as[Int], 'campaign.as[Int]) { (site, campaign) =>
        val f = (campaignRegion ? GetOpenCnt(site, campaign)).mapTo[Int].map(_.toString)
        complete(f)
      }
    } ~ (get & path("campaign" / "uniqueopens")) {
      parameters('site.as[Int], 'campaign.as[Int]) { (site, campaign) =>
        val f = (campaignRegion ? GetUniqueOpenCnt(site, campaign)).mapTo[Int].map(_.toString)
        complete(f)
      }
    } ~ (get & path("campaign" / "campaigncontacts")) {
      parameters('site.as[Int], 'campaign.as[Int]) { (site, campaign) =>
        val f = (campaignRegion ? GetCampaignContacts(site, campaign)).mapTo[CampaignsContacts]
        complete(f)
      }
    } ~ get {
      complete("OK")
    }
}
