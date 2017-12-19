package com.flowtomation.akkared.api

import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import spray.json.{JsObject, JsString}

object Comms {

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

  def ws(implicit mat: Materializer): Flow[Message, Message, Any] =
    Flow[Message].mapConcat {
      case tm: TextMessage =>
        println(s"comms <- in text message: $tm")

        // this is used when an other browser redeploys the flows
        val message = JsObject(
          "topic" -> JsString("notification/runtime-deploy"),
          "data" -> JsObject(
            "revision" -> JsString("test")
          )
        )
        /* TextMessage(message.toString()) :: */ TextMessage(Source.single("Hello ") ++ tm.textStream ++ Source.single("!")) :: Nil
      case bm: BinaryMessage =>
        println(s"comms <- Binary message over comms: $bm")
        // ignore binary messages but drain content to avoid the stream being clogged
        bm.dataStream.runWith(Sink.ignore)
        Nil
    }
}
