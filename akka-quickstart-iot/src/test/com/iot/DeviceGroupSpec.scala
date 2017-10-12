package com.lightbend.akka.sample

import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import akka.actor.{Actor, ActorSystem, PoisonPill, Props}

import scala.concurrent.duration._

class DeviceGroupSpec (_system: ActorSystem) extends TestKit(_system)
  with Matchers
  with FlatSpecLike
  with BeforeAndAfterAll{

  override def afterAll: Unit = {
    shutdown(system)
  }

  def this() = this(ActorSystem("DeviceGroupSpec"))

  "multiple device registrations " should "be passed" in {
    val probe = TestProbe()
    val groupActor = system.actorOf(DeviceGroup.props("Andrew Home"))

    groupActor.tell(DeviceManager.RequestTrackDevice("Andrew Home", "device1"), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered)
    val deviceActor1 = probe.lastSender

    groupActor.tell(DeviceManager.RequestTrackDevice("Andrew Home", "device2"), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered)
    val deviceActor2 = probe.lastSender
    deviceActor1 should !==(deviceActor2)

    // Check that the device actors are working
    deviceActor1.tell(Device.RecordTemperature(requestId = 0, 1.0), probe.ref)
    probe.expectMsg(Device.TemperatureRecorded(requestId = 0))
    deviceActor2.tell(Device.RecordTemperature(requestId = 1, 2.0), probe.ref)
    probe.expectMsg(Device.TemperatureRecorded(requestId = 1))
  }

  "wrong group registration " should "be ignored " in {
    val probe = TestProbe()
    val groupActor = system.actorOf(DeviceGroup.props("Andrew Home"))

    groupActor.tell(DeviceManager.RequestTrackDevice("wrongGroup", "device1"), probe.ref)
    probe.expectNoMessage(500.milliseconds)
  }

  "Existing device actor " should "be returned" in {
    val probe = TestProbe()
    val groupActor = system.actorOf(DeviceGroup.props("John's Home"))
    groupActor.tell(DeviceManager.RequestTrackDevice("John's Home", "device1"), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered)
    val deviceActor1 = probe.lastSender

    groupActor.tell(DeviceManager.RequestTrackDevice("John's Home", "device1"), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered)
    val deviceActor2 = probe.lastSender

    deviceActor1 should ===(deviceActor2)
  }

  "Resgitered active device list " should "be returned" in {
    val probe = TestProbe()
    val groupActor = system.actorOf(DeviceGroup.props("Adam's Home"))

    groupActor.tell(DeviceManager.RequestTrackDevice("Adam's Home", "device1"), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered)

    groupActor.tell(DeviceManager.RequestTrackDevice("Adam's Home", "device2"), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered)

    groupActor.tell(DeviceGroup.RequestDeviceList(requestId = 0), probe.ref)
    probe.expectMsg(DeviceGroup.ReplyDeviceList(requestId = 0, Set("device1", "device2")))
  }

  "Shutdown of one active device " should "be removed from list" in {
    val probe = TestProbe()
    val groupActor = system.actorOf(DeviceGroup.props("Prithvi's house"))

    probe.watch(groupActor)

    groupActor.tell(DeviceManager.RequestTrackDevice("Prithvi's house", "device1"), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered)

    groupActor.tell(DeviceManager.RequestTrackDevice("Prithvi's house", "device2"), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered)


    val shutDownTo = probe.lastSender
    probe.watch(shutDownTo)
    shutDownTo ! PoisonPill
    probe.expectTerminated(shutDownTo)

    groupActor.tell(DeviceGroup.RequestDeviceList(requestId = 0), probe.ref)
    probe.expectMsg(DeviceGroup.ReplyDeviceList(requestId = 0, Set("device1")))
  }
}
