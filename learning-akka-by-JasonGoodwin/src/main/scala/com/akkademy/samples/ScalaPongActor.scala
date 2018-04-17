package com.akkademy.samples

import akka.actor.{Actor, Status, Props, ActorLogging}

object ScalaPongActor {
  def props(): Props = {
    Props(new ScalaPongActor())
  }
}
class ScalaPongActor extends Actor with ActorLogging {
  override def receive: Receive = {
    case "Ping" =>
      log.info("Sending Pong message")
      sender() ! "Pong"
    case _ =>
      log.info("Unknown message exception")
      sender() ! Status.Failure(new Exception("unknown message"))
  }
}
