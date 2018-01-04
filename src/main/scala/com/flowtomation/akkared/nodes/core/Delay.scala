package com.flowtomation.akkared.nodes.core

import akka.actor.{Actor, ActorLogging, Props}
import com.flowtomation.akkared.{NodeContext, NodeType}
import com.flowtomation.akkared.model.FlowNode

object Delay extends NodeType{
  override def name: String = "delay"

  override def instance(ctx: NodeContext): Props = Props(new DelayActor(ctx))
}

class DelayActor(ctx: NodeContext) extends Actor with ActorLogging{
  override def receive: Receive = {
    case m => log.warning(m.toString)
  }
}