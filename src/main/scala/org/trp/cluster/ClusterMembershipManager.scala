package org.trp.cluster

import akka.actor.{ Actor, ActorLogging }
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._

class ClusterMembershipManager extends Actor with ActorLogging {
  val cluster = Cluster(context.system)

  override def preStart(): Unit =
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents, classOf[MemberEvent], classOf[UnreachableMember])

  override def postStop(): Unit =
    cluster.unsubscribe(self)

  def receive = {
    case MemberUp(member) =>
      log.info("Member is Up: {}. Members are: [{}]", member.address, cluster.state.members.map(_.uniqueAddress))
    case UnreachableMember(member) =>
      log.info("Member detected as unreachable: {}. Members are: [{}]", member, cluster.state.members.map(_.uniqueAddress))
    case MemberRemoved(member, previousStatus) =>
      log.info("Member is Removed: {} after {}.  Members are: [{}]", member.address, previousStatus, cluster.state.members.map(_.uniqueAddress))
    case me: MemberEvent =>
      log.info(s"Member event [{}] received.", me)
  }
}
