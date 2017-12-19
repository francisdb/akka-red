package com.flowtomation.akkared

import java.net.InetAddress
import java.nio.file.Paths

import akka.http.scaladsl.settings.ServerSettings
import build.BuildInfo
import com.flowtomation.akkared.runtime.storage.FilesystemStorage
import com.typesafe.config.ConfigFactory

object Main extends App{
  val port = 1881

  val userHome = sys.env("HOME") // or jvm user.home
  val hostname = InetAddress.getLocalHost.getHostName
  val userDirectory = Paths.get(userHome).resolve(".node-red").toAbsolutePath
  val flowsPath = userDirectory.resolve(s"flows_$hostname.json").toAbsolutePath
  val settingsPath = userDirectory.resolve("settings.json").toAbsolutePath

  println("Akka-RED version: " + BuildInfo.version)
  println("Java runtime version: " + System.getProperty("java.runtime.version"))
  println(System.getProperty("os.name") + " " + System.getProperty("os.arch"))
  //println("Loading palette nodes")
  println(s"Settings file  : $settingsPath")
  println(s"User directory : $userDirectory")
  println(s"Flows file     : $flowsPath")

  val settings = ServerSettings(ConfigFactory.load).withVerboseErrorMessages(true)
  val storage = new FilesystemStorage(flowsPath)
  val routes = new ServerRoutes(storage).routes
  val server = new WebServer(routes)
  server.startServer("0.0.0.0", port, settings)
}
