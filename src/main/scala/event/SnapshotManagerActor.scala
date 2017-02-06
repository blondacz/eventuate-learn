package event

import akka.actor.Actor

class SnapshotManagerActor extends Actor {
  override def receive: Receive = {
    case CaptureSnapshot => context.actorSelection(s"/user/manager/**") ! CaptureSnapshot
  }
}
