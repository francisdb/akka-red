package com.flowtomation.akkared.nodes.core

import akka.actor.{Actor, ActorLogging, Props}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives
import com.flowtomation.akkared.{NodeType, Runtime}
import com.flowtomation.akkared.model.{FlowNode, ItemId}
import com.flowtomation.akkared.nodes.core.DebugActor.SetActive

object Debug extends NodeType with Directives{
  val name = "debug"

  def instance(node: FlowNode): Props = {
    Props(new DebugActor(node))
  }

  override def routes(runtime: Runtime) = pathPrefix("debug") {
    pathPrefix(Segment) { nodeId =>
      // if node not found return 404 with plain text body "Not Found"
      path(Segment) { action =>
        post {
          // empty body
          // TODO can we get this more typesafe?
          action match {
            case "disable" =>
              runtime.send(ItemId(nodeId), SetActive(false))
              complete(StatusCodes.Created, HttpEntity(ContentTypes.`text/plain(UTF-8)`, "Created"))
            case "enable" =>
              runtime.send(ItemId(nodeId), SetActive(true))
              complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, "OK"))
            case other =>
              complete(StatusCodes.BadRequest, HttpEntity(ContentTypes.`text/plain(UTF-8)`, "Bad Request"))
          }
        }
      }
    }
  }

}

object DebugActor{
  case class SetActive(active: Boolean)
}
private class DebugActor(node: FlowNode) extends Actor with ActorLogging {

  private var active = false

  override def receive: Receive = {
    case SetActive(a) =>
      this.active = a
      log.info(s"active = $active")
    case m => log.warning(m.toString)
  }
}
