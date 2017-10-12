package com.iot

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props, Terminated}

import scala.concurrent.duration.FiniteDuration

object DeviceGroupQuery {
  def props(requestId: Long,
            requestor: ActorRef,
            actorToDeviceId: Map[ActorRef, String],
            timeout: FiniteDuration) : Props  = {
    Props(new DeviceGroupQuery(requestId, requestor, actorToDeviceId,timeout))
  }

  case object CollectionTimeout
}
class DeviceGroupQuery(requestId: Long,
                       requestor: ActorRef,
                       actorToDeviceId: Map[ActorRef, String],
                       timeout: FiniteDuration) extends Actor with ActorLogging {

  import DeviceGroupQuery._
  import context.dispatcher

  val queryTimeoutTimer = context.system.scheduler.scheduleOnce(timeout, self, CollectionTimeout)

  override def preStart(): Unit = {
    log.info("Reading temperatures for requestId {}", requestId)
    actorToDeviceId.keysIterator.foreach({deviceActor =>
      context.watch(deviceActor)
      deviceActor ! Device.ReadTemperature(requestId)
    })
  }
  override def postStop(): Unit = {
    queryTimeoutTimer.cancel()
    log.info("Completed reading of temperatures for requestId {}", requestId)
  }

  override def receive = waitingForReplies(Map.empty, actorToDeviceId.keySet)

  def waitingForReplies(repliesSoFar: Map[String, DeviceGroup.TemperatureReading],
                        stillWaiting: Set[ActorRef]) : Receive = {
    case Device.RespondTemperature(`requestId`, valueOption) =>
      val deviceActor = sender()
      val reading = valueOption match {
        case Some(value) => DeviceGroup.Temperature(value)
        case None => DeviceGroup.TemperatureNotAvailable
      }
      receivedResponse(deviceActor, reading, stillWaiting, repliesSoFar)

    case Terminated(deviceActor) =>
      receivedResponse(deviceActor, DeviceGroup.DeviceNotAvailable, stillWaiting, repliesSoFar)

    case CollectionTimeout =>
      log.warning("Time out is executed for requestId {} with devices to respond {}", requestId, stillWaiting.mkString)
      val timedOutReplies = stillWaiting.map { deviceActor =>
        val deviceId = actorToDeviceId(deviceActor)
        deviceId -> DeviceGroup.DeviceTimedOut
      }
      requestor ! DeviceGroup.RespondAllTemperatures(requestId, repliesSoFar ++ timedOutReplies)
      context.stop(self)
  }

  def receivedResponse(deviceActor: ActorRef,
                       reading: DeviceGroup.TemperatureReading,
                       stillWaiting: Set[ActorRef],
                       repliesSoFar: Map[String, DeviceGroup.TemperatureReading]) : Unit = {
    context.unwatch(deviceActor)
    val deviceId = actorToDeviceId(deviceActor)
    val newStillWaiting = stillWaiting - deviceActor
    val newRepliesSoFar = repliesSoFar + (deviceId -> reading)

    log.debug("Collected temperature from "+deviceId)
    if(newStillWaiting.isEmpty) {
      requestor ! DeviceGroup.RespondAllTemperatures(requestId, newRepliesSoFar)
      context.stop(self) // stop watching so that terminations after reading the temperature is not required
    } else {
      //as we can't change the repliesSoFar and stillWaiting, when using new parameters, we have change the context instead of just calling
      //waitingForReplies with new parameters.
      context.become(waitingForReplies(newRepliesSoFar, newStillWaiting))
    }

  }
}
