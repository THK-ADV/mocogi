package providers

import config.KafkaConfig
import models.Metadata
import org.apache.kafka.common.serialization.Serializer
import play.api.inject.ApplicationLifecycle
import play.api.libs.json.Json
import publisher.KafkaPublisher

import javax.inject.{Inject, Provider, Singleton}
import scala.util.control.NonFatal

private class MetadataSerializer extends Serializer[Metadata] {
  override def serialize(topic: String, data: Metadata) =
    topic match {
      case "metadata" =>
        try Json.toBytes(Json.toJson(data))
        catch { case NonFatal(_) => Array.empty[Byte] }
      case _ =>
        Array.empty[Byte]
    }
}

@Singleton
final class KafkaPublisherProvider @Inject() (
    config: ConfigReader,
    applicationLifecycle: ApplicationLifecycle
) extends Provider[Option[KafkaPublisher[Metadata]]] {

  override def get(): Option[KafkaPublisher[Metadata]] =
    for {
      kafkaServerUrl <- config.kafkaServerUrl
      kafkaApplicationId <- config.kafkaApplicationId
    } yield new KafkaPublisher(
      KafkaConfig(kafkaServerUrl, kafkaApplicationId),
      "metadata",
      applicationLifecycle.addStopHook,
      classOf[MetadataSerializer]
    )
}
