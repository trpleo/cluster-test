package org.trp.cluster

import akka.actor.{ ActorRef, ActorSystem, Props }
import akka.cluster.Cluster
import akka.event.{ Logging, LoggingAdapter }
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.management.AkkaManagement
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, ExecutionContext }
import scala.util.Try

object QuickstartServer extends App with UserRoutes with Config {

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

  val cmm = system.actorOf(Props[ClusterMembershipManager], name = "clusterListener")

  val userRegistryActor: ActorRef = system.actorOf(UserRegistryActor.props, "userRegistryActor")

  lazy val routes: Route = userRoutes

  Http().bindAndHandle(routes, httpHost, httpPort)

  log.info(s"Server online at http://$httpHost:$httpPort/")

  Await.result(system.whenTerminated, Duration.Inf)
}
