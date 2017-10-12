package com.lightbend.akka.sample

import akka.actor.{ Actor, ActorLogging, Props }

object Device {
  // way to invoke constructor
  def props(groupId: String, deviceId: String): Props = Props(new Device(groupId, deviceId))

  //define messages that can be received by Device actor
  final case class ReadTemperature(requestId: Long)
  final case class RespondTemperature(requestId: Long, value: Option[Double])

  final case class RecordTemperature(requestId:Long, value:Double)
  final case class TemperatureRecorded(requestId: Long)
}

class Device(groupId: String, deviceId: String) extends Actor with ActorLogging {
  import Device._

  var lastTemperatureReading: Option[Double] = None

  override def preStart: Unit = log.info("Device actor {}-{} started", groupId, deviceId)
  override def postStop(): Unit = log.info("Device actor {}-{} stopped", groupId, deviceId)

  override def receive: Receive = {
    case ReadTemperature(id) =>
      sender() ! RespondTemperature(id, lastTemperatureReading)

    case RecordTemperature(id,value) =>
      lastTemperatureReading = Some(value)
      sender() ! TemperatureRecorded(id)
      log.info("Recorded temperature reading {} with id as {}", value, id)

    case DeviceManager.RequestTrackDevice(`groupId`,`deviceId`) =>
      sender() ! DeviceManager.DeviceRegistered

    case DeviceManager.RequestTrackDevice(groupId, deviceId) =>
      log.warning("Ignoring TrackDevice request for {}-{}. This actor is responsible for {}-{}.", groupId, deviceId, this.groupId, this.deviceId)
  }
}
