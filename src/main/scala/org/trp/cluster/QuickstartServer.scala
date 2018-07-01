package org.trp.cluster

import akka.actor.{ ActorRef, ActorSystem, Props }
import akka.cluster.Cluster
import akka.cluster.sharding.{ ClusterSharding, ClusterShardingSettings, ShardRegion }
import akka.event.{ Logging, LoggingAdapter }
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.management.AkkaManagement
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import org.trp.cluster.ProcessManager.NACK
import org.trp.cluster.v100.RabbitMQEnvelope

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, ExecutionContext }
import scala.util.Try

object QuickstartServer extends App with ReadSideEndpointsRoutes with Config {

  val (hostname, port) = Try {
    val arr = args(0).split("@")(1).split(":")
    (arr(0), arr(1).toInt)
  } getOrElse {
    ("0.0.0.0", 6651)
  }

  val rawSeedNodes = s""""${System.getenv("SEEDNODES").split(",").toList.mkString("""", """")}""""

  val sysConfig =
    ConfigFactory.parseString(s"akka.remote.netty.tcp.port=${port}").
      withFallback(ConfigFactory.parseString(s"akka.remote.netty.tcp.hostname=${hostname}")).
      withFallback(ConfigFactory.parseString(s"akka.cluster.seed-nodes=[${rawSeedNodes}]")).
      withFallback(ConfigFactory.load("application"))

  implicit val system: ActorSystem = ActorSystem("TestSTOCluster", sysConfig)
  implicit val executor: ExecutionContext = system.dispatcher // todo: custom dispatcher to become default dispatcher
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit override lazy val log: LoggingAdapter = Logging(system, getClass)

  AkkaManagement(system).start()

  log.info(s"remote.netty.tcp.port=$port")
  log.info(s"remote.netty.tcp.hostname=$hostname")
  log.info(s"cluster.seed-nodes=[$rawSeedNodes]")

  implicit val cluster = Cluster(system)
  val selfAddress = cluster.selfAddress
  log.info(s"cluster: [$cluster]")
  log.info(s"state: [${cluster.state}]")
  log.info(s"selfAddress: [$selfAddress]")

  val numberOfShards = 100

  /**
   * Campaign for Cluster Sharding
   */
  def defineEntityIdForCampaign(cmd: RabbitMQEnvelope) = s"${Site.siteName2HandleMessage(cmd)}-${Campaign.campaignName2HandleMessage(cmd)}"

  val extractEntityIdForCampaign: ShardRegion.ExtractEntityId = {
    case cmd: RabbitMQEnvelope => (defineEntityIdForCampaign(cmd), cmd)
    case cmd @ NACK(env) => (defineEntityIdForContact(env), cmd)
  }

  val extractShardIdForCampaign: ShardRegion.ExtractShardId = {
    case cmd: RabbitMQEnvelope => (cmd.payload.get.campaign % numberOfShards).toString // defineEntityIdForCampaign (cmd)
    case NACK(env) => (env.payload.get.campaign % numberOfShards).toString // defineEntityIdForContact(env)
    case ShardRegion.StartEntity(id) => id.toString
  }

  val campaignRegion: ActorRef = ClusterSharding(system).start(
    typeName = "campaigns",
    entityProps = Props[Campaign],
    settings = ClusterShardingSettings(system),
    extractEntityId = extractEntityIdForCampaign,
    extractShardId = extractShardIdForCampaign)

  log.debug("campaignRegion: [{}]", campaignRegion.path)

  /**
   * Contact for Cluster Sharding
   */
  def defineEntityIdForContact(cmd: RabbitMQEnvelope) = {
    val name = s"${Site.siteName2HandleMessage(cmd)}-${Contact.contactName2HandleMessage(cmd)}"
    log.debug("EntityID is [{}]", name)
    name
  }

  val extractEntityIdForContact: ShardRegion.ExtractEntityId = {
    case cmd: RabbitMQEnvelope => (defineEntityIdForContact(cmd), cmd)
    case cmd @ NACK(env) => (defineEntityIdForContact(env), cmd)
  }

  val extractShardIdForContact: ShardRegion.ExtractShardId = {
    case cmd: RabbitMQEnvelope => (cmd.payload.get.campaign % numberOfShards).toString // defineEntityIdForContact(cmd)
    case NACK(env) => (env.payload.get.campaign % numberOfShards).toString // defineEntityIdForContact(env)
    case ShardRegion.StartEntity(id) => id.toString
  }

  val contactRegion: ActorRef = ClusterSharding(system).start(
    typeName = "contacts",
    entityProps = Props[Contact],
    settings = ClusterShardingSettings(system),
    extractEntityId = extractEntityIdForContact,
    extractShardId = extractShardIdForContact)

  log.debug("contactRegion: [{}]", contactRegion.path)

  /**
   * Service actor initialization
   */
  val cmm = system.actorOf(Props[ClusterMembershipManager], name = "clusterListener")
  //  val sm = system.actorOf(Props[SiteManager], SiteManager.siteManagerName)

  // forward the incoming OpenEvent message to the SiteManager, in order to
  // further processing.
  val pb = PublishSubscribe()

  lazy val routes: Route = userRoutes

  Http().bindAndHandle(routes, httpHost, httpPort)

  log.info(s"Server online at http://$httpHost:$httpPort/")

  Await.result(system.whenTerminated, Duration.Inf)
}
