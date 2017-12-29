package com.flowtomation.akkared

import java.net.InetAddress
import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.settings.ServerSettings
import akka.stream.ActorMaterializer
import build.BuildInfo
import com.flowtomation.akkared.runtime.storage.FilesystemStorage
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.ExecutionContext.Implicits
import scala.util.Success

object Main extends App with StrictLogging{

  val port = 1881

  val userHome = sys.env("HOME") // or jvm user.home
  val hostname = InetAddress.getLocalHost.getHostName
  val userDirectory = Paths.get(userHome).resolve(".node-red").toAbsolutePath
  val flowsPath = userDirectory.resolve(s"flows_$hostname.json").toAbsolutePath
  val settingsPath = userDirectory.resolve("settings.json").toAbsolutePath

  logger.info("Akka-RED version: " + BuildInfo.version)
  logger.info("Java runtime version: " + System.getProperty("java.runtime.version"))
  logger.info(System.getProperty("os.name") + " " + System.getProperty("os.arch"))
  //logger.info("Loading palette nodes")
  logger.info(s"Settings file  : $settingsPath")
  logger.info(s"User directory : $userDirectory")
  logger.info(s"Flows file     : $flowsPath")

  implicit val system = ActorSystem(Logging.simpleName(this).replaceAll("\\$", ""))
  implicit val mat = ActorMaterializer()

  val settings = ServerSettings(ConfigFactory.load).withVerboseErrorMessages(true)
  val storage = new FilesystemStorage(flowsPath)
  val registry = new Registry()
  val runtime = new Runtime(registry)
  val routes = new ServerRoutes(storage, runtime).routes
  val server = new WebServer(routes)

  {
    implicit val ec = Implicits.global
    storage.readFlows.map { case (flows, _) =>
      runtime.update(flows)
    }.failed.foreach { e =>
      logger.error("Failed to update flows", e)
    }
  }

  server.startServer("0.0.0.0", port, settings, system)

}
