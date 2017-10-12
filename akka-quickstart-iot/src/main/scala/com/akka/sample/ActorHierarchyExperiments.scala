package com.lightbend.akka.sample

import akka.actor.{Actor, Props, ActorSystem}
import scala.io.StdIn

class PrintMyActorRefActor extends Actor{
  override def receive: Receive = {
    case "printit" =>
      val secondRef = context.actorOf(Props.empty, "second-actor")
      println(s"Second: $secondRef")
    case "justmsg" =>
      println("Received msg, no processing defined")
  }
}

object ActorHierarchyExperiments extends App {

  val system = ActorSystem("testSystem")

  val firstRef = system.actorOf(Props[PrintMyActorRefActor], "first-actor")
  println(s"First: $firstRef")

  //calling the actor
  firstRef ! "printit"

  firstRef ! "justmsg"

  println(">>> Press ENTER to exit <<<")
  try StdIn.readLine
  finally system.terminate()
}
