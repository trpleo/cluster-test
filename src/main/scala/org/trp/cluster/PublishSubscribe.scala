package org.trp.cluster

import akka.actor.{ ActorRef, ActorSystem }
import akka.event.LoggingAdapter
import akka.util.ByteString
import com.newmotion.akka.rabbitmq.{ BasicProperties, Channel, ChannelActor, ChannelMessage, ConnectionActor, CreateChannel, DefaultConsumer, Envelope }
import com.rabbitmq.client.ConnectionFactory
import org.trp.cluster.v100.OpenEvent
import scalapb.descriptors.ScalaType.ByteString
import scalapb.json4s.JsonFormat

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

class PublishSubscribe(implicit system: ActorSystem, log: LoggingAdapter) extends ProtobufSupport {
  val message =
    """
      |{
      |	"userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.99 Safari/537.36",
      |	"site": 123,
      |	"campaign": 456,
      |	"contact": 789,
      |	"ip": "10.5.5.7",
      |	"time": "2018-06-27 12:12:12"
      |}
    """.stripMargin
  val message2 = OpenEvent(
    userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.99 Safari/537.36",
    site = 123,
    campaign = 456,
    contact = 789,
    ip = "10.5.5.7",
    time = "2018-06-27 12:12:12")

  val queueName = "userEventQueue"

  val factory = new ConnectionFactory()

  log.info(s"${factory.getHost}:${factory.getPort}")

  val connection = system.actorOf(ConnectionActor.props(factory, reconnectionDelay = 10 seconds), "rabbitmq")
  val exchange = "amq.direct" // direct, topic, headers and fanout

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
        //        println("received: " + OpenEvent.parseFrom(body))
        // println(s"received: [${OpenEvent.parseFrom(body)}]")
        // todo: this must be changed. the companion must come from the message, as it is solved in [[ProtobufSupport]]
        implicit val a = message2.companion
        println(s"received: [${JsonFormat.fromJsonString(akka.util.ByteString(body).utf8String)}]")
      }
    }
    channel.basicConsume(queue, true, consumer)
  }
  connection ! CreateChannel(ChannelActor.props(setupSubscriber), Some("subscriber"))

  Future {
    def loop(n: Long) {
      val publisher = system.actorSelection("/user/rabbitmq/publisher")

      def publish(channel: Channel) {
        //        channel.basicPublish(exchange, queueName, null, message.getBytes)
        channel.basicPublish(exchange, queueName, null, JsonFormat.toJsonString(message2).getBytes)
      }
      publisher ! ChannelMessage(publish, dropIfNoChannel = false)

      Thread.sleep(1000)
      loop(n + 1)
    }
    loop(0)
  }

  def fromBytes(x: Array[Byte]) = new String(x, "UTF-8")
}
