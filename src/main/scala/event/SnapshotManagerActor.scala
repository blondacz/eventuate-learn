package event

import akka.actor.{Actor, DeadLetter}

class SnapshotManagerActor extends Actor {
  var earliestEventSeqNr: Either[String, Long] = Left("NA")
  var previous: Either[String, Long] = Left("NA")

  override def receive: Receive = {
    case CaptureSnapshot =>
      context.system.eventStream.publish(CaptureSnapshot)
      println(s"snapshoting started last snapshot adjustment was: $previous => $earliestEventSeqNr")
      previous = earliestEventSeqNr
      earliestEventSeqNr = Right(Long.MaxValue)
      context.actorSelection("/user/manager/**") ! CaptureSnapshot
      context.actorSelection("/user/manager") ! CaptureSnapshot
      context.actorSelection("/user/input-reader") ! CaptureSnapshot

    case SnapshotSaveSuccess(metadata) if metadata.sequenceNr < previous.fold(x => 0L, l => l) =>
      println(s"ERROR: Incorrect adjustment past deleted events. Previous: ${previous.fold(x => 0L, l => l)} " +
        s"Failed: ${metadata.sequenceNr} from: ${metadata.emitterId}")
    case SnapshotSaveSuccess(metadata) =>
      println(s"snapshot saved $metadata")
      earliestEventSeqNr = earliestEventSeqNr.right.map(n => if (n > metadata.sequenceNr) {
        println(s"adjusting earliest sequence from $n to: ${metadata.sequenceNr}")
        metadata.sequenceNr
      } else n)
    case SnapshotSaveFailure(aggregateId, ex) =>
      earliestEventSeqNr = Left(ex.getMessage)
      println(s"snapshot of $aggregateId failed  $ex")
    case DeadLetter(e: Snapshotting, s, t) if s == context.self || t == context.self =>
      earliestEventSeqNr = Left(s"Snapshotting failed. Message not delivered: $e")
  }
}
