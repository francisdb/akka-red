package com.flowtomation.akkared.api

import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream.{Materializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.flowtomation.akkared.{LookupBusImpl, NodeMessage}
import play.api.libs.json.{JsString, JsValue, Json}

class Comms(lookupBus :LookupBusImpl) {

  // TODO add heartbeat messages text {topic: "hb", data: 1513722251009}

  // var webSocketKeepAliveTime = settings.webSocketKeepAliveTime || 15000
  //
  //  lastSentTime = Date.now();
  //  heartbeatTimer = setInterval(function() {
  //    var now = Date.now();
  //    if (now-lastSentTime > webSocketKeepAliveTime) {
  //      publish("hb",lastSentTime);
  //    }
  //  }, webSocketKeepAliveTime);

  def ws(implicit mat: Materializer): Flow[Message, Message, Any] = {

    val actorSource: Source[Message, _] = Source.actorRef[AnyRef](256, OverflowStrategy.fail).mapMaterializedValue{ actorRef =>
      println("Subscribing for debug")
      lookupBus.subscribe(actorRef, "debug")
    }.mapConcat {
      case nm: NodeMessage =>
        val msg = nm.msg match {
          case json:JsValue =>
            json
          case other =>
            JsString(other.toString)
        }
        val message = Json.obj(
          "topic" -> "debug",
          "data" -> Json.obj(
            "msg" -> msg
          )
        )
        val debugMsg = TextMessage(message.toString())
        println(s"comms -> $debugMsg")
        debugMsg :: Nil
      case str: String =>
        println(s"Got string: $str")
        Nil
      case other =>
        println(s"Got other: $other")
        Nil
    }

    Flow[Message].mapConcat {
      case tm: TextMessage =>
        println(s"comms <- in text message: $tm")

        // this is used when an other browser redeploys the flows
        val message = Json.obj(
          "topic" -> "notification/runtime-deploy",
          "data" -> Json.obj(
            "revision" -> "test"
          )
        )
        /* TextMessage(message.toString()) :: */ TextMessage(Source.single("Hello ") ++ tm.textStream ++ Source.single("!")) :: Nil
      case bm: BinaryMessage =>
        println(s"comms <- Binary message over comms: $bm")
        // ignore binary messages but drain content to avoid the stream being clogged
        bm.dataStream.runWith(Sink.ignore)
        Nil
    }.merge(actorSource)
  }
}
