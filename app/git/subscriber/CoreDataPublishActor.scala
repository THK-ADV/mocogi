package git.subscriber

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success

import git.subscriber.CoreDataPublishActor._
import kafka.KafkaPublisher
import kafka.Topics
import models.core._
import monocle.macros.GenLens
import monocle.Lens
import ops.LoggerOps
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.pekko.actor.Actor
import org.apache.pekko.actor.ActorRef
import org.apache.pekko.actor.Props
import play.api.libs.json.Writes
import play.api.Logging

object CoreDataPublishActor {
  def props(
      serverUrl: String,
      locationTopic: Topics[ModuleLocation],
      languageTopic: Topics[ModuleLanguage],
      statusTopic: Topics[ModuleStatus],
      assessmentTopic: Topics[AssessmentMethod],
      moduleTypeTopic: Topics[ModuleType],
      seasonTopic: Topics[Season],
      identityTopic: Topics[Identity],
      focusAreaTopic: Topics[FocusArea],
      globalCriteriaTopic: Topics[ModuleGlobalCriteria],
      poTopic: Topics[PO],
      competenceTopic: Topics[ModuleCompetence],
      facultyTopic: Topics[Faculty],
      degreeTopic: Topics[Degree],
      studyProgramTopic: Topics[StudyProgram],
      specializationTopic: Topics[Specialization],
      ctx: ExecutionContext
  ) =
    Props(
      new Impl(
        serverUrl,
        locationTopic,
        languageTopic,
        statusTopic,
        assessmentTopic,
        moduleTypeTopic,
        seasonTopic,
        identityTopic,
        focusAreaTopic,
        globalCriteriaTopic,
        poTopic,
        competenceTopic,
        facultyTopic,
        degreeTopic,
        studyProgramTopic,
        specializationTopic,
        ctx
      )
    )

  private case class PublishLocations(
      created: Seq[ModuleLocation],
      updated: Seq[ModuleLocation],
      deleted: Seq[String]
  )
  private case class PublishLanguages(
      created: Seq[ModuleLanguage],
      updated: Seq[ModuleLanguage],
      deleted: Seq[String]
  )
  private case class PublishModuleStatus(
      created: Seq[ModuleStatus],
      updated: Seq[ModuleStatus],
      deleted: Seq[String]
  )
  private case class PublishAssessmentMethods(
      created: Seq[AssessmentMethod],
      updated: Seq[AssessmentMethod],
      deleted: Seq[String]
  )
  private case class PublishModuleTypes(
      created: Seq[ModuleType],
      updated: Seq[ModuleType],
      deleted: Seq[String]
  )
  private case class PublishSeasons(
      created: Seq[Season],
      updated: Seq[Season],
      deleted: Seq[String]
  )
  private case class PublishIdentities(
      created: Seq[Identity],
      updated: Seq[Identity],
      deleted: Seq[String]
  )
  private case class PublishFocusAreas(
      created: Seq[FocusArea],
      updated: Seq[FocusArea],
      deleted: Seq[String]
  )
  private case class PublishModuleGlobalCriteria(
      created: Seq[ModuleGlobalCriteria],
      updated: Seq[ModuleGlobalCriteria],
      deleted: Seq[String]
  )
  private case class PublishPOs(
      created: Seq[PO],
      updated: Seq[PO],
      deleted: Seq[String]
  )
  private case class PublishModuleCompetences(
      created: Seq[ModuleCompetence],
      updated: Seq[ModuleCompetence],
      deleted: Seq[String]
  )
  private case class PublishFaculties(
      created: Seq[Faculty],
      updated: Seq[Faculty],
      deleted: Seq[String]
  )
  private case class PublishDegrees(
      created: Seq[Degree],
      updated: Seq[Degree],
      deleted: Seq[String]
  )
  private case class PublishStudyPrograms(
      created: Seq[StudyProgram],
      updated: Seq[StudyProgram],
      deleted: Seq[String]
  )
  private case class PublishSpecializations(
      created: Seq[Specialization],
      updated: Seq[Specialization],
      deleted: Seq[String]
  )

  private final class Impl(
      override val serverUrl: String,
      locationTopic: Topics[ModuleLocation],
      languageTopic: Topics[ModuleLanguage],
      statusTopic: Topics[ModuleStatus],
      assessmentTopic: Topics[AssessmentMethod],
      moduleTypeTopic: Topics[ModuleType],
      seasonTopic: Topics[Season],
      identityTopic: Topics[Identity],
      focusAreaTopic: Topics[FocusArea],
      globalCriteriaTopic: Topics[ModuleGlobalCriteria],
      poTopic: Topics[PO],
      competenceTopic: Topics[ModuleCompetence],
      facultyTopic: Topics[Faculty],
      degreeTopic: Topics[Degree],
      studyProgramTopic: Topics[StudyProgram],
      specializationTopic: Topics[Specialization],
      implicit val ctx: ExecutionContext
  ) extends Actor
      with Logging
      with LoggerOps
      with KafkaPublisher {

    override def receive = {
      case PublishLocations(created, updated, deleted) =>
        publish(
          created,
          updated,
          deleted,
          locationTopic,
          locationProducer,
          GenLens[ModuleLocation](_.id)
        )
      case PublishLanguages(created, updated, deleted) =>
        publish(
          created,
          updated,
          deleted,
          languageTopic,
          languageProducer,
          GenLens[ModuleLanguage](_.id)
        )
      case PublishModuleStatus(created, updated, deleted) =>
        publish(
          created,
          updated,
          deleted,
          statusTopic,
          statusProducer,
          GenLens[ModuleStatus](_.id)
        )
      case PublishAssessmentMethods(created, updated, deleted) =>
        publish(
          created,
          updated,
          deleted,
          assessmentTopic,
          assessmentMethodProducer,
          GenLens[AssessmentMethod](_.id)
        )
      case PublishModuleTypes(created, updated, deleted) =>
        publish(
          created,
          updated,
          deleted,
          moduleTypeTopic,
          moduleTypeProducer,
          GenLens[ModuleType](_.id)
        )
      case PublishSeasons(created, updated, deleted) =>
        publish(
          created,
          updated,
          deleted,
          seasonTopic,
          seasonProducer,
          GenLens[Season](_.id)
        )
      case PublishIdentities(created, updated, deleted) =>
        publish(
          created,
          updated,
          deleted,
          identityTopic,
          identityProducer,
          Identity.idLens
        )
      case PublishFocusAreas(created, updated, deleted) =>
        publish(
          created,
          updated,
          deleted,
          focusAreaTopic,
          focusAreaProducer,
          GenLens[FocusArea](_.id)
        )
      case PublishModuleGlobalCriteria(created, updated, deleted) =>
        publish(
          created,
          updated,
          deleted,
          globalCriteriaTopic,
          globalCriteriaProducer,
          GenLens[ModuleGlobalCriteria](_.id)
        )
      case PublishPOs(created, updated, deleted) =>
        publish(
          created,
          updated,
          deleted,
          poTopic,
          poProducer,
          GenLens[PO](_.id)
        )
      case PublishModuleCompetences(created, updated, deleted) =>
        publish(
          created,
          updated,
          deleted,
          competenceTopic,
          competenceProducer,
          GenLens[ModuleCompetence](_.id)
        )
      case PublishFaculties(created, updated, deleted) =>
        publish(
          created,
          updated,
          deleted,
          facultyTopic,
          facultyProducer,
          GenLens[Faculty](_.id)
        )
      case PublishDegrees(created, updated, deleted) =>
        publish(
          created,
          updated,
          deleted,
          degreeTopic,
          degreeProducer,
          GenLens[Degree](_.id)
        )
      case PublishStudyPrograms(created, updated, deleted) =>
        publish(
          created,
          updated,
          deleted,
          studyProgramTopic,
          studyProgramProducer,
          GenLens[StudyProgram](_.id)
        )
      case PublishSpecializations(created, updated, deleted) =>
        publish(
          created,
          updated,
          deleted,
          specializationTopic,
          specializationProducer,
          GenLens[Specialization](_.id)
        )
    }

    private def makeProducer[A](topics: Topics[A])(implicit writes: Writes[A]): KafkaProducer[String, A] =
      makeStringProducer[A](Seq(topics.created, topics.updated))

    private val locationProducer = makeProducer[ModuleLocation](locationTopic)

    private val languageProducer = makeProducer[ModuleLanguage](languageTopic)

    private val statusProducer = makeProducer[ModuleStatus](statusTopic)

    private val assessmentMethodProducer =
      makeProducer[AssessmentMethod](assessmentTopic)

    private val moduleTypeProducer = makeProducer[ModuleType](moduleTypeTopic)

    private val seasonProducer = makeProducer[Season](seasonTopic)

    private val identityProducer = makeProducer[Identity](identityTopic)

    private val focusAreaProducer = makeProducer[FocusArea](focusAreaTopic)

    private val globalCriteriaProducer =
      makeProducer[ModuleGlobalCriteria](globalCriteriaTopic)

    private val poProducer = makeProducer[PO](poTopic)

    private val competenceProducer =
      makeProducer[ModuleCompetence](competenceTopic)

    private val facultyProducer = makeProducer[Faculty](facultyTopic)

    private val degreeProducer = makeProducer[Degree](degreeTopic)

    private val studyProgramProducer =
      makeProducer[StudyProgram](studyProgramTopic)

    private val specializationProducer =
      makeProducer[Specialization](specializationTopic)

    private def publish[A](
        created: Seq[A],
        updated: Seq[A],
        deleted: Seq[String],
        topics: Topics[A],
        producer: KafkaProducer[String, A],
        id: Lens[A, String]
    ): Unit = {
      val res: Future[(Int, Int, Int)] = for {
        created <- sendMany(producer, topics.created, id, created)
        updated <- sendMany(producer, topics.updated, id, updated)
        deleted <- deleteMany(topics.deleted, deleted)
      } yield (created, updated, deleted)

      res.onComplete {
        case Success((created, updated, deleted)) =>
          if (created > 0) {
            logger.info(
              s"successfully sent $created records to topic ${topics.created}"
            )
          }
          if (updated > 0) {
            logger.info(
              s"successfully sent $updated records to topic ${topics.updated}"
            )
          }
          if (deleted > 0) {
            logger.info(
              s"successfully sent $deleted records to topic ${topics.deleted}"
            )
          }
        case Failure(e) =>
          logStackTrace(e)
      }
    }
  }
}

final class CoreDataPublishActor(actorRef: ActorRef) {
  def publishLocations(
      created: Seq[ModuleLocation],
      updated: Seq[ModuleLocation],
      deleted: Seq[String]
  ): Unit = actorRef ! PublishLocations(created, updated, deleted)

  def publishLanguages(
      created: Seq[ModuleLanguage],
      updated: Seq[ModuleLanguage],
      deleted: Seq[String]
  ): Unit = actorRef ! PublishLanguages(created, updated, deleted)

  def publishModuleStatus(
      created: Seq[ModuleStatus],
      updated: Seq[ModuleStatus],
      deleted: Seq[String]
  ): Unit = actorRef ! PublishModuleStatus(created, updated, deleted)

  def publishAssessmentMethods(
      created: Seq[AssessmentMethod],
      updated: Seq[AssessmentMethod],
      deleted: Seq[String]
  ): Unit = actorRef ! PublishAssessmentMethods(created, updated, deleted)

  def publishModuleTypes(
      created: Seq[ModuleType],
      updated: Seq[ModuleType],
      deleted: Seq[String]
  ): Unit = actorRef ! PublishModuleTypes(created, updated, deleted)

  def publishSeasons(
      created: Seq[Season],
      updated: Seq[Season],
      deleted: Seq[String]
  ): Unit = actorRef ! PublishSeasons(created, updated, deleted)

  def publishIdentities(
      created: Seq[Identity],
      updated: Seq[Identity],
      deleted: Seq[String]
  ): Unit = actorRef ! PublishIdentities(created, updated, deleted)

  def publishFocusAreas(
      created: Seq[FocusArea],
      updated: Seq[FocusArea],
      deleted: Seq[String]
  ): Unit = actorRef ! PublishFocusAreas(created, updated, deleted)

  def publishModuleGlobalCriteria(
      created: Seq[ModuleGlobalCriteria],
      updated: Seq[ModuleGlobalCriteria],
      deleted: Seq[String]
  ): Unit = actorRef ! PublishModuleGlobalCriteria(created, updated, deleted)

  def publishPOs(
      created: Seq[PO],
      updated: Seq[PO],
      deleted: Seq[String]
  ): Unit = actorRef ! PublishPOs(created, updated, deleted)

  def publishModuleCompetences(
      created: Seq[ModuleCompetence],
      updated: Seq[ModuleCompetence],
      deleted: Seq[String]
  ): Unit = actorRef ! PublishModuleCompetences(created, updated, deleted)

  def publishFaculties(
      created: Seq[Faculty],
      updated: Seq[Faculty],
      deleted: Seq[String]
  ): Unit = actorRef ! PublishFaculties(created, updated, deleted)

  def publishDegrees(
      created: Seq[Degree],
      updated: Seq[Degree],
      deleted: Seq[String]
  ): Unit = actorRef ! PublishDegrees(created, updated, deleted)

  def publishStudyPrograms(
      created: Seq[StudyProgram],
      updated: Seq[StudyProgram],
      deleted: Seq[String]
  ): Unit = actorRef ! PublishStudyPrograms(created, updated, deleted)

  def publishSpecializations(
      created: Seq[Specialization],
      updated: Seq[Specialization],
      deleted: Seq[String]
  ): Unit = actorRef ! PublishSpecializations(created, updated, deleted)
}
