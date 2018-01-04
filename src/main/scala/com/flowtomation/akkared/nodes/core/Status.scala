package com.flowtomation.akkared.nodes.core

import akka.actor.{Actor, ActorLogging, Props}
import com.flowtomation.akkared.{NodeContext, NodeType}

object Status extends NodeType{
  override def name: String = "status"

  override def instance(ctx: NodeContext): Props = Props(new StatusActor(ctx))
}

object StatusActor{

}

private class StatusActor(ctx: NodeContext) extends Actor with ActorLogging{
  override def receive: Receive = {
    case m => log.warning(m.toString)
  }
}