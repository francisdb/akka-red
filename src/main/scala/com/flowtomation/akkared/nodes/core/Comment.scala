package com.flowtomation.akkared.nodes.core

import akka.actor.{Actor, Props}
import com.flowtomation.akkared.{NodeContext, NodeType}
import com.flowtomation.akkared.model.FlowNode

object Comment extends NodeType{
  override def name: String = "comment"

  override def instance(ctx: NodeContext): Props = Props(new CommentActor())
}

private class CommentActor extends Actor{
  override def receive: Receive = {
    case _ => //ignore
  }
}
