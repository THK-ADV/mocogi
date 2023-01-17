package providers

import config.KafkaConfig
import controllers.json.ModuleCompendiumFormat
import org.apache.kafka.common.serialization.Serializer
import play.api.inject.ApplicationLifecycle
import play.api.libs.json.Json
import publisher.KafkaPublisher
import validator.Metadata

import javax.inject.{Inject, Provider, Singleton}
import scala.util.control.NonFatal

private class MetadataSerializer
    extends Serializer[Metadata]
    with ModuleCompendiumFormat {
  override def serialize(topic: String, data: Metadata) =
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
) extends Provider[KafkaPublisher[Metadata]] {

  override def get(): KafkaPublisher[Metadata] =
    new KafkaPublisher(
      KafkaConfig(config.kafkaServerUrl, config.kafkaApplicationId),
      "metadata",
      applicationLifecycle.addStopHook,
      classOf[MetadataSerializer]
    )
}
