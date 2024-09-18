package git.publisher

import database.view.{ModuleViewRepository, StudyProgramViewRepository}
import git.publisher.CoreDataPublisher.Handle
import git.subscriber.CoreDataPublishActor
import git.{GitFile, GitFileContent}
import models.core._
import monocle.Lens
import monocle.macros.GenLens
import ops.EitherOps.EThrowableOps
import org.apache.pekko.actor.{Actor, ActorRef, Props}
import play.api.Logging
import service.core._

import javax.inject.Singleton
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object CoreDataPublisher {
  def props(
      locationService: LocationService,
      languageService: LanguageService,
      statusService: StatusService,
      assessmentMethodService: AssessmentMethodService,
      moduleTypeService: ModuleTypeService,
      seasonService: SeasonService,
      identityService: IdentityService,
      focusAreaService: FocusAreaService,
      globalCriteriaService: GlobalCriteriaService,
      poService: POService,
      competenceService: CompetenceService,
      facultyService: FacultyService,
      degreeService: DegreeService,
      studyProgramService: StudyProgramService,
      specializationService: SpecializationService,
      studyProgramViewRepository: StudyProgramViewRepository,
      moduleViewRepository: ModuleViewRepository,
      publishActor: CoreDataPublishActor,
      ctx: ExecutionContext
  ) =
    Props(
      new Impl(
        locationService,
        languageService,
        statusService,
        assessmentMethodService,
        moduleTypeService,
        seasonService,
        identityService,
        focusAreaService,
        globalCriteriaService,
        poService,
        competenceService,
        facultyService,
        degreeService,
        studyProgramService,
        specializationService,
        studyProgramViewRepository,
        moduleViewRepository,
        publishActor,
        ctx
      )
    )

  private object Filenames {
    val location = "location"
    val lang = "lang"
    val status = "status"
    val assessment = "assessment"
    val module_type = "module_type"
    val season = "season"
    val person = "person"
    val focus_area = "focus_area"
    val global_criteria = "global_criteria"
    val po = "po"
    val competence = "competence"
    val faculty = "faculty"
    val grade = "grade"
    val program = "program"
    val specialization = "specialization"
  }

  private class Graph[A](vertices: List[A], edges: List[(Int, Int)]) {
    private val numVertices = vertices.size
    private val adjacency =
      Array.fill(numVertices)(Array.fill(numVertices)(false))

    edges.foreach { case (a, b) => adjacency(a)(b) = true }

    private def hasDependency(r: Int, todo: ListBuffer[Int]): Boolean =
      todo.exists(c => adjacency(r)(c))

    def topoSort(): List[A] = {
      val result = ListBuffer.empty[A]
      val todo = ListBuffer.tabulate(numVertices)(identity)
      while (todo.nonEmpty) {
        val indexToRemove = todo.indexWhere(r => !hasDependency(r, todo))
        if (indexToRemove != -1) {
          val removedVertex = todo.remove(indexToRemove)
          result += vertices(removedVertex)
        } else {
          throw new Exception("Graph has cycles")
        }
      }
      result.toList
    }
  }

  private def coreFileDependencies = Map(
    Filenames.person -> List(Filenames.faculty),
    Filenames.program -> List(Filenames.grade, Filenames.person),
    Filenames.po -> List(Filenames.program),
    Filenames.specialization -> List(Filenames.po),
    Filenames.focus_area -> List(Filenames.program)
  )

  def toCreate(e: Seq[String], v: Seq[String]) =
    v diff e

  def toDelete(e: Seq[String], v: List[String]) =
    e diff v

  def toUpdate(e: Seq[String], v: List[String]) =
    e intersect v

  def split[A](
      allIds: Seq[String],
      locations: List[A],
      id: Lens[A, String]
  ): (Seq[A], Seq[A], Seq[String]) = {
    val vIds = locations.map(id.get)
    val toCreate =
      this
        .toCreate(allIds, vIds)
        .map(i => locations.find(a => id.get(a) == i).get)
    val toUpdate =
      this
        .toUpdate(allIds, vIds)
        .map(i => locations.find(a => id.get(a) == i).get)
    val toDelete = this.toDelete(allIds, vIds)
    (toCreate, toUpdate, toDelete)
  }

  private final class Impl(
      private val locationService: LocationService,
      private val languageService: LanguageService,
      private val statusService: StatusService,
      private val assessmentMethodService: AssessmentMethodService,
      private val moduleTypeService: ModuleTypeService,
      private val seasonService: SeasonService,
      private val identityService: IdentityService,
      private val focusAreaService: FocusAreaService,
      private val globalCriteriaService: GlobalCriteriaService,
      private val poService: POService,
      private val competenceService: CompetenceService,
      private val facultyService: FacultyService,
      private val degreeService: DegreeService,
      private val studyProgramService: StudyProgramService,
      private val specializationService: SpecializationService,
      private val studyProgramViewRepository: StudyProgramViewRepository,
      private val moduleViewRepository: ModuleViewRepository,
      private val publishActor: CoreDataPublishActor,
      private implicit val ctx: ExecutionContext
  ) extends Actor
      with Logging {

    override def receive = { case Handle(coreFiles) =>
      val order = topologicalSort(coreFiles)
      val updates = order.foldLeft(Future.unit) {
        case (acc, (filename, _, content)) =>
          acc.flatMap(_ => createOrUpdate(filename, content))
      }
      val res = for {
        _ <- updates
        _ <- studyProgramViewRepository.refreshView()
        _ <- moduleViewRepository.refreshView()
      } yield ()
      res onComplete {
        case Success(_) => logger.info("finished!")
        case Failure(t) => logFailure(t)
      }
    }

    private def topologicalSort(
        coreFiles: List[(GitFile.CoreFile, GitFileContent)]
    ): Seq[(String, GitFile.CoreFile, GitFileContent)] = {
      val deps = coreFileDependencies
      val vertices = coreFiles.map(_._1.path.fileName)
      val edges = vertices.flatMap { f =>
        deps.get(f) match {
          case Some(value) =>
            val self = vertices.indexOf(f)
            value.collect {
              case d if vertices.contains(d) => (self, vertices.indexOf(d))
            }
          case None => Nil
        }
      }
      val sorted = new Graph(vertices, edges).topoSort()
      sorted.map { filename =>
        val (coreFile, content) =
          coreFiles.find(_._1.path.fileName == filename).get
        (filename, coreFile, content)
      }
    }

    private def createOrUpdate(
        filename: String,
        content: GitFileContent
    ): Future[Unit] = {
      def go[A](
          ids: => Future[Seq[String]],
          yamlService: YamlService[A],
          getId: Lens[A, String],
          deleteMany: Seq[String] => Future[Int],
          publish: (Seq[A], Seq[A], Seq[String]) => Unit
      ) =
        for {
          parser <- yamlService.parser
          parsedValues <- parser.parse(content.value)._1.toFuture
          existing <- ids
          (toCreate, toUpdate, toDelete) = split[A](
            existing,
            parsedValues,
            getId
          )
          _ <- yamlService.createOrUpdateMany(toCreate.appendedAll(toUpdate))
          _ <- deleteMany(toDelete)
        } yield {
          logSuccess(filename, toCreate, toUpdate, toDelete)
          publish(toCreate, toUpdate, toDelete)
        }

      filename match {
        case Filenames.location =>
          go(
            locationService.repo.allIds(),
            locationService,
            GenLens[ModuleLocation](_.id),
            locationService.repo.deleteMany,
            publishActor.publishLocations
          )
        case Filenames.lang =>
          go(
            languageService.repo.allIds(),
            languageService,
            GenLens[ModuleLanguage](_.id),
            languageService.repo.deleteMany,
            publishActor.publishLanguages
          )
        case Filenames.status =>
          go(
            statusService.repo.allIds(),
            statusService,
            GenLens[ModuleStatus](_.id),
            statusService.repo.deleteMany,
            publishActor.publishModuleStatus
          )
        case Filenames.assessment =>
          go(
            assessmentMethodService.repo.allIds(),
            assessmentMethodService,
            GenLens[AssessmentMethod](_.id),
            assessmentMethodService.repo.deleteMany,
            publishActor.publishAssessmentMethods
          )
        case Filenames.module_type =>
          go(
            moduleTypeService.repo.allIds(),
            moduleTypeService,
            GenLens[ModuleType](_.id),
            moduleTypeService.repo.deleteMany,
            publishActor.publishModuleTypes
          )
        case Filenames.season =>
          go(
            seasonService.repo.allIds(),
            seasonService,
            GenLens[Season](_.id),
            seasonService.repo.deleteMany,
            publishActor.publishSeasons
          )
        case Filenames.person =>
          go(
            identityService.repo.allIds(),
            identityService,
            Identity.idLens,
            identityService.repo.deleteMany,
            publishActor.publishIdentities
          )
        case Filenames.focus_area =>
          go(
            focusAreaService.repo.allIds(),
            focusAreaService,
            GenLens[FocusArea](_.id),
            focusAreaService.repo.deleteMany,
            publishActor.publishFocusAreas
          )
        case Filenames.global_criteria =>
          go(
            globalCriteriaService.repo.allIds(),
            globalCriteriaService,
            GenLens[ModuleGlobalCriteria](_.id),
            globalCriteriaService.repo.deleteMany,
            publishActor.publishModuleGlobalCriteria
          )
        case Filenames.po =>
          go(
            poService.repo.allIds(),
            poService,
            GenLens[PO](_.id),
            poService.repo.deleteMany,
            publishActor.publishPOs
          )
        case Filenames.competence =>
          go(
            competenceService.repo.allIds(),
            competenceService,
            GenLens[ModuleCompetence](_.id),
            competenceService.repo.deleteMany,
            publishActor.publishModuleCompetences
          )
        case Filenames.faculty =>
          go(
            facultyService.repo.allIds(),
            facultyService,
            GenLens[Faculty](_.id),
            facultyService.repo.deleteMany,
            publishActor.publishFaculties
          )
        case Filenames.grade =>
          go(
            degreeService.repo.allIds(),
            degreeService,
            GenLens[Degree](_.id),
            degreeService.repo.deleteMany,
            publishActor.publishDegrees
          )
        case Filenames.program =>
          go(
            studyProgramService.allIds(),
            studyProgramService,
            GenLens[StudyProgram](_.id),
            studyProgramService.deleteMany,
            publishActor.publishStudyPrograms
          )
        case Filenames.specialization =>
          go(
            specializationService.repo.allIds(),
            specializationService,
            GenLens[Specialization](_.id),
            specializationService.repo.deleteMany,
            publishActor.publishSpecializations
          )
        case _ =>
          logUnknownFile(filename)
          Future.unit
      }
    }

    private def logSuccess[A](
        filename: String,
        toCreate: Seq[A],
        toUpdate: Seq[A],
        toDelete: Seq[String]
    ): Unit = {
      if (toCreate.nonEmpty) {
        logger.info(
          s"successfully created ${toCreate.size} $filename files"
        )
      }
      if (toUpdate.nonEmpty) {
        logger.info(
          s"successfully updated ${toUpdate.size} $filename files"
        )
      }
      if (toDelete.nonEmpty) {
        logger.info(
          s"successfully deleted ${toDelete.size} $filename files"
        )
      }
    }

    private def logFailure(error: Throwable): Unit =
      logger.error(s"""failed to create or update core data file
           |  - message: ${error.getMessage}
           |  - trace: ${error.getStackTrace.mkString(
                       "\n           "
                     )}""".stripMargin)

    private def logUnknownFile(filename: String): Unit =
      logger.info(s"no handler found for file $filename")
  }

  private case class Handle(
      coreFiles: List[(GitFile.CoreFile, GitFileContent)]
  ) extends AnyVal
}

@Singleton
case class CoreDataPublisher(private val value: ActorRef) {
  def notifySubscribers(
      coreFiles: List[(GitFile.CoreFile, GitFileContent)]
  ): Unit =
    value ! Handle(coreFiles)
}
