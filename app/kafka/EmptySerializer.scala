package kafka

import org.apache.kafka.common.serialization.Serializer

object EmptySerializer extends Serializer[Unit] {
  override def serialize(topic: String, data: Unit) = Array.empty[Byte]
}
