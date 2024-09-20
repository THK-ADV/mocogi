package kafka

import java.util.Properties
import java.util.UUID

import scala.annotation.unused
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.control.NonFatal

import kafka.KafkaPublisher.stringSerializer
import kafka.KafkaPublisher.uuidSerializer
import monocle.Lens
import ops.LoggerOps
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.DescribeClusterOptions
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.Serializer
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.common.serialization.UUIDSerializer
import play.api.libs.json.Writes
import play.api.Logging

trait KafkaPublisher { self: Logging with LoggerOps =>
  private val props = buildProperties

  private val voidUUIDProducer = new KafkaProducer(
    props,
    uuidSerializer,
    EmptySerializer
  )

  private val voidStringProducer = new KafkaProducer(
    props,
    stringSerializer,
    EmptySerializer
  )

  protected def serverUrl: String

  protected implicit def ctx: ExecutionContext

  protected def send[K, V](
      producer: KafkaProducer[K, V],
      topic: String,
      key: K,
      value: V
  ): Future[Unit] = {
    val record = new ProducerRecord(topic, key, value)
    val f      = producer.send(record)
    Future(f.get).map(_ => ())
  }

  protected def sendMany[K, V](
      producer: KafkaProducer[K, V],
      topic: String,
      key: Lens[V, K],
      values: Seq[V]
  ): Future[Int] =
    Future
      .sequence(values.map(v => send(producer, topic, key.get(v), v)))
      .map(_.size)

  protected def delete(topic: String, id: UUID): Future[Unit] =
    send(voidUUIDProducer, topic, id, ())

  protected def deleteMany(topic: String, ids: Seq[String]): Future[Int] =
    Future
      .sequence(ids.map(id => send(voidStringProducer, topic, id, ())))
      .map(_.size)

  protected def makeUUIDProducer[K](
      topics: Seq[String]
  )(implicit writes: Writes[K]) =
    makeProducer(uuidSerializer, topics)

  protected def makeStringProducer[K](
      topics: Seq[String]
  )(implicit writes: Writes[K]) =
    makeProducer(stringSerializer, topics)

  protected def makeProducer[K, V](
      keySerializer: Serializer[K],
      topics: Seq[String]
  )(implicit writes: Writes[V]) =
    new KafkaProducer(
      props,
      keySerializer,
      new JsonSerializer[V](topics)
    )

  @unused("for debug purposes only")
  protected def verifyConnection(): Boolean =
    try {
      val client  = AdminClient.create(props)
      val options = new DescribeClusterOptions()
      options.timeoutMs(2000)
      val nodes = client.describeCluster(options).nodes().get()
      nodes != null && nodes.size() > 0
    } catch {
      case NonFatal(e) =>
        logStackTrace(e)
        false
    }

  private def buildProperties: Properties = {
    val properties = new Properties
    properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, serverUrl)
    properties.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 3000)
    properties.put(ProducerConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG, 5000)
    properties
  }
}

object KafkaPublisher {
  val uuidSerializer   = new UUIDSerializer
  val stringSerializer = new StringSerializer
}
