package org.trp.cluster

import org.trp.cluster.v100.RabbitMQEnvelope

import scala.language.postfixOps

object Site {
  val siteNamePrefix = "site-"
  def siteName2HandleMessage(cmd: RabbitMQEnvelope) = s"$siteNamePrefix${cmd.payload.get.site.toString}"
}