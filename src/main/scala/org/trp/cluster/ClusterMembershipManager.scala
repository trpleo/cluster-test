package org.trp.cluster

import akka.actor.{ Actor, ActorLogging, ExtendedActorSystem }
import akka.cluster.ClusterEvent._
import akka.cluster.{ Cluster, Member }
import org.trp.cluster.ClusterMembershipManager._

import scala.collection.immutable.SortedSet
import scala.concurrent.duration._
import scala.language.postfixOps

class ClusterMembershipManager extends Actor with ActorLogging {

  val cluster = Cluster(context.system)

  context.become(open(SortedSet.empty[Member]))

  override def preStart(): Unit =
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents, classOf[MemberEvent], classOf[UnreachableMember])

  override def postStop(): Unit =
    cluster.unsubscribe(self)

  override def receive: Receive = { case msg => unhandled(msg) }

  /**
   * case m if m.status == Joining  ⇒ MemberJoined(m)
   * case m if m.status == WeaklyUp ⇒ MemberWeaklyUp(m)
   * case m if m.status == Up       ⇒ MemberUp(m)
   * case m if m.status == Leaving  ⇒ MemberLeft(m)
   * case m if m.status == Exiting  ⇒ MemberExited(m)
   *
   * @param s0
   * @return
   */
  def open(s0: SortedSet[Member]): Receive = {

    case MemberUp(member) =>
      log.info("Member is Up: {}. Members are: [{}]", member.address, cluster.state.members.map(_.uniqueAddress))
      context.become(open(s0.+(member)))

    case msg @ UnreachableMember(member) =>
      log.warning("YIKES... Member detected as unreachable: {}. Members are: [{}]", member, cluster.state.members.map(_.uniqueAddress))
      if (cluster.state.unreachable.size > Math.floor(cluster.state.members.size / 2).toInt + 1) {
        cluster.leave(Cluster(context.system).selfAddress)
        cluster.down(Cluster(context.system).selfAddress)
      }

    case MemberRemoved(member, previousStatus) =>
      log.info("Member is Removed: {} after {}.  Members are: [{}]", member.address, previousStatus, cluster.state.members.map(_.uniqueAddress))
      context.become(open(s0.-(member)))

    case me: MemberEvent =>
      log.info(s"Member event [{}] received.", me)
  }
}

object ClusterMembershipManager {
  final case object TimeIsUp
  final val timeIsUp = 1 second
}