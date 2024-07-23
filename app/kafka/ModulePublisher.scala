package kafka

import ops.LoggerOps
import org.apache.kafka.clients.admin.{AdminClient, DescribeClusterOptions}
import org.apache.kafka.clients.producer.{
  KafkaProducer,
  ProducerConfig,
  ProducerRecord,
  RecordMetadata
}
import org.apache.kafka.common.serialization.StringSerializer
import parsing.types.Module
import play.api.Logging

import java.util.Properties
import scala.annotation.unused
import scala.util.control.NonFatal

final class ModulePublisher(serverUrl: String, topic: String)
    extends Logging
    with LoggerOps {

  private val props = buildProperties
  private val producer = new KafkaProducer[String, Module](
    props,
    new StringSerializer,
    new ModuleSerializer(topic)
  )

  @unused("for debug purposes only")
  def verifyConnection(): Boolean =
    try {
      val client = AdminClient.create(props)
      val options = new DescribeClusterOptions()
      options.timeoutMs(2000)
      val nodes = client.describeCluster(options).nodes().get()
      nodes != null && nodes.size() > 0
    } catch {
      case NonFatal(e) =>
        logStackTrace(e)
        false
    }

  def commit(): Unit =
    producer.flush()

  def publish(value: Module): Unit = {
    val record = new ProducerRecord(topic, "updated", value)

    try {
      producer.send(record, onCompletion)
    } catch {
      case NonFatal(e) =>
        logStackTrace(e)
    }
  }

  private def onCompletion(
      metadata: RecordMetadata,
      exception: Exception
  ): Unit =
    if (exception != null) {
      logStackTrace(exception)
    } else {
      logger.info(
        s"record sent to partition ${metadata.partition()}. Offset ${metadata.offset()}"
      )
    }

  private def buildProperties: Properties = {
    val properties = new Properties
    properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, serverUrl)
    properties.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 3000)
    properties.put(ProducerConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG, 5000)
    properties
  }
}
