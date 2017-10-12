package com.lightbend.akka.sample

import akka.actor.{Actor, Props, ActorSystem}


class SupervisingActor extends Actor {
  val child = context.actorOf(Props[SupervisedActor], "Supervised-actor")

  override def receive: Receive = {
    case "failchild" => child ! "fail"
  }
}

class SupervisedActor extends Actor {
  override def preStart(): Unit = println("Supervised actor started")
  override def postStop(): Unit = println("Supervised actor stopped")

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    println("from preRestart")
    super.preRestart(reason, message)
  }

  override def postRestart(reason: Throwable): Unit = {
    println("from postRestart")
    super.postRestart(reason)
  }

  override def receive: Receive = {
    case "fail" =>
      println("supervised actor fails now")
      throw new Exception("I failed")
  }

}


object SupervisorStrategy extends App{
  val system = ActorSystem("failureMgmt")

  val parent = system.actorOf(Props[SupervisingActor], "supervising-actor")

  parent ! "failchild"

  system.terminate();
}
