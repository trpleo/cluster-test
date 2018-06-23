package org.trp.cluster

import com.typesafe.config.ConfigFactory

import scala.concurrent.duration.{ Duration, FiniteDuration }

trait Config {
  private val config = ConfigFactory.load()

  private val httpConfig = config.getConfig("http")

  val httpHost = httpConfig.getString("interface")
  val httpPort = httpConfig.getInt("port")

  val jsonEnabled = httpConfig.getBoolean("jsonEnabled")

  private val applicationConfig = config.getConfig("clusterTest")

  val getStosTimeout = Duration.fromNanos(applicationConfig.getDuration("getAllSTOsTimeout").toNanos)
}