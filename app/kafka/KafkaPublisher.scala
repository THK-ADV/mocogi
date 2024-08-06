package kafka

import kafka.KafkaPublisher.{stringSerializer, uuidSerializer}
import monocle.Lens
import ops.LoggerOps
import org.apache.kafka.clients.admin.{AdminClient, DescribeClusterOptions}
import org.apache.kafka.clients.producer.{
  KafkaProducer,
  ProducerConfig,
  ProducerRecord
}
import org.apache.kafka.common.serialization.{
  Serializer,
  StringSerializer,
  UUIDSerializer
}
import play.api.Logging
import play.api.libs.json.Writes

import java.util.{Properties, UUID}
import scala.annotation.unused
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

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
    val f = producer.send(record)
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

  private def buildProperties: Properties = {
    val properties = new Properties
    properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, serverUrl)
    properties.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 3000)
    properties.put(ProducerConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG, 5000)
    properties
  }
}

object KafkaPublisher {
  val uuidSerializer = new UUIDSerializer
  val stringSerializer = new StringSerializer
}
