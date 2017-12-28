package com.flowtomation.akkared.runtime.storage

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.security.MessageDigest

import akka.stream.{IOResult, Materializer}
import akka.stream.scaladsl.{FileIO, Source}
import akka.util.ByteString
import play.api.libs.json._
import Json._
import com.flowtomation.akkared.model.Flow
import com.flowtomation.akkared.format.FlowsFormat._
import scala.collection.immutable.Seq

import scala.concurrent.Future

class FilesystemStorage(flowsPath: Path) {

  val readFlows: Source[ByteString, Future[IOResult]] = {
    FileIO.fromPath(flowsPath).fold(ByteString.empty)(_ ++ _).map { bytes =>
      val jsonString = bytes.decodeString(StandardCharsets.UTF_8)
      val flows = Json.parse(jsonString).as[JsArray]
      val wrapped = obj(
        "flows" -> flows,
        "rev" -> flowsRevision(jsonString)
      )
      ByteString(wrapped.toString(), StandardCharsets.UTF_8)
    }
  }


  def writeFlows(flows: Seq[Flow])(implicit mat: Materializer): Future[String] = {
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
