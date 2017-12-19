package com.flowtomation.akkared.runtime.storage

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.security.MessageDigest

import akka.stream.{IOResult, Materializer}
import akka.stream.scaladsl.{FileIO, Source}
import akka.util.ByteString
import spray.json._
import DefaultJsonProtocol._

import scala.concurrent.Future

class FilesystemStorage(flowsPath: Path) {

  val readFlows: Source[ByteString, Future[IOResult]] = {
    FileIO.fromPath(flowsPath).fold(ByteString.empty)(_ ++ _).map { bytes =>
      val flows = bytes.decodeString(StandardCharsets.UTF_8).parseJson.convertTo[JsArray]
      val wrapped = JsObject(
        "flows" -> flows,
        "rev" -> JsString(flowsRevision(flows))
      )
      ByteString(wrapped.toString(), StandardCharsets.UTF_8)
    }
  }

  def writeFlows(json: JsObject)(implicit mat: Materializer): Future[String] = {
    // TODO Node-RED-Deployment-Type:full
    // full / flows / nodes
    implicit val ec = mat.executionContext
    json.getFields("flows", "rev") match {
      case Seq(flows: JsArray, JsString(oldRevision)) =>
        println("old " + oldRevision)
        Source.single(ByteString(flows.toString(), StandardCharsets.UTF_8))
          .runWith(FileIO.toPath(flowsPath))
          .map{_ =>
            flowsRevision(flows)
          }
      case _ =>
        throw DeserializationException("Invalid flows format")
    }

  }

  private def flowsRevision(flows: JsArray) = {
    // js crypto.createHash('md5').update(JSON.stringify(config)).digest("hex")
    // but the config for them seems to be more than only the flows node
    // TODO not that big of an issue as long as users don't switch from akka to node
    MessageDigest.getInstance("MD5")
      .digest(flows.toString().getBytes(StandardCharsets.UTF_8))
      .map("%02x".format(_)).mkString
  }

}
