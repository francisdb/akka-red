package com.flowtomation.akkared

import akka.actor.{Actor, ActorLogging, ActorRef, ActorRefFactory, Props, Terminated}
import com.flowtomation.akkared.RuntimeActor.{SendToNode, Update}
import com.flowtomation.akkared.model.{FlowNode, Flows, ItemId}

class Runtime(registry: Registry, lookupBus: LookupBusImpl)(implicit actorSystem: ActorRefFactory) {

  private val runtimeRef = actorSystem.actorOf(Props(new RuntimeActor(registry, lookupBus)))

  def update(flows: Flows): Unit = {
    runtimeRef ! Update(flows)
  }

  def send(itemId: ItemId, message: Any): Unit = {
    runtimeRef ! SendToNode(itemId, message)
  }

}

object RuntimeActor{
  case class Update(flows: Flows)
  case class SendToNode(itemId: ItemId, message: Any)
}
class RuntimeActor(registry: Registry, lookupBus: LookupBusImpl) extends Actor with ActorLogging{

  private var nodeRefs = Map[ItemId, ActorRef]()

  private def sendOut(wires: Seq[Seq[ItemId]])(msg: Any): Unit = {
    wires.foreach{ portWires =>
      portWires.foreach{ id =>
        nodeRefs.get(id) match {
          case Some(nodeRef) => nodeRef ! NodeMessage(msg)
          case None => log.warning(s"Failed to send to unexisting node $id")
        }
      }
    }
  }

  private def publish(topic: String, msg: NodeMessage): Unit = {
    val envelope = MsgEnvelope(topic, msg)
    println(s"publish $envelope")
    lookupBus.publish(envelope)
  }

  override def receive: Receive = {
    case Update(flows) =>
      nodeRefs.values.foreach(context.stop)
      nodeRefs = flows.items.collect{
        case n:FlowNode =>
          registry.getNode(n.`type`) match {
            case Some(nodeType) =>
              val ctx = NodeContext(n, sendOut(n.wires), publish)
              val nodeRef = context.actorOf(nodeType.instance(ctx), s"${n.id.id}-${System.nanoTime()}")
              log.info(s"Created ${nodeType.name} node $nodeRef")
              context.watch(nodeRef)
              Some(n.id -> nodeRef)
            case None =>
              log.error(s"No node with type ${n.`type`}")
              None
          }
      }.flatten.toMap
    case SendToNode(itemId, message) =>
      nodeRefs.get(itemId).foreach(_ ! message)
    case Terminated(ref) =>
      log.debug(s"Terminated $ref")
    case other =>
      log.warning(other.toString)
  }
}



