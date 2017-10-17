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

* If one actor carries out operations on another actor (not falling in hierarchy, so supervisor pattern doesnt apply), it good to watch the actor for its liveliness.

* An actor is a container for state, behavior, Mail box, child actors and Supervisor Strategy. This all is encapsulated in "Actor Reference".

* An actor has explicit life cycle. Its not automatically destroyed. Programmer has to take care of it. This gives flexibility in managing release of resources.

* Internally, each actor has its own single light-weight thread. 

* Each actor has got exactly one FIFO (default) Mail box. Customizations to Mail box are also possible. Messages are ordered based on the time received.

* A priority mailbox can also be set based on need which will be processed based on message priority.

* Every actor is potentially a supervisor for its child actors. Child actors can be altered by suing `context.actorOf(...)` or `context.stop(child)` APIs.

* The actual creation or termination of actors happens behind the scenes in asynchronous way so its doesnt block the supervising actor.

* Once an actor terminates, i.e. fails in a way which is not handled by a restart, stops itself or is stopped by its supervisor, it will free up its resources, draining all remaining messages from its mailbox into the system’s “dead letter mailbox” which will forward them to the EventStream as DeadLetters.

* Supervisor has the following options which an exception is encountered in child actor:
	- Resume the child actor using its existing internal state
	- Restart the child actor by removing the existing internal state
	- Stop the child actor permenantly
	- Escalate the failure, by signaling the failure itself.
	

#### Actor Best Practices

1. Actors should be nice co-workers. Should not block each other for resources for long time. Actors should not block thread by waiting for an external entity like lock or a network socket. It should passively wait on thread instead of blocking the thread.
2. Dont pass mutating objects between actors. They have to be communicated by using <i>immutable messages</i>
3. Actors are made containers for state and behavior. This should be mutated by sharing this state and behavor using "Scala Closures"

#### Top-Level Supervisors

* Root Guardian (/:) -- bubble-walker -- This is a synthetic ActorRef. 
* system Guardian (/system:)
* user Guardian (/user:) -- Actors created using system.actorOf(...)

* Restart of any actor will precisely look like this:
	- Suspend the actor and all its children -- Dont process normal message until its resumed
	- call the old instance's `preRestart` hook (defaults to sending termination on all child actors and calling `postStop` of self)
	- wait for all the children to process termination(`context.stop(child)`). This termination is also async, will wait till last child to respond to progress to next step.
	- create a new actor instance by invoking originally provided factory again
	- invoke `postRestart` on the new instance (which by default calls `preStart`)
	- send restart request to all child actors which are not killed in step 3. restarted child nodes follow from step 2 recursively.
	- resume the actor
	
* Life cycle monitoring in Akka is termed as `DeathWatch`

* Delayed restarts with BackOff Supervision pattern. The sub-sequential restart of the child actors is exponentially delayed so that necessary dependencies are up in the mean time. Like time taken to boot a database server etc or a third party service( url ).	

##### One-For-One Strategy vs One-For-All Strategy

* The difference between them is that the former applies the obtained directive only to the failed child, whereas the latter applies it to all siblings as well.

* The AllForOneStrategy is applicable in cases where the ensemble of children has such tight dependencies among them, that a failure of one child affects the function of the others, i.e. they are inextricably linked. Since a restart does not clear out the mailbox, it often is best to terminate the children upon failure and re-create them explicitly from the supervisor (by watching the children’s lifecycle);

* Normally stopping a child (i.e. not in response to a failure) will not automatically terminate the other children in an all-for-one strategy; this can easily be done by watching their lifecycle: if the Terminated message is not handled by the supervisor, it will throw a DeathPactException which (depending on its supervisor) will restart it, and the default preRestart action will terminate all children. Of course this can be handled explicitly as well.

#### Actor Paths and Addresses

* Refer to actor path and addresses hierarchy at https://doc.akka.io/docs/akka/current/scala/general/addressing.html

#### Actor and JMM

* JVM has following "happens before" rules:

1. Monitor Lock -- Lock releas happens before sub-sequent lock aquisition
2. Volatile variables -- Write "happens before" subsequent reads

* Akka modal also as following "happens before" rules:

1. The actor send rule : the send of a message to an Actor happens before receiving the message by the same actor.
2. The actor subsequent processing: the processing of message happens before processing of next message by the same actor.

`In layman’s terms this means that changes to internal fields of the actor are visible when the next message is processed by that actor. So fields in your actor need not be volatile or equivalent.`   