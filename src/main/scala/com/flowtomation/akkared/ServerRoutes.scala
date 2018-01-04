package com.flowtomation.akkared

import akka.event.Logging
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, _}
import akka.http.scaladsl.server.directives.LogEntry
import akka.http.scaladsl.server.{Directives, Route}
import build.BuildInfo
import ch.megard.akka.http.cors.scaladsl.CorsDirectives
import com.flowtomation.akkared.api.Comms
import com.flowtomation.akkared.model.Flows
import com.flowtomation.akkared.runtime.storage.FilesystemStorage
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import play.api.libs.json.Json
import com.flowtomation.akkared.format.FlowsFormat._
import com.flowtomation.akkared.nodes.core.{Debug, Inject}

class ServerRoutes(storage: FilesystemStorage, runtime: Runtime, comms: Comms) extends Directives with CorsDirectives with PlayJsonSupport{

  def requestMethodAsInfo(req: HttpRequest): LogEntry =
    LogEntry(s"${req.method.name} ${req.uri}", Logging.InfoLevel)

  val routes: Route =
    logRequest(requestMethodAsInfo _) {
      extractMaterializer { implicit mat =>
        implicit val ec = mat.executionContext
        cors() {
          path("flows") {
            get {
              complete(storage.readFlows)
            } ~
              post {
                entity(as[(Flows, String)]) { case (flows, oldRevision) =>
                  // TODO read header Node-RED-Deployment-Type:full
                  // full / flows / nodes
                  println(s"old $oldRevision")
                  onSuccess{
                    for {
                      revision <- storage.writeFlows(flows)
                    } yield {
                      runtime.update(flows)
                      revision
                    }
                  } { revision =>
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
              handleWebSocketMessages(comms.ws)
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
          } ~ Debug.routes(runtime) ~ Inject.routes(runtime) ~ pathEndOrSingleSlash {
            get {
              complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, s"${BuildInfo.name} ${BuildInfo.version}"))
            }
          }

        }
      }
    }
}
