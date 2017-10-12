package com.iot

import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import akka.actor.{Actor, ActorSystem, PoisonPill, Props}

import scala.concurrent.duration._

class DeviceGroupQuerySpec (_system: ActorSystem) extends TestKit(_system)
  with Matchers
  with FlatSpecLike
  with BeforeAndAfterAll {

  override def afterAll: Unit = {
    shutdown(system)
  }

  def this() = this(ActorSystem("DeviceGroupQuerySpec"))

  "Reading of temperatures" should "return all reading from devices" in {
    val requestor = TestProbe()

    val device1 = TestProbe()
    val device2 = TestProbe()

    val queryActor = system.actorOf(DeviceGroupQuery.props(
      requestId = 1,
      requestor = requestor.ref,
      actorToDeviceId = Map(device1.ref -> "device1", device2.ref -> "device2"),
      3.seconds))

    device1.expectMsg(Device.ReadTemperature(requestId=1))
    device2.expectMsg(Device.ReadTemperature(requestId=1))

    queryActor.tell(Device.RespondTemperature(requestId = 1, Some(1.0)), device1.ref)
    queryActor.tell(Device.RespondTemperature(requestId = 1, Some(2.0)), device2.ref)

    requestor.expectMsg(DeviceGroup.RespondAllTemperatures(
      requestId = 1,
      temperatures = Map(
        "device1" -> DeviceGroup.Temperature(1.0),
        "device2" -> DeviceGroup.Temperature(2.0))))

  }

  "TemperatureNotAvailable for devices" should "be handled" in {

    val requestor = TestProbe()

    val device1 = TestProbe()
    val device2 = TestProbe()

    val queryActor = system.actorOf(DeviceGroupQuery.props(
      requestId = 1,
      requestor = requestor.ref,
      actorToDeviceId = Map(device1.ref -> "device1", device2.ref -> "device2"),
      3.seconds))

    device1.expectMsg(Device.ReadTemperature(requestId=1))
    device2.expectMsg(Device.ReadTemperature(requestId=1))

    queryActor.tell(Device.RespondTemperature(requestId = 1, None), device1.ref)
    queryActor.tell(Device.RespondTemperature(requestId = 1, Some(2.0)), device2.ref)

    requestor.expectMsg(DeviceGroup.RespondAllTemperatures(
      requestId = 1,
      temperatures = Map(
        "device1" -> DeviceGroup.TemperatureNotAvailable,
        "device2" -> DeviceGroup.Temperature(2.0))))
  }

  "Device Shutdown" should "also reply" in {

    val requestor = TestProbe()

    val device1 = TestProbe()
    val device2 = TestProbe()

    val queryActor = system.actorOf(DeviceGroupQuery.props(
      requestId = 1,
      requestor = requestor.ref,
      actorToDeviceId = Map(device1.ref -> "device1", device2.ref -> "device2"),
      3.seconds))

    device1.expectMsg(Device.ReadTemperature(requestId=1))
    device2.expectMsg(Device.ReadTemperature(requestId=1))

    queryActor.tell(Device.RespondTemperature(requestId = 1, Some(1.0)), device1.ref)
    device2.ref ! PoisonPill

    requestor.expectMsg(DeviceGroup.RespondAllTemperatures(
      requestId = 1,
      temperatures = Map(
        "device1" -> DeviceGroup.Temperature(1.0),
        "device2" -> DeviceGroup.DeviceNotAvailable)))
  }

  "Timeout from device" should "be recorded" in {

    val requestor = TestProbe()

    val device1 = TestProbe()
    val device2 = TestProbe()

    val queryActor = system.actorOf(DeviceGroupQuery.props(
      requestId = 1,
      requestor = requestor.ref,
      actorToDeviceId = Map(device1.ref -> "device1", device2.ref -> "device2"),
      3.seconds))

    device1.expectMsg(Device.ReadTemperature(requestId=1))
    device2.expectMsg(Device.ReadTemperature(requestId=1))

    queryActor.tell(Device.RespondTemperature(requestId = 1, Some(1.0)), device1.ref)

    requestor.expectMsg(DeviceGroup.RespondAllTemperatures(
      requestId = 1,
      temperatures = Map(
        "device1" -> DeviceGroup.Temperature(1.0),
        "device2" -> DeviceGroup.DeviceTimedOut)))
  }
}
