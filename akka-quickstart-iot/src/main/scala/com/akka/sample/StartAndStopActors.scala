package com.lightbend.akka.sample

import akka.actor.{Actor, ActorSystem, Props}

import scala.io.StdIn

class StartStopActor1 extends Actor{

  override def preStart(): Unit = {

    println("First started")
    context.actorOf(Props[StartStopActor2], "Second")
  }

  override def postStop(): Unit = println("First stopped")

  override def receive: Receive = {
    case "stop" => context.stop(self)
  }

}

class StartStopActor2 extends Actor{

  override def preStart(): Unit = {
    println("Second started")
  }
  override def postStop(): Unit = {
    println("Second stopped")
  }
  override def receive : Receive = Actor.emptyBehavior
}

object StartAndStopActors extends App {
  val system = ActorSystem("lifeCycleMgmt")

  val firstActor = system.actorOf(Props[StartStopActor1], "First")

  firstActor ! "stop"

  println(">>> Press ENTER to exit <<<")
  try StdIn.readLine
  finally system.terminate()
}

