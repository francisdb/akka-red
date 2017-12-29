package com.flowtomation.akkared.nodes.core

import akka.actor.{Actor, ActorLogging, Cancellable, Props}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.PathMatchers.Segment
import com.flowtomation.akkared.{NodeType, Runtime}
import com.flowtomation.akkared.model.{FlowNode, ItemId}
import com.flowtomation.akkared.nodes.core.Debug.{complete, path, pathPrefix, post}
import com.flowtomation.akkared.nodes.core.InjectActor.Injection

import scala.concurrent.duration._

object Inject extends NodeType{
  val name = "inject"

  override def instance(node: FlowNode): Props = {
    Props(new InjectActor(node))
  }

  override def routes(runtime: Runtime) = pathPrefix("inject") {
    path(Segment) { nodeId =>
      // if node not found return 404 with plain text body "Not Found"
      post {
        runtime.send(ItemId(nodeId), Injection)
        // empty body
        // TODO inject into node
        complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, "OK"))
      }
    }
  }
}

object InjectActor{
  case object Injection
}

private class InjectActor(node:FlowNode) extends Actor with ActorLogging{

  // TODO proper parsing of all possible values (format)
//  akka-red (once,true)
//  akka-red (payload,"")
//  akka-red (crontab,"")
//  akka-red (payloadType,"date")
//  akka-red (topic,"")
//  akka-red (repeat,"")
  //node.otherProperties.foreach(println)
  val repeat: Option[FiniteDuration] = node.otherProperties.get("repeat").map(_.as[String]).filter(_.nonEmpty).map(_.toLong.seconds)
  val once = node.otherProperties.get("once").map(_.as[Boolean]).getOrElse(false)

  // TODO add period injection

  if(once){
    self ! Injection
  }
  val repeatCancellable = repeat.fold(Cancellable.alreadyCancelled){ duration =>
    context.system.scheduler.schedule(duration, duration, self, Injection)(context.system.dispatcher)
  }

  override def receive: Receive = {
    case Injection =>
      log.info("Injection")
    case m => log.warning(m.toString)
  }

  override def postStop(): Unit = {
    repeatCancellable.cancel()
  }


}
