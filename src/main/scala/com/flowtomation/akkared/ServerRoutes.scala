package com.flowtomation.akkared

import akka.event.Logging
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, _}
import akka.http.scaladsl.server.directives.LogEntry
import akka.http.scaladsl.server.{Directives, Route}
import build.BuildInfo
import ch.megard.akka.http.cors.scaladsl.CorsDirectives
import com.flowtomation.akkared.api.Comms
import com.flowtomation.akkared.model.Flow
import com.flowtomation.akkared.runtime.storage.FilesystemStorage
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import play.api.libs.json.Json
import scala.collection.immutable.Seq
import com.flowtomation.akkared.format.FlowsFormat._

class ServerRoutes(storage: FilesystemStorage) extends Directives with CorsDirectives with PlayJsonSupport{

  def requestMethodAsInfo(req: HttpRequest): LogEntry =
    LogEntry(s"${req.method.name} ${req.uri}", Logging.InfoLevel)

  val routes: Route =
    logRequest(requestMethodAsInfo _) {
      extractMaterializer { implicit mat =>
        cors() {
          path("flows") {
            get {
              complete(HttpEntity(ContentTypes.`application/json`, storage.readFlows))
            } ~
              post {
                entity(as[(Seq[Flow], String)]) { case (flows, oldRevision) =>
                  // TODO read header Node-RED-Deployment-Type:full
                  // full / flows / nodes
                  println(s"old $oldRevision")
                  onSuccess(storage.writeFlows(flows)) { revision =>
                    complete {
                      Json.obj("revision" -> revision)
                    }
                  }

                }
              }
          } ~ pathPrefix("library") {
            pathPrefix("flows") {
              path(Segment) { flowId =>
                get {
                  println(flowId)
                  complete(HttpEntity(ContentTypes.`application/json`, "{}"))
                }
              } ~
              get {
                complete(HttpEntity(ContentTypes.`application/json`, "{}"))
              }
            }
          } ~ path("comms") {
            get {
              handleWebSocketMessages(Comms.ws)
            }
          } ~ path("settings") {
            get {
              // TODO
              ???
            }
          } ~ path("nodes") {
            get {
              // TODO
              ???
            }
          } ~ pathPrefix("inject"){
            path(Segment) { nodeId =>
              // if node not found return 404 with plain text body "Not Found"
              post{
                // empty body
                // TODO inject into node
                complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, "OK"))
              }
            }
          } ~ pathPrefix("debug") {
            pathPrefix(Segment) { nodeId =>
              // if node not found return 404 with plain text body "Not Found"
              path(Segment) { action =>
                post {
                  // empty body
                  // TODO can we get this more typesafe?
                  action match {
                    case "disable" =>
                      // TODO disable node
                      complete(StatusCodes.Created, HttpEntity(ContentTypes.`text/plain(UTF-8)`, "Created"))
                    case "enable" =>
                      // TODO enable node
                      complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, "OK"))
                    case other =>
                      complete(StatusCodes.BadRequest, HttpEntity(ContentTypes.`text/plain(UTF-8)`, "Bad Request"))
                  }
                }
              }
            }
          } ~ pathEndOrSingleSlash {
            get {
              complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, s"${BuildInfo.name} ${BuildInfo.version}"))
            }
          }
        }
      }
    }
}
