package com.lightbend.akka.sample

import akka.actor.{ActorSystem,Actor,Props,ActorRef}
import com.lightbend.akka.sample.IotSupervisor._
import scala.io.StdIn

object IoTApp extends App {
  val system = ActorSystem("IoT-system")

  val iotSupervisor: ActorRef = system.actorOf(IotSupervisor.props(), "IoT-supervisor")

  try StdIn.readLine()
  finally system.terminate()
}
