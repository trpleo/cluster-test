package org.trp.cluster

import java.util.concurrent.TimeUnit

import akka.actor.{ ActorRef, ActorSystem }
import akka.event.LoggingAdapter
import com.newmotion.akka.rabbitmq.{ BasicProperties, Channel, ChannelActor, ChannelMessage, ConnectionActor, CreateChannel, DefaultConsumer, Envelope }
import com.rabbitmq.client.ConnectionFactory
import org.trp.cluster.v100.{ OpenEvent, RabbitMQAckData, RabbitMQEnvelope }
import scalapb.json4s.JsonFormat

import scala.annotation.tailrec
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random

final case class PublishSubscribe(queueName: String = "userEventQueue")(implicit system: ActorSystem, log: LoggingAdapter) extends ProtobufSupport {
  val baseMessage = OpenEvent(
    userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.99 Safari/537.36",
    site = 123,
    campaign = 456,
    contact = 789,
    ip = "10.5.5.7",
    time = "2018-06-27 12:12:12")

  val factory = new ConnectionFactory()
  val connection = system.actorOf(ConnectionActor.props(factory, reconnectionDelay = 10 seconds), "rabbitmq")
  val exchange = "amq.direct" // direct, topic, headers and fanout

  // todo: for testing purposes
  def setupPublisher(channel: Channel, self: ActorRef) {
    val queue = channel.queueDeclare(queueName, true, false, false, null).getQueue
    channel.queueBind(queue, exchange, queueName)
  }
  connection ! CreateChannel(ChannelActor.props(setupPublisher), Some("publisher"))

  def setupSubscriber(channel: Channel, self: ActorRef) {
    val queue = channel.queueDeclare(queueName, true, false, false, null).getQueue
    channel.queueBind(queue, exchange, queueName)
    val consumer = new DefaultConsumer(channel) {
      override def handleDelivery(consumerTag: String, envelope: Envelope, properties: BasicProperties, body: Array[Byte]) {
        // todo: this must be changed. the companion must come from the message, as it is solved in [[ProtobufSupport]]
        implicit val a = baseMessage.companion
        val payload = JsonFormat.fromJsonString(akka.util.ByteString(body).utf8String)
        val data = RabbitMQAckData(envelope.getDeliveryTag, properties.getReplyTo, properties.getPriority)
        val env = RabbitMQEnvelope(Some(data), Some(payload))
        log.info(s"Open Event was received: [$env]")
        system.actorOf(ProcessManager.props(channel, env, 1000l, FiniteDuration(1000, TimeUnit.MILLISECONDS)), s"procman-${data.deliveryTag}")
      }
    }
    channel.basicConsume(queue, false, consumer)
  }
  connection ! CreateChannel(ChannelActor.props(setupSubscriber), Some("subscriber"))

  // todo: for testing purposes
  def rndInRange(start: Int, end: Int) = start + Random.nextInt((end - start) + 1)

  // todo: for testing purposes
  Future {
    @tailrec
    def loop(n: Long) {
      val publisher = system.actorSelection("/user/rabbitmq/publisher")
      def publish(channel: Channel) = {
        val bm = baseMessage.copy(site = rndInRange(1, 25), campaign = rndInRange(1, 100), contact = rndInRange(1, 10000000).toLong)
        channel.basicPublish(exchange, queueName, null, JsonFormat.toJsonString(bm).getBytes)
      }
      publisher ! ChannelMessage(publish, dropIfNoChannel = false)

      Thread.sleep(10000)
      loop(n + 1)
    }
    loop(0)
  }
}
