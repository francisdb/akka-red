name := "akka-red"
organization := "com.flowtomation.akka-red"
version := "0.0.1-SNAPSHOT"
scalaVersion := "2.12.4"

val akkaVersion = "2.5.8"

libraryDependencies ++= Seq(
  // For Akka 2.4.x or 2.5.x
  "com.typesafe.akka" %% "akka-http" % "10.1.0-RC1",
  //"com.typesafe.akka" %% "akka-http-spray-json" % "10.0.11",
  "de.heikoseeberger" %% "akka-http-play-json" % "1.19.0-M3",
  // Only when running against Akka 2.5 explicitly depend on akka-streams in same version as akka-actor
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-actor"  % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j"  % akkaVersion,
  "ch.megard" %% "akka-http-cors" % "0.2.2",

  "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2",
  "ch.qos.logback" % "logback-classic" % "1.2.3",


  "org.scalactic" %% "scalactic" % "3.0.4" % "test",
  "org.scalatest" %% "scalatest" % "3.0.4" % "test"
)

enablePlugins(BuildInfoPlugin)

buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion)
buildInfoPackage := "build"
