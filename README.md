# eventuate-learn
Learn eventuate ES framework
-----------------------------

Project contains 3 main components and can demonstrate replication between two systems and receiving external events and publishing generated events maintaining watermarks. The communication between the systems is secured by ssl and certificates :
 - KafkaBoot - scala test that writes obligation events (created,amended, cancelled) 
 to "obligation-events" topic every second and reads events from "instruction-events" topic that is populated by primary Boot service 
 - Boot - primary - actor system that reads messages from "obligation-events" topic and writes generated events  to the "instruction-events" topic 
 It can also listens for same commands typed to the console.
 All the vents are replicated to the second system using replication log
 - Boot - secondary - same actor system as primary but does not listen or write to Kafka. Its state is update by replicated log. 
 Listens on std input for status commands. 

Supported commands
------------------
- `status` - shows status of all obligations in the system (state and previous amount and new amount )
- `status {obligation reference}` shows status of obligation for given reference
- `obligation created {obligation ref} {amount}` - obligation changes its state to InstructingStarted 
- `obligation amended {obligation ref} {amount}` - obligation changes its state to Amended
- `obligation cancelled {obligation ref} {amount}` - obligation changes its state to Cancelled
- `snapshot` - snapshots state of all obligations to a target dir. On the restart all obligation actores restore their state from the snapshots


Running tests
-------------------
Run KafkaBoot scala test
Run two instances to see replication working like this (+ correct classpath or using intellij)
- `java -cp ... event.Boot primary`
- `java -cp ... event.Boot` 

0. KafkaBoot after restart deletes all topics and starts from beginning. replicated logs are stored under target directory
1. Use `status` command to observe state of obligations on both primary and secondary instance
2. Restart primary to see how it resumes reading and writing 
3. change line 54 in Boot.scala to `if (!primary) { `. Restart primary and then secondary to see that secondary continuse where primary left of
4. stop secondary, delete ist replicated log under target directory and start secondary again. State will be re-replicated

TODO
------
1. Serialization of the events using i.e. protobuf and show evolution of the events (use akka serialization and String Manifests)
2. Change akka streams for reading and writting so they use batches
3. Filter outbound events using akka streams
4. Plug in EventSourcedWriter that writes to external DB (Spring Boot Data Rest?)
5. Expand model so it shows multiple actors in sequence i.e. instruction actors - (make sure the secondary system only maintains the state - use ConditionalRequest?)
6. explored CRDTs
7. Try creating another actor system consuming filter replicated log
8. ~~explore snapshots~~
9. explore versioning
10. explore event driven communication
11. attach Kamon for monitoring
12. performance test
13. ...
