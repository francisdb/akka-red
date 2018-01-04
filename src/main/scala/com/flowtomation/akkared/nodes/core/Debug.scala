package com.flowtomation.akkared.nodes.core

import java.time.Instant

import akka.actor.{Actor, ActorLogging, Props}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives
import com.flowtomation.akkared.{NodeContext, NodeMessage, NodeType, Runtime}
import com.flowtomation.akkared.model.{FlowNode, ItemId}
import com.flowtomation.akkared.nodes.core.DebugActor.SetActive
import play.api.libs.json.{JsObject, Json, Reads}

object Debug extends NodeType with Directives{
  val name = "debug"

  def instance(ctx: NodeContext): Props = {
    Props(new DebugActor(ctx))
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

private object DebugConfig{
  implicit val reads: Reads[DebugConfig] = Json.reads[DebugConfig]
}
private case class DebugConfig(
  active: Boolean,
  console: String, // "true" or "false" <- also print to console?
  complete: String // complete message body or only msg property
)

object DebugActor{
  case class SetActive(active: Boolean)
}

private class DebugActor(ctx: NodeContext) extends Actor with ActorLogging {

  private val config = Json.fromJson[DebugConfig](JsObject(ctx.node.otherProperties)).fold( e =>
    throw new RuntimeException(e.toString)
    , identity
  )

  private var active = config.active
  //ctx.node.otherProperties.foreach(println)

  private def sendDebugComplete(msg: NodeMessage){
    // { id:node.id, name:node.name, topic:msg.topic, msg:msg, _path:msg._path}
    debug(msg)
  }

  private def sendDebug(msg: NodeMessage) {
    //{id:node.id, z:node.z, name:node.name, topic:msg.topic, property:property, msg:output, _path:msg._path})}
    debug(msg)
  }

  override def receive: Receive = {
    case SetActive(a) =>
      this.active = a
      log.info(s"active = $active")
    case m:NodeMessage =>
      if (config.complete == "true"){
        if(config.console == "true"){
          log.info(m.toString)
        }
        if(active) {
          sendDebugComplete(m)
        }
      }else{
//        var property = "payload";
//        var output = msg[property];
//        if (this.complete !== "false" && typeof this.complete !== "undefined") {
//          property = this.complete;
//          try {
//            output = RED.util.getMessageProperty(msg,this.complete);
//          } catch(err) {
//            output = undefined;
//          }
//        }
//        if (this.console === "true") {
//          if (typeof output === "string") {
//            node.log((output.indexOf("\n") !== -1 ? "\n" : "") + output);
//          } else if (typeof output === "object") {
//            node.log("\n"+util.inspect(output, {colors:useColors, depth:10}));
//          } else {
//            node.log(util.inspect(output, {colors:useColors}));
//          }
//        }
        if(config.console == "true"){
          log.info(m.toString)
        }
        if(active) {
           sendDebug (m)
       }
      }
    case otherMessage =>
      log.warning(otherMessage.toString)
  }

  private def debug(msg: NodeMessage){
    val toPublish: NodeMessage = msg.msg match {
      case number:BigDecimal =>
        NodeMessage(Json.obj(
          "format" -> "number",
          "msg" -> number.toString()
        ))
//        msg.format = "number"
//        msg.msg = msg.msg.toString();
      case other =>
        msg
    }
//    // don't put blank errors in sidebar (but do add to logs)
//    //if ((msg.msg === "") && (msg.hasOwnProperty("level")) && (msg.level === 20)) { return; }
//    if (msg.msg instanceof Error) {
//      msg.format = "error";
//      var errorMsg = {};
//      if (msg.msg.name) {
//        errorMsg.name = msg.msg.name;
//      }
//      if (msg.msg.hasOwnProperty('message')) {
//        errorMsg.message = msg.msg.message;
//      } else {
//        errorMsg.message = msg.msg.toString();
//      }
//      msg.msg = JSON.stringify(errorMsg);
//    } else if (msg.msg instanceof Buffer) {
//      msg.format = "buffer["+msg.msg.length+"]";
//      msg.msg = msg.msg.toString('hex');
//      if (msg.msg.length > debuglength) {
//        msg.msg = msg.msg.substring(0,debuglength);
//      }
//    } else if (msg.msg && typeof msg.msg === 'object') {
//      try {
//        msg.format = msg.msg.constructor.name || "Object";
//        // Handle special case of msg.req/res objects from HTTP In node
//        if (msg.format === "IncomingMessage" || msg.format === "ServerResponse") {
//          msg.format = "Object";
//        }
//      } catch(err) {
//        msg.format = "Object";
//      }
//      if (/error/i.test(msg.format)) {
//        msg.msg = JSON.stringify({
//          name: msg.msg.name,
//          message: msg.msg.message
//        });
//      } else {
//        var isArray = util.isArray(msg.msg);
//        if (isArray) {
//          msg.format = "array["+msg.msg.length+"]";
//          if (msg.msg.length > debuglength) {
//            // msg.msg = msg.msg.slice(0,debuglength);
//            msg.msg = {
//              __encoded__: true,
//              type: "array",
//              data: msg.msg.slice(0,debuglength),
//              length: msg.msg.length
//            }
//          }
//        }
//        if (isArray || (msg.format === "Object")) {
//          msg.msg = safeJSONStringify(msg.msg, function(key, value) {
//            if (key === '_req' || key === '_res') {
//              value = "[internal]"
//            } else if (value instanceof Error) {
//              value = value.toString()
//            } else if (util.isArray(value) && value.length > debuglength) {
//              value = {
//                __encoded__: true,
//                type: "array",
//                data: value.slice(0,debuglength),
//                length: value.length
//              }
//            } else if (typeof value === 'string') {
//              if (value.length > debuglength) {
//                value = value.substring(0,debuglength)+"...";
//              }
//            } else if (value && value.constructor) {
//              if (value.type === "Buffer") {
//                value.__encoded__ = true;
//                value.length = value.data.length;
//                if (value.length > debuglength) {
//                  value.data = value.data.slice(0,debuglength);
//                }
//              } else if (value.constructor.name === "ServerResponse") {
//                value = "[internal]"
//              } else if (value.constructor.name === "Socket") {
//                value = "[internal]"
//              }
//            }
//            return value;
//          }," ");
//        } else {
//          try { msg.msg = msg.msg.toString(); }
//          catch(e) { msg.msg = "[Type not printable]"; }
//        }
//      }
//    } else if (typeof msg.msg === "boolean") {
//      msg.format = "boolean";
//      msg.msg = msg.msg.toString();
//    } else if (typeof msg.msg === "number") {
//      msg.format = "number";
//      msg.msg = msg.msg.toString();
//    } else if (msg.msg === 0) {
//      msg.format = "number";
//      msg.msg = "0";
//    } else if (msg.msg === null || typeof msg.msg === "undefined") {
//      msg.format = (msg.msg === null)?"null":"undefined";
//      msg.msg = "(undefined)";
//    } else {
//      msg.format = "string["+msg.msg.length+"]";
//      if (msg.msg.length > debuglength) {
//        msg.msg = msg.msg.substring(0,debuglength)+"...";
//      }
//    }
//    // if (msg.msg.length > debuglength) {
//    //     msg.msg = msg.msg.substr(0,debuglength) +" ....";
//    // }
    ctx.publish("debug", toPublish)
  }
}
