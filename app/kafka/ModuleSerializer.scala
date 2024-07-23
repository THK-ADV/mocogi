package kafka

import ops.LoggerOps
import org.apache.kafka.common.serialization.Serializer
import parsing.types.Module
import play.api.Logging
import play.api.libs.json.Json

import scala.util.control.NonFatal

final class ModuleSerializer(topic: String)
    extends Serializer[Module]
    with Logging
    with LoggerOps {
  override def serialize(topic: String, data: Module) =
    if (data == null || topic != this.topic) Array.empty[Byte]
    else
      try Json.toBytes(Json.toJson(data))
      catch {
        case NonFatal(e) =>
          logStackTrace(e)
          Array.empty[Byte]
      }
}
