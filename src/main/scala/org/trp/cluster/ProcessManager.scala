package org.trp.cluster

import java.util.concurrent.TimeUnit

import akka.actor.{ Actor, ActorLogging, ActorRef, Cancellable, Props, Stash, Terminated }
import akka.pattern.pipe
import akka.util.Timeout
import com.newmotion.akka.rabbitmq.Channel
import org.trp.cluster.ProcessManager._
import org.trp.cluster.v100.RabbitMQEnvelope

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration
import scala.util.{ Failure, Success } // todo create a proper dispatcher

class ProcessManager(channel: Channel, message: RabbitMQEnvelope, contactManagerActorPath: String, txTimeout: Long = 1000l)(implicit actorSelectionTimeout: Timeout) extends Actor with ActorLogging with Stash {

  val EXPECTED_PROCESS_DONE_MESSAGES_CNT = 2

  override def preStart(): Unit = {
    super.preStart()

    // don't want to block the thread and waste resource. But as a consequence,
    // the preStart will be finished immediately. Therefore the [[StartProcessing]]
    // will be sent by the time, when the contactManager will be resolved. Messages
    // must be stashed.
    context.system.actorSelection(contactManagerActorPath).resolveOne().onComplete {
      case Success(contactManager) =>
        log.debug(s"Init finished, because actor at [{}] was found", contactManagerActorPath)
        context.watch(contactManager) // todo: what to do if the contactManager would die
        context.become(processing(ProcessingState(contactManager, 0, None)))
        unstashAll()

      case Failure(t) =>
        log.error("Could not initialize PM, because actor at [{}] could not be found. Actual error was [{}]", contactManagerActorPath, t.getMessage)
        channel.basicNack(message.ack.get.deliveryTag, false, true)
        // todo: it's parent must handle in the Supervision Strategy, what to do if, the given ContactManager actor
        // todo:  cannot be resolved through its actor path.
        throw new ContactManagerMissingException(actorSelectionTimeout)
    }
  }

  self ! StartProcessing

  // init state
  override def receive: Receive = { case _ => stash() }

  def processing(s0: ProcessingState): Receive = {
    case StartProcessing =>
      // the [[StartProcessing]] object was created in order to avoid the race condition between
      // creating the actor itself and the first message, which could come back, when the operation is
      // ready, even if this is unlikely.
      s0.contactManager ! message
      val cancellable = context.system.scheduler.scheduleOnce(FiniteDuration(txTimeout, TimeUnit.MILLISECONDS), self, Timeout)
      context.become(processing(s0.copy(cancellable = Some(cancellable))))

    case ProcessDone if (s0.doneMessageCnt + 1 == EXPECTED_PROCESS_DONE_MESSAGES_CNT) =>
      log.info(s"Process is DONE. Last message from [{}]", sender.path)
      channel.basicAck(message.ack.get.deliveryTag, false)
      s0.cancellable.map(_.cancel())
      context.stop(self)

    case ProcessDone =>
      val curr = s0.doneMessageCnt + 1
      lazy val missing = EXPECTED_PROCESS_DONE_MESSAGES_CNT - curr
      log.debug(s"Process done notification was received from [{}]. Waiting for [{}] more", sender.path, missing)
      context.become(processing(s0.copy(doneMessageCnt = curr)))

    case Timeout =>
      log.error(s"Unfortunately did not finished the action in timeout [{}]", txTimeout)
      channel.basicNack(message.ack.get.deliveryTag, false, true)
      s0.contactManager ! NACK(message)
      context.stop(self)

    case Terminated =>
      context.unwatch(s0.contactManager)
      context.system.actorSelection(contactManagerActorPath).resolveOne().pipeTo(self)
      context.become(contactManagerUnknown(UnknownCMState(s0.doneMessageCnt, s0.cancellable)))
  }

  def contactManagerUnknown(s0: UnknownCMState): Receive = {
    case Success(contactManager: ActorRef) =>
      context.watch(contactManager) // todo: what to do if the contactManager would die
      context.become(processing(ProcessingState(contactManager, s0.doneMessageCnt, s0.cancellable)))
      unstashAll()

    case Failure(t) if (s0.retryCnt < 3) =>
      log.error(s"Couldn't find searched actor [$contactManagerActorPath]. Error was: [${t.getMessage}]. Will retry again.")
      context.system.actorSelection(contactManagerActorPath).resolveOne().pipeTo(self)
      context.become(contactManagerUnknown(s0.copy(retryCnt = s0.retryCnt + 1)))

    case Failure(t) =>
      log.error(s"Couldn't find searched actor [$contactManagerActorPath]. Error was: [${t.getMessage}]. Terminate.")
      channel.basicNack(message.ack.get.deliveryTag, false, true)
      throw new ContactManagerMissingException(actorSelectionTimeout)

    case _ =>
      stash()
  }
}

object ProcessManager {
  final case class ProcessingState(contactManager: ActorRef, doneMessageCnt: Int, cancellable: Option[Cancellable])
  final case class UnknownCMState(doneMessageCnt: Int, cancellable: Option[Cancellable], retryCnt: Int = 0)
  final case class NACK(message: RabbitMQEnvelope)
  object ProcessDone
  object StartProcessing

  def props(channel: Channel, message: RabbitMQEnvelope, contactManagerActorPath: String, txTimeout: Long, actorSelectionTimeout: Timeout) = Props(classOf[ProcessManager], channel, message, contactManagerActorPath, txTimeout, actorSelectionTimeout)
}

class ContactManagerMissingException(actorSelectionTimeout: Timeout) extends RuntimeException {
  override def getMessage: String = {
    s"ContactManager could not be resolved in [${actorSelectionTimeout.duration}] timeout."
  }
}