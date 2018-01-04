package com.flowtomation.akkared.nodes.core

import java.time.Instant

import akka.actor.{Actor, ActorLogging, Cancellable, Props}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.PathMatchers.Segment
import akka.parboiled2.RuleTrace.StringMatch
import com.flowtomation.akkared.{NodeContext, NodeType, Runtime}
import com.flowtomation.akkared.model.{FlowNode, ItemId}
import com.flowtomation.akkared.nodes.core.Debug.{complete, path, pathPrefix, post}
import com.flowtomation.akkared.nodes.core.InjectActor.Injection
import play.api.libs.json._

import scala.concurrent.duration._

object Inject extends NodeType{
  val name = "inject"

  override def instance(ctx: NodeContext): Props = {
    Props(new InjectActor(ctx))
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


private object InjectConfig{
  implicit val reads = Json.reads[InjectConfig]
}
private case class InjectConfig(
  once: Boolean,
  payload: JsValue,
  crontab: String,
  payloadType: String,
  topic: String,
  repeat: String
)

object InjectActor{
  case object Injection
}

private class InjectActor(ctx: NodeContext) extends Actor with ActorLogging{

  // TODO proper parsing of all possible values (format)
//  akka-red (once,true)
//  akka-red (payload,"")
//  akka-red (crontab,"")
//  akka-red (payloadType,"date")
//  akka-red (topic,"")
//  akka-red (repeat,"")
  //node.otherProperties.foreach(println)
  private val config = Json.fromJson[InjectConfig](JsObject(ctx.node.otherProperties)).fold( e =>
  throw new RuntimeException(e.toString)
  , identity
)
  val repeat: Option[FiniteDuration] = Option(config.repeat).filter(_.nonEmpty).map(_.toLong.seconds)
  val once = config.once

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

      val payload: JsValue = if (( config.payloadType == null && config.payload == JsString("")) || config.payloadType == "date") {
        JsNumber(System.currentTimeMillis())
      } else if (config.payloadType == null) {
        config.payload
      } else if (config.payloadType == "none") {
        JsString("")
      } else {
        evaluateNodeProperty(config.payload, config.payloadType, config, "TODO")
      }
      val msg = Json.obj(
        "topic" -> config.topic,
        "payload" -> payload
      )
      ctx.send(msg)
    case m =>
      log.warning(m.toString)
  }

  override def postStop(): Unit = {
    repeatCancellable.cancel()
  }

  private def evaluateNodeProperty(value: JsValue, `type`: String, node: Any, msg: String): JsValue = {
    `type` match {
      case "str" => JsString(value.as[String])
      case "num" => JsNumber(BigDecimal(value.as[String]))
      case "json" => Json.parse(value.as[String])
      case "date" => JsNumber(System.currentTimeMillis())
      case "bool" => JsBoolean(Option(value).map(_.as[String].toLowerCase).contains("true"))
      case _ => value
    }

//    } else if (`type` == "re") {
//      new RegExp(value)
//    } else if (`type` == "bin") {
//      var data = JSON.parse(value)
//      Buffer.from(data)
//    } else if (`type` == "msg" && msg) {
//      getMessageProperty(msg,value)
//    } else if (`type` == "flow" && node) {
//      node.context().flow.get(value)
//    } else if (`type` == "global" && node) {
//      node.context().global.get(value)
//    } else if (`type` == "jsonata") {
//      var expr = prepareJSONataExpression(value,node)
//      return evaluateJSONataExpression(expr,msg)
//    }else{
//      value.as[JsValue]
//    }
  }


}


