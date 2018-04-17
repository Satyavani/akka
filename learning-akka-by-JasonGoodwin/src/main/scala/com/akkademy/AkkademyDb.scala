package com.akkademy

import akka.actor.{Actor, ActorLogging}

import scala.collection.mutable

import com.akkademy.Messages._

class AkkademyDb extends Actor with ActorLogging {
  val map = new mutable.HashMap[String,Object]()

  override def receive() : Receive = {
    case SetRequest(key,value) =>
      log.info("Received SetRequest Key - {}, value - {}", key, value)
      map.put(key, value)
    case o =>
      log.info("Unexpected message received {}", o)

  }
}
