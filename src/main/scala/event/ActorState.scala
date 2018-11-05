package event

import com.rbmhtechnology.eventuate.EventsourcedActor

trait ActorState[S] {
  this: EventsourcedActor =>
  def state: S
}


