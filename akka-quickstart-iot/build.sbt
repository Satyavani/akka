name := "akka-quickstart-scala"

version := "1.0"

scalaVersion := "2.12.3"

val akkaVersion = "2.5.6"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.6",
  "com.typesafe.akka" %% "akka-slf4j" % "2.5.6",

  "com.typesafe.akka" %% "akka-remote" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
  "io.kamon" % "sigar-loader" % "1.6.6-rev002",

  "com.typesafe.akka" %% "akka-testkit" % "2.5.6",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test"

)
