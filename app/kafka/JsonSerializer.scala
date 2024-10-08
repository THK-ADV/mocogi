package kafka

import scala.util.control.NonFatal

import ops.LoggerOps
import org.apache.kafka.common.serialization.Serializer
import play.api.libs.json.Json
import play.api.libs.json.Writes
import play.api.Logging

final class JsonSerializer[A](topics: Seq[String])(implicit val writes: Writes[A])
    extends Serializer[A]
    with Logging
    with LoggerOps {
  override def serialize(topic: String, data: A) =
    if (data == null || !topics.contains(topic)) Array.empty[Byte]
    else
      try Json.toBytes(Json.toJson(data))
      catch {
        case NonFatal(e) =>
          logStackTrace(e)
          Array.empty[Byte]
      }
}
