package com.flowtomation.akkared

import akka.actor.{Actor, ActorLogging, ActorRef, ActorRefFactory, Props}
import com.flowtomation.akkared.RuntimeActor.{SendToNode, Update}
import com.flowtomation.akkared.model.{FlowNode, Flows, ItemId}

class Runtime(registry: Registry)(implicit actorSystem: ActorRefFactory) {

  private val actorRef = actorSystem.actorOf(Props(new RuntimeActor(registry)))

  def update(flows: Flows): Unit = {
    actorRef ! Update(flows)
  }

  def send(itemId: ItemId, message: Any): Unit = {
    actorRef ! SendToNode(itemId, message)
  }

}

object RuntimeActor{
  case class Update(flows: Flows)
  case class SendToNode(itemId: ItemId, message: Any)
}
class RuntimeActor(registry: Registry) extends Actor with ActorLogging{

  private var nodeRefs = Map[ItemId, ActorRef]()

  override def receive: Receive = {
    case Update(flows) =>
      nodeRefs.values.foreach(context.stop)
      nodeRefs = flows.items.collect{
        case n:FlowNode =>
          val nodeType = registry.getNode(n.`type`).getOrElse(
            throw new RuntimeException(s"No node with type ${n.`type`}")
          )
          val nodeRef = context.actorOf(nodeType.instance(n), s"${n.id.id}-${System.nanoTime()}")
          log.info(s"Created ${nodeType.name} node $nodeRef")
          context.watch(nodeRef)
          (n.id, nodeRef)
      }.toMap
    case SendToNode(itemId, message) =>
      nodeRefs.get(itemId).foreach(_ ! message)
    case other =>
      log.warning(other.toString)
  }
}
