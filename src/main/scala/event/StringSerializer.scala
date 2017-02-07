package event


class CustomStringSerializer extends  akka.serialization.SerializerWithStringManifest {
  private val eventReadSerializer_v1 = new EventReadSerializer_v1()
  override def identifier: Int = 131313


  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case e:  EventRead => eventReadSerializer_v1.serialize(e)
  }

  override def manifest(o: AnyRef): String = "EventReadSerializer_v1"

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = manifest match {
    case "EventReadSerializer_v1" => eventReadSerializer_v1.deserialize(bytes)
  }
}

class EventReadSerializer_v1 {
  def deserialize(bytes: Array[Byte]) : AnyRef = EventRead(new String(bytes).toLong)
  def serialize(e: EventRead) : Array[Byte] = e.offset.toString.getBytes

}
