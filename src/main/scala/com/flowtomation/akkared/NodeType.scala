package com.flowtomation.akkared

import akka.actor.Props
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.RouteDirectives
import com.flowtomation.akkared.model.FlowNode

trait NodeType{
  def name: String

  def instance(ctx: NodeContext): Props

  // TODO create some context to inject here instead of the runtime
  def routes(runtime: Runtime): Route = RouteDirectives.reject

}

case class NodeContext(
  node: FlowNode,
  send: Any => Unit,
  publish: (String, NodeMessage) => Unit
)

case class NodeMessage(msg: Any)