package com.flowtomation.akkared.runtime.storage

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.security.MessageDigest

import akka.stream.Materializer
import akka.stream.scaladsl.{FileIO, Sink, Source}
import akka.util.ByteString
import play.api.libs.json._
import com.flowtomation.akkared.model.Flows
import com.flowtomation.akkared.format.FlowsFormat._

import scala.collection.immutable.Seq
import scala.concurrent.Future

class FilesystemStorage(flowsPath: Path) {

  def readFlows(implicit mat: Materializer): Future[(Flows, String)] = {
    implicit val ec = mat.executionContext
    if(flowsPath.toFile.exists()) {
      FileIO.fromPath(flowsPath).fold(ByteString.empty)(_ ++ _).runWith(Sink.head).map { bytes =>
        val jsonString = bytes.decodeString(StandardCharsets.UTF_8)
        val flows = Json.parse(jsonString).as[Flows]
        (flows, flowsRevision(jsonString))
      }
    }else{
      Future.successful(Flows.empty, flowsRevision("{}"))
    }
  }


  def writeFlows(flows: Flows)(implicit mat: Materializer): Future[String] = {
    implicit val ec = mat.executionContext
    val json = Json.toJson(flows)
    val jsonString = Json.stringify(json)
    Source.single(ByteString(jsonString, StandardCharsets.UTF_8))
      .runWith(FileIO.toPath(flowsPath))
      .map{_ =>
        flowsRevision(jsonString)
      }
  }

  private def flowsRevision(jsonString: String) = {
    // js crypto.createHash('md5').update(JSON.stringify(config)).digest("hex")
    // but the config for them seems to be more than only the flows node
    // TODO not that big of an issue as long as users don't switch from akka to node
    MessageDigest.getInstance("MD5")
      .digest(jsonString.getBytes(StandardCharsets.UTF_8))
      .map("%02x".format(_)).mkString
  }

}
