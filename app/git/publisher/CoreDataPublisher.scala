package git.publisher

import akka.actor.{Actor, ActorRef, Props}
import database.view.{ModuleViewRepository, StudyProgramViewRepository}
import git.publisher.CoreDataPublisher.ParsingValidation
import git.{GitChanges, GitFileContent, GitFilePath}
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
      ctx: ExecutionContext
  ) =
    Props(
      new CoreDataPublisherImpl(
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

  private final class CoreDataPublisherImpl(
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
      private implicit val ctx: ExecutionContext
  ) extends Actor
      with Logging {

    override def receive = { case ParsingValidation(changes) =>
      val order = topologicalSort(changes)
      val updates = order.foldLeft(Future.unit) { case (acc, a) =>
        logger.info(a._1)
        acc.flatMap(_ => createOrUpdate(a._1, a._2))
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
        changes: GitChanges[List[(GitFilePath, GitFileContent)]]
    ): Seq[(String, GitFileContent)] = {
      val allCoreFiles = changes.modified ::: changes.added

      val deps = coreFileDependencies
      val vertices = allCoreFiles.map(_._1.fileName)
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
      sorted.map(a => (a, allCoreFiles.find(_._1.fileName == a).get._2))
    }

    /*TODO add support for deletion.
                if an entry doesn't exists anymore in a yaml file, it will not be deleted currently.
                instead, each entry will be either created (if new) or updated (if already exists).
     */
    private def createOrUpdate(
        filename: String,
        content: GitFileContent
    ): Future[Unit] =
      filename match {
        case Filenames.location =>
          locationService
            .createOrUpdate(content.value)
            .map(a => logSuccess(filename, a.size))
        case Filenames.lang =>
          languageService
            .createOrUpdate(content.value)
            .map(a => logSuccess(filename, a.size))
        case Filenames.status =>
          statusService
            .createOrUpdate(content.value)
            .map(a => logSuccess(filename, a.size))
        case Filenames.assessment =>
          assessmentMethodService
            .createOrUpdate(content.value)
            .map(a => logSuccess(filename, a.size))
        case Filenames.module_type =>
          moduleTypeService
            .createOrUpdate(content.value)
            .map(a => logSuccess(filename, a.size))
        case Filenames.season =>
          seasonService
            .createOrUpdate(content.value)
            .map(a => logSuccess(filename, a.size))
        case Filenames.person =>
          identityService
            .createOrUpdate(content.value)
            .map(a => logSuccess(filename, a.size))
        case Filenames.focus_area =>
          focusAreaService
            .createOrUpdate(content.value)
            .map(a => logSuccess(filename, a.size))
        case Filenames.global_criteria =>
          globalCriteriaService
            .createOrUpdate(content.value)
            .map(a => logSuccess(filename, a.size))
        case Filenames.po =>
          poService
            .createOrUpdate(content.value)
            .map(a => logSuccess(filename, a.size))
        case Filenames.competence =>
          competenceService
            .createOrUpdate(content.value)
            .map(a => logSuccess(filename, a.size))
        case Filenames.faculty =>
          facultyService
            .createOrUpdate(content.value)
            .map(a => logSuccess(filename, a.size))
        case Filenames.grade =>
          degreeService
            .createOrUpdate(content.value)
            .map(a => logSuccess(filename, a.size))
        case Filenames.program =>
          studyProgramService
            .createOrUpdate(content.value)
            .map(a => logSuccess(filename, a.size))
        case Filenames.specialization =>
          specializationService
            .createOrUpdate(content.value)
            .map(a => logSuccess(filename, a.size))
        case _ =>
          logUnknownFile(filename)
          Future.unit
      }

    private def logFailure(error: Throwable): Unit =
      logger.error(s"""failed to create or update core data file
           |  - message: ${error.getMessage}
           |  - trace: ${error.getStackTrace.mkString(
                       "\n           "
                     )}""".stripMargin)

    private def logUnknownFile(filename: String): Unit =
      logger.info(s"no handler found for file $filename")

    private def logSuccess(filename: String, size: Int): Unit =
      logger.info(s"""successfully created or updated core data file
           |  - file name: $filename
           |  - result: $size""".stripMargin)
  }

  private case class ParsingValidation(
      changes: GitChanges[List[(GitFilePath, GitFileContent)]]
  )
}

@Singleton
case class CoreDataPublisher(private val value: ActorRef) {
  def notifySubscribers(
      coreFiles: GitChanges[List[(GitFilePath, GitFileContent)]]
  ): Unit =
    value ! ParsingValidation(coreFiles)
}
