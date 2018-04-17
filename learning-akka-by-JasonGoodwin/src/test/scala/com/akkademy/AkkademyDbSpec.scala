package com.akkademy

import org.scalatest.{BeforeAndAfterAll, FunSpecLike, Matchers}
import akka.testkit.{TestActor, TestActorRef}
import akka.actor.ActorSystem
import akka.util.Timeout
import com.akkademy.Messages._

class AkkademyDbSpec extends FunSpecLike with Matchers with BeforeAndAfterAll  {

  implicit val system = ActorSystem();

  describe("AkkademyDb") {
    describe("Given SetRequest") {
      it("should place the key/value into the map") {
        val actorRef = TestActorRef(new AkkademyDb)
        actorRef ! SetRequest("key1", "value1")

        val akkademyDb = actorRef.underlyingActor
        akkademyDb.map.get("key1") should equal(Some("value1"))
      }
    }
  }
}
