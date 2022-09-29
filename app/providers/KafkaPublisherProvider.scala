package providers

import config.KafkaConfig
import controllers.json.MetadataFormat
import org.apache.kafka.common.serialization.Serializer
import parsing.types.Metadata
import play.api.inject.ApplicationLifecycle
import play.api.libs.json.Json
import publisher.KafkaPublisher
import validator.ValidMetadata

import javax.inject.{Inject, Provider, Singleton}
import scala.util.control.NonFatal

private class MetadataSerializer
    extends Serializer[ValidMetadata]
    with MetadataFormat {
  override def serialize(topic: String, data: ValidMetadata) =
    topic match {
      case "metadata" =>
        try Json.toBytes(metaDataFormat.writes(data))
        catch { case NonFatal(_) => Array.empty[Byte] }
      case _ =>
        Array.empty[Byte]
    }
}

@Singleton
final class KafkaPublisherProvider @Inject() (
    config: ConfigReader,
    applicationLifecycle: ApplicationLifecycle
) extends Provider[KafkaPublisher[ValidMetadata]] {

  override def get(): KafkaPublisher[ValidMetadata] =
    new KafkaPublisher(
      KafkaConfig(config.kafkaServerUrl, config.kafkaApplicationId),
      "metadata",
      applicationLifecycle.addStopHook,
      classOf[MetadataSerializer]
    )
}
