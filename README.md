All the below notes is from : https://doc.akka.io/docs/akka/current/scala/guide/index.html

* The main difference between messages and events is that messages are directed while events happen. Messages have a clear destination while events may be observed by zero or more (0-N) observers.
* concurrency means handling tasks in parallel. In OOPs kind of lang, its acheived by spanning a new thread for async processing. But the question is how that task completion should be notified to main thread without blocking main thread. If there is any exception in worker thread, how should be added to main thread's stack trace... or that worker thread got crashed....
* All these are not thought well at that time because we never has any multi-core process systems in olden days. In traditional way, when we want to avoid corruption of object's state, we had to use locks and locks are leading to less performance and deadlock situations.
* In multi-core processes, now-a-days, CPU don't talk directly with main memory. They use cache lines. There are many things in common in communication of CPUs and nodes in a network. This commuincation between CPUs using the cache lines is very costly. Thats the reason we mark only "few variables" as "volatile" that means they are passed between CPU cores.

	: So how do we solve this, should we stop sharing data between threads? Is it acceptable in real world?

* Unlike OOP objects, actors not only hide encapsulation but also its execution. This is acheived by passing messages but not by calling methods on object. Communication between actors is via message passing not by method calling...

* Life cycle management of concurrent resources in multi-threaded environment is greatly simplified with actor hierarchy. If an actor is stopped (either crash or exception) it recursively stops its children as well which avoids any accidental leaks of file system or connections to DB etc 

* when a child throws an exception, then the parent (supervisor) decides on what to do. The default strategy "supervisor strategy" which will restart the child actor. How many times??

* Conflict free replicated data types (CRDTs) -- Redis -- im-memory cache impl -- data structure allows writes simultaneously and at later point in time, conflicts are resolved with math formulaes

* Main modules of Akka
  - Akka-actor
  - Akka-Remoting
  - Akka-Cluster
  - Akka Cluster Sharding
  - Akka Cluster Singleton
  - Akka Cluster Publish-Subcribe
  - Akka Persistence
  - Akka Distributed Data
  - Akka-HTTP
  - Akka Stream


* With actors, first we have to identify the actor hierarchy like we do find objects in OOPs. In OOPs, we start with Interface design. But here with actors we have to identify the protocals. Since its not posssible to idenfy the protocols in language, we generally design the messages that we want to send across the actors (their basic element of communication)

* When ever a message has to be designed :-
  - Keep it as immutable class in scala (case class)
  - Design it keeping the distributed messaging in mind. 
	
* The messages can be lost, can be duplicated, may not be delivered in order.. How to deal with them?
  - message delivery
  - message ordering	

`Since, Akka choses message delivery as "At-most-once" delivery (duplication of messages and no guarantee of mesage delivery). Message order is maintained between the sender and receiver pair.`	

* Messages can be either case classes or case objects. When the message has any property associated to it then declare it as "final case class". If the message is just for acknowledgement (DeviceRegistered) or a marker kind of interface (TemperatureNotAvailable, DeviceNotAvailable) then declare them as "case object"

* In general its good to have requestId in the messages for tracking purposes (logging) for any state changes or query capabilities

* Reading Temperature from Device: (Request-Respond pattern)

	- When ever there is a request for reading the temperature from the a device, we simplay return the last "recorded" temperature. If no temprerature is not recorded then it returns none.

* Device-Group-Registration: (Delegate-Respond pattern)

	- The DeviceManager is responsible for registering a device with a group. But the registration of Device will be delegated to DeviceGroup actor instead of DeviceManager.

	- DeviceManager is responsible for creation of DeviceGroup actors and DeviceGroup actor is responsible for creating Device actors.

	- In this way, DeviceManager has all the DeviceGroups and DeviceGroup has all the Devices registered.

* Notification of actor termination: (Create-watch-terminate)

	- Since DeviceManager is responsible for DeviceGroups, it will be watching all the DeviceGroup actors it created. When ever a DeviceGroup actor is shutdown, then a Terminated(groupActor) message is received by DeviceManager.

	- DeviceGroup will be watching the Device actors and then Terminated(deviceActor) message is received by DeviceGroup on a device shutdown. 

###### Till now actors are defined for domain objects (like Device, Device Group etc) They can also be defined for tasks (like querying the temperatures from all devices).

* Now the scenario is like there should be a way to list all the temperatures from all the registered devices for a group.

* Why cant we include the query of temperatures into DeviceGroup:
  - There can be multiple queries fired at one shot for the same group. If we keep it as another case pattern in DeviceGroup, then the queries will be queued causing high response times.
  - To handle the new device registrations while querying.

* Following things can happen while reading the temperatures
  - A new Device can be registered while the query is in between
  - While reading the temperatures, a Device can go down (shutdown) 
  - A Device can take long time than anticipated

`The first scenario is handled by taking the snapshot of the devices registered. So that multiple queries can be run with different snapshots at a time.3 can be solved by having a response deadline using the context.system.scheduler API.`

When quering a device for temperature, we might face following response:

1. It has a temperature with value (Temperature(value))
2. It has responded but no temperature is available yet. (TemperatureNotAvailable)
3. It went down after snapshot is taken (DeviceNotAvailable)
4. The response didnt come within time (DeviceTimedOut)

Typically messages follow patterns. (conversation patterns)

* Request-Respond (temparature recordings)
* Delegate-Respond (device and group registration)
* Create-watch-terminate (for creating group and device actor as children)

* In an actor, if a message is received which is not matching any of the case statements then thats called "Dead Letters"

## Actor in Detail

* If one actor carries very important data (i.e. its state shall not be lost if avoidable), this actor should source out any possibly dangerous sub-tasks to children it supervises and handle failures of these children as appropriate. Depending on the nature of the requests, it may be best to create a new child for each request, which simplifies state management for collecting the replies. This is known as the “Error Kernel Pattern” from Erlang.