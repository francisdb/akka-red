name := "akka-red"
organization := "com.flowtomation.akka-red"
version := "0.0.1-SNAPSHOT"
scalaVersion := "2.12.4"

libraryDependencies ++= Seq(
  // For Akka 2.4.x or 2.5.x
  "com.typesafe.akka" %% "akka-http" % "10.0.11",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.11",
  // Only when running against Akka 2.5 explicitly depend on akka-streams in same version as akka-actor
  "com.typesafe.akka" %% "akka-stream" % "2.5.8", // or whatever the latest version is
  "com.typesafe.akka" %% "akka-actor"  % "2.5.8", // or whatever the latest version is
  "ch.megard" %% "akka-http-cors" % "0.2.2",
)

enablePlugins(BuildInfoPlugin)

buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion)
buildInfoPackage := "build"
